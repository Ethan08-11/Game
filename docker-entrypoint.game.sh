#!/bin/sh
set -eu

# 从容器 DNS 读取 nameserver，供 Nginx 变量式 proxy_pass 使用
NGINX_RESOLVER="$(awk '/^nameserver/{print $2; exit}' /etc/resolv.conf)"
if [ -z "${NGINX_RESOLVER}" ]; then
  NGINX_RESOLVER="8.8.8.8"
fi
export NGINX_RESOLVER
export PORT="${PORT:-8080}"

# 优先用公网域名（内网 hostname 在本项目常解析失败）
# 例: https://handinhand-api.zeabur.app
export BACKEND_UPSTREAM="${BACKEND_UPSTREAM:-http://127.0.0.1:9}"

# 从 BACKEND_UPSTREAM 解析 Host 头
# https://foo.zeabur.app -> foo.zeabur.app
# http://backend.zeabur.internal:8080 -> backend.zeabur.internal
if [ -n "${BACKEND_HOST:-}" ]; then
  :
else
  BACKEND_HOST="$(printf '%s' "$BACKEND_UPSTREAM" | sed -E 's#^[a-zA-Z]+://##' | sed -E 's#/.*##' | sed -E 's#:.*##')"
  export BACKEND_HOST
fi

echo "[game-nginx] BACKEND_UPSTREAM=$BACKEND_UPSTREAM BACKEND_HOST=$BACKEND_HOST RESOLVER=$NGINX_RESOLVER"

envsubst '${PORT} ${BACKEND_UPSTREAM} ${BACKEND_HOST} ${NGINX_RESOLVER}' \
  < /etc/nginx/conf.d/configfile.template \
  > /etc/nginx/conf.d/default.conf

nginx -t
exec nginx -g 'daemon off;'

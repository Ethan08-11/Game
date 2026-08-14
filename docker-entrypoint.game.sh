#!/bin/sh
set -eu

# 从容器 DNS 读取 nameserver，供 Nginx 变量式 proxy_pass 使用
NGINX_RESOLVER="$(awk '/^nameserver/{print $2; exit}' /etc/resolv.conf)"
if [ -z "${NGINX_RESOLVER}" ]; then
  NGINX_RESOLVER="8.8.8.8"
fi
export NGINX_RESOLVER
export PORT="${PORT:-8080}"

# 后端上游地址（不要填前端自己的域名，否则会回环）
# 正确示例: https://your-backend.zeabur.app
export BACKEND_UPSTREAM="${BACKEND_UPSTREAM:-http://127.0.0.1:9}"

# 注意: Zeabur 可能会注入名为 BACKEND_HOST 的服务 ID，不能直接当 Host 头用。
# 一律从 BACKEND_UPSTREAM 解析主机名。
UPSTREAM_HOST="$(printf '%s' "$BACKEND_UPSTREAM" | sed -E 's#^[a-zA-Z]+://##' | sed -E 's#/.*##' | sed -E 's#:.*##')"
export UPSTREAM_HOST

echo "[game-nginx] BACKEND_UPSTREAM=$BACKEND_UPSTREAM UPSTREAM_HOST=$UPSTREAM_HOST RESOLVER=$NGINX_RESOLVER"

envsubst '${PORT} ${BACKEND_UPSTREAM} ${UPSTREAM_HOST} ${NGINX_RESOLVER}' \
  < /etc/nginx/conf.d/configfile.template \
  > /etc/nginx/conf.d/default.conf

nginx -t
exec nginx -g 'daemon off;'

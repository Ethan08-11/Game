#!/bin/sh
set -eu

# 从容器 DNS 读取 nameserver，供 Nginx 变量式 proxy_pass 使用
NGINX_RESOLVER="$(awk '/^nameserver/{print $2; exit}' /etc/resolv.conf)"
if [ -z "${NGINX_RESOLVER}" ]; then
  NGINX_RESOLVER="8.8.8.8"
fi
export NGINX_RESOLVER
export PORT="${PORT:-8080}"
export BACKEND_UPSTREAM="${BACKEND_UPSTREAM:-http://127.0.0.1:9}"

envsubst '${PORT} ${BACKEND_UPSTREAM} ${NGINX_RESOLVER}' \
  < /etc/nginx/conf.d/configfile.template \
  > /etc/nginx/conf.d/default.conf

# 启动前校验配置，失败时打印配置便于排查
nginx -t
exec nginx -g 'daemon off;'

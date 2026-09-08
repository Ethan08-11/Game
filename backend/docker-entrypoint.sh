#!/bin/sh
set -eu

# Zeabur 的 startup probe 会立刻 TCP 拨 $PORT。Spring Boot 要等 Bean 初始化完才会听端口，
# 这期间会刷 connection refused。先在 $PORT 上接住 TCP，等 JVM 起来后再转到内网端口。
PUBLIC_PORT="${PORT:-8080}"
INTERNAL_PORT="${APP_INTERNAL_PORT:-18080}"

shutdown() {
  if [ -n "${JAVA_PID:-}" ]; then
    kill -TERM "$JAVA_PID" 2>/dev/null || true
    wait "$JAVA_PID" 2>/dev/null || true
  fi
  if [ -n "${SOCAT_PID:-}" ]; then
    kill -TERM "$SOCAT_PID" 2>/dev/null || true
  fi
}
trap shutdown INT TERM

socat TCP-LISTEN:"${PUBLIC_PORT}",fork,reuseaddr,keepalive,bind=0.0.0.0 \
  TCP:127.0.0.1:"${INTERNAL_PORT}",retry=180,interval=1 &
SOCAT_PID=$!

export SERVER_PORT="${INTERNAL_PORT}"
# shellcheck disable=SC2086
java $JAVA_OPTS -Dserver.port="${INTERNAL_PORT}" -Dserver.address=127.0.0.1 -jar app.jar &
JAVA_PID=$!

wait "$JAVA_PID"
STATUS=$?
kill "$SOCAT_PID" 2>/dev/null || true
exit "$STATUS"

# Used when Zeabur Root Directory = backend
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml .
COPY wa-common/pom.xml wa-common/
COPY wa-api/pom.xml wa-api/
COPY wa-demo-service/pom.xml wa-demo-service/

RUN mvn dependency:go-offline -B -pl wa-demo-service -am

COPY wa-common/ wa-common/
COPY wa-api/ wa-api/
COPY wa-demo-service/ wa-demo-service/

RUN mvn clean package -DskipTests -B -pl wa-demo-service -am \
  && JAR=$(ls wa-demo-service/target/wa-demo-service-*.jar | head -n 1) \
  && cp "$JAR" /build/app.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
  && apk add --no-cache wget socat

COPY --from=builder /build/app.jar app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh \
  && chown -R appuser:appgroup /app
USER appuser

ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC" \
    SERVER_PORT=8080 \
    APP_INTERNAL_PORT=18080 \
    NACOS_DISCOVERY_ENABLED=false \
    NACOS_CONFIG_ENABLED=false

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=180s --retries=8 \
  CMD wget -qO- "http://127.0.0.1:${APP_INTERNAL_PORT:-18080}/actuator/health" || exit 1

ENTRYPOINT ["sh", "/app/docker-entrypoint.sh"]

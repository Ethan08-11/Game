# ============================================
# wa-demo-service Dockerfile 示例
# ============================================

# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 先复制 POM 文件，利用 Docker 缓存加速依赖下载
COPY pom.xml .
COPY wa-common/pom.xml wa-common/
COPY wa-api/pom.xml wa-api/
COPY wa-demo-service/pom.xml wa-demo-service/

RUN mvn dependency:go-offline -B -pl wa-demo-service -am

# 复制源码并打包
COPY wa-common/ wa-common/
COPY wa-api/ wa-api/
COPY wa-demo-service/ wa-demo-service/

RUN mvn clean package -DskipTests -B -pl wa-demo-service -am

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="cc.shturl.wa"
LABEL service="wa-demo-service"

WORKDIR /app

# 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 复制构建产物
COPY --from=builder /build/wa-demo-service/target/wa-demo-service-*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

# JVM 参数（可通过环境变量 JAVA_OPTS 覆盖）
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 服务端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

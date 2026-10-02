FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn -B -Dmaven.test.skip=true clean package -am -pl wa-demo-service

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/wa-demo-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

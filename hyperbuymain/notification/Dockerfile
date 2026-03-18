
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app


COPY . .


RUN mvn -f ./notification/pom.xml clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app


COPY --from=builder /app/notification/target/notification-service*.jar app.jar


EXPOSE 8085
# Lệnh để chạy ứng dụng khi container khởi động
ENTRYPOINT ["java", "-jar", "/app/app.jar"]


##############################################
# GIAI ĐOẠN 1: BUILD ỨNG DỤNG VỚI MAVEN
##############################################
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Thư mục làm việc cho quá trình build
WORKDIR /app

# 1️⃣ Sao chép parent POM (pom.xml gốc của project hyperbuymain)
COPY pom.xml .

# 2️⃣ Sao chép file pom.xml của module voucher vào đúng thư mục
COPY voucher/pom.xml ./voucher/

# 3️⃣ Sao chép toàn bộ mã nguồn module voucher
COPY voucher/src ./voucher/src/

# 4️⃣ Tải dependency để build offline
RUN mvn -B -f voucher/pom.xml dependency:go-offline

# 5️⃣ Build project voucher (tạo file .jar)
RUN mvn -B -f voucher/pom.xml package -DskipTests


##############################################
# GIAI ĐOẠN 2: CHẠY ỨNG DỤNG
##############################################
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 6️⃣ Copy file .jar từ builder stage
COPY --from=builder /app/voucher/target/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8089
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

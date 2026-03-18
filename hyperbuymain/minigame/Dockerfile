##############################################
# GIAI ĐOẠN 1: BUILD ỨNG DỤNG VỚI MAVEN
##############################################
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Thư mục làm việc cho quá trình build
WORKDIR /app

# 1️⃣ Sao chép parent POM (pom.xml gốc của project hyperbuymain)
COPY pom.xml .

# 2️⃣ Sao chép file pom.xml của module minigame
COPY minigame/pom.xml ./minigame/

# 3️⃣ Sao chép toàn bộ mã nguồn module minigame
COPY minigame/src ./minigame/src/

# 4️⃣ Tải dependency để build offline
RUN mvn -B -f minigame/pom.xml dependency:go-offline

# 5️⃣ Build project minigame (tạo file .jar)
RUN mvn -B -f minigame/pom.xml package -DskipTests


##############################################
# GIAI ĐOẠN 2: CHẠY ỨNG DỤNG
##############################################
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Tạo user riêng cho app (bảo mật hơn root)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 6️⃣ Copy file .jar từ builder stage
COPY --from=builder /app/minigame/target/*.jar app.jar

# Gán quyền chạy cho appuser
RUN chown appuser:appgroup app.jar
USER appuser

# 7️⃣ Expose cổng của mini game service (ví dụ 8090)
EXPOSE 8087

# 8️⃣ Lệnh khởi chạy container
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

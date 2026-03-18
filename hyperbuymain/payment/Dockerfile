# STAGE 1: Build a JAR file
# Sử dụng một base image Maven với OpenJDK 17 (hoặc phiên bản JDK bạn đang dùng)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Đặt thư mục làm việc trong container
WORKDIR /app

# Sao chép file pom.xml của module order và pom.xml gốc (nếu order là một module trong project multi-module)
# Giả sử order là một module riêng, chỉ cần pom.xml của nó.
# Nếu là multi-module và order phụ thuộc vào các module khác đã được build, bạn cần điều chỉnh bước này.
# COPY pom.xml ./ # Sao chép pom.xml gốc nếu cần
COPY . .


# (Tùy chọn) Nếu order là một module trong một project Maven multi-module,
# bạn có thể cần build toàn bộ project hoặc các module phụ thuộc trước.
# Ví dụ đơn giản nhất là build chỉ module order.
# Chạy lệnh build Maven để tạo file JAR.
# Lệnh này sẽ tải dependencies và build project.
# Sử dụng -f để chỉ định vị trí của pom.xml cho module order.
# Bỏ qua tests để build nhanh hơn trong Docker, tests nên được chạy trong CI pipeline.
RUN mvn -f ./payment/pom.xml clean package -DskipTests

# STAGE 2: Create the runtime image
# Sử dụng một base image OpenJDK JRE nhỏ gọn hơn
FROM eclipse-temurin:21-jre-alpine

# Đặt thư mục làm việc
WORKDIR /app

# Sao chép file JAR đã được build từ stage 'builder' vào image hiện tại
# Đường dẫn đến file JAR có thể thay đổi tùy theo cấu trúc project và tên artifactId/version trong pom.xml
# Thông thường nó nằm trong target/tên-artifact-version.jar
# Thay thế 'order-service-0.0.1-SNAPSHOT.jar' bằng tên file JAR thực tế của bạn
COPY --from=builder /app/payment/target/payment-service*.jar app.jar

# Expose cổng mà OrderService sẽ chạy (ví dụ: 8083 như trong docker-compose)
EXPOSE 8084
# Lệnh để chạy ứng dụng khi container khởi động
ENTRYPOINT ["java", "-jar", "/app/app.jar"]


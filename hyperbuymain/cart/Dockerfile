# Stage 1: Build the application using Maven and Java 21
# Sử dụng base image Maven với JDK Temurin 21 (phiên bản Maven có thể điều chỉnh nếu cần)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Thiết lập thư mục làm việc trong image build
WORKDIR /app

# Để build một module con trong project đa module, chúng ta cần toàn bộ project context
# hoặc ít nhất là parent POM và module POM của cart-service.
# Khi docker-compose build với context là '.', nó sẽ gửi toàn bộ thư mục gốc (hyperBuy-main)
# vào Docker daemon.

# Sao chép toàn bộ nội dung của project (từ context build của docker-compose) vào /app
COPY . .

# Chạy lệnh Maven để build chỉ module cart-service và các dependency của nó trong project.
# -pl cart: Chỉ định build project/module có tên thư mục là 'cart' (hoặc artifactId nếu khác).
#           Đảm bảo 'cart' là tên thư mục chứa pom.xml của cart-service trong project gốc.
# -am (also-make): Cũng build các project mà 'cart' phụ thuộc vào (nếu có trong cùng reactor).
# -DskipTests: Bỏ qua việc chạy test để build image nhanh hơn.
# -B: Chạy ở chế độ batch (không tương tác).
RUN mvn clean package -pl cart -am -DskipTests -B

# Stage 2: Create the runtime image using a JRE base image
# Sử dụng base image JRE Temurin 21 nhỏ gọn cho runtime
FROM eclipse-temurin:21-jre-alpine

# Thiết lập thư mục làm việc trong image runtime
WORKDIR /app

# (Tùy chọn) Tạo user không phải root để tăng cường bảo mật
# RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
# USER appuser

# Sao chép file JAR đã được build của cart-service từ stage 'builder' vào image runtime.
# Đường dẫn đến JAR sẽ là /app/cart/target/cart-service-ARTIFACTID-VERSION.jar
# Thay 'cart-service-0.0.1-SNAPSHOT.jar' bằng tên file JAR thực tế của bạn.
# Hoặc sử dụng wildcard nếu phiên bản có thể thay đổi.
# Bạn cần biết artifactId của cart-service (ví dụ: 'cart-service')
COPY --from=builder /app/cart/target/cart-service-*.jar ./app.jar

# Expose cổng mà ứng dụng CartService sẽ chạy (ví dụ: 8082)
# Đảm bảo khớp với server.port trong application.properties của cart-service
EXPOSE 8082

# Biến môi trường có thể được thiết lập ở đây hoặc (tốt hơn) trong docker-compose.yml
# ENV SPRING_PROFILES_ACTIVE=docker
# ENV SERVER_PORT=8082

# Câu lệnh để chạy ứng dụng khi container khởi động
ENTRYPOINT ["java", "-jar", "app.jar"]

# Bạn có thể thêm các tùy chọn JVM ở đây nếu cần, ví dụ:
# ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
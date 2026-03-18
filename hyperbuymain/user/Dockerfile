# GIAI ĐOẠN 1: XÂY DỰNG ỨNG DỤNG VỚI MAVEN
# Sử dụng một base image Maven với OpenJDK 21
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Đặt thư mục làm việc cho giai đoạn build
# Đây sẽ là thư mục gốc của dự án đa module bên trong container build
WORKDIR /app

# 1. Sao chép file Parent POM (từ hyperBuy-main/pom.xml) vào thư mục làm việc /app của container.
#    Khi build context là thư mục gốc (hyperBuy-main), "pom.xml" ở đây chính là Parent POM.
COPY pom.xml .

# 2. Sao chép file pom.xml của module 'user' vào đúng vị trí module của nó bên trong container.
#    Đường dẫn "user/pom.xml" là tương đối so với build context (hyperBuy-main).
#    Nó sẽ được copy vào "/app/user/pom.xml" trong container.
COPY user/pom.xml ./user/

# 3. Sao chép toàn bộ mã nguồn của module 'user' vào đúng vị trí module của nó bên trong container.
#    Đường dẫn "user/src" là tương đối so với build context.
#    Nó sẽ được copy vào "/app/user/src/" trong container.
COPY user/src ./user/src/

# (Tùy chọn, nếu các module khác như 'product' cũng cần được build đồng thời hoặc là dependency)
# COPY product/pom.xml ./product/
# COPY product/src ./product/src/

# 4. Chạy lệnh mvn dependency:go-offline cho module 'user'.
#    Maven sẽ chạy từ thư mục /app (nơi có Parent POM).
#    Cờ -f (hoặc --file) chỉ định file POM của module 'user' cần xử lý.
#    Lúc này, <relativePath>../pom.xml</relativePath> trong /app/user/pom.xml sẽ trỏ đúng đến /app/pom.xml (Parent POM).
RUN mvn -B -f user/pom.xml dependency:go-offline

# 5. Xây dựng (package) module 'user'.
#    File JAR sẽ được tạo ra trong /app/user/target/
#    LƯU Ý: Tên file JAR thường là <artifactId>-<version>.jar. Đảm bảo artifactId của module user là "user" hoặc "user-service"
#    trong file user/pom.xml để lệnh COPY ở giai đoạn 2 hoạt động đúng với wildcard.
RUN mvn -B -f user/pom.xml package -DskipTests


# GIAI ĐOẠN 2: CHẠY ỨNG DỤNG
# Sử dụng một base image OpenJDK JRE 21 nhỏ gọn hơn
FROM eclipse-temurin:21-jre-alpine

# Đặt thư mục làm việc cho image cuối cùng
WORKDIR /app

# Tạo user và group không phải root để tăng cường bảo mật
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Sao chép file .jar đã được build của module 'user' từ giai đoạn 'builder'.
# Đường dẫn đến file JAR trong giai đoạn builder là /app/user/target/
# Wildcard * sẽ khớp với phiên bản. Ví dụ: user-0.0.1-SNAPSHOT.jar hoặc user-service-0.0.1-SNAPSHOT.jar
COPY --from=builder /app/user/target/*.jar app.jar

# Thay đổi quyền sở hữu file JAR cho user không phải root
RUN chown appuser:appgroup app.jar

# Chuyển sang user không phải root để chạy ứng dụng
USER appuser

# Mở cổng mà ứng dụng User Service sẽ lắng nghe (ví dụ: 8080).
# Đảm bảo cổng này khớp với server.port trong application.properties của module 'user'.
EXPOSE 8080

# Lệnh để chạy ứng dụng khi container được khởi động
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

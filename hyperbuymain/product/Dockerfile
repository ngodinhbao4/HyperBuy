# GIAI ĐOẠN 1: XÂY DỰNG ỨNG DỤNG VỚI MAVEN
# Sử dụng một base image Maven với OpenJDK 21
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Đặt thư mục làm việc cho giai đoạn build
# Đây sẽ là thư mục gốc của dự án đa module bên trong container build
WORKDIR /app

# 1. Sao chép file Parent POM (từ hyperBuy-main/pom.xml) vào thư mục làm việc /app của container.
#    Khi build context trong docker-compose.yml là thư mục gốc (hyperBuy-main),
#    "pom.xml" ở đây chính là Parent POM (hyperBuy-main/pom.xml).
COPY pom.xml .

# 2. Sao chép file pom.xml của module 'product' vào đúng vị trí module của nó bên trong container.
#    Đường dẫn "product/pom.xml" là tương đối so với build context (hyperBuy-main).
#    Nó sẽ được copy vào "/app/product/pom.xml" trong container.
COPY product/pom.xml ./product/

# 3. Sao chép toàn bộ mã nguồn của module 'product' vào đúng vị trí module của nó bên trong container.
#    Đường dẫn "product/src" là tương đối so với build context.
#    Nó sẽ được copy vào "/app/product/src/" trong container.
COPY product/src ./product/src/

# (Tùy chọn, nếu bạn có các module khác và product-service phụ thuộc vào chúng ở mức source code,
#  hoặc bạn muốn build nhiều module trong cùng một Dockerfile - ít phổ biến hơn cho microservices)
# COPY user/pom.xml ./user/
# COPY user/src ./user/src/

# 4. Chạy lệnh mvn dependency:go-offline cho module 'product'.
#    Maven sẽ chạy từ thư mục /app (nơi có Parent POM).
#    Cờ -f (hoặc --file) chỉ định file POM của module 'product' cần xử lý (/app/product/pom.xml).
#    Thẻ <relativePath>../pom.xml</relativePath> trong /app/product/pom.xml sẽ trỏ đúng đến Parent POM tại /app/pom.xml.
RUN mvn -B -f product/pom.xml dependency:go-offline

# 5. Xây dựng (package) module 'product'.
#    File JAR sẽ được tạo ra trong /app/product/target/
RUN mvn -B -f product/pom.xml package -DskipTests


# GIAI ĐOẠN 2: CHẠY ỨNG DỤNG
# Sử dụng một base image OpenJDK JRE 21 nhỏ gọn hơn
FROM eclipse-temurin:21-jre-alpine

# Đặt thư mục làm việc cho image cuối cùng
WORKDIR /app

# Tạo user và group không phải root để tăng cường bảo mật
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# TẠO THƯ MỤC UPLOAD VÀ CẤP QUYỀN CHO APPUSER
# Giả sử app.upload.dir của bạn là "uploads/product-images" (tương đối so với WORKDIR /app)
# Đường dẫn đầy đủ sẽ là /app/uploads/product-images
# Tạo thư mục cha /app/uploads trước, sau đó tạo thư mục con và cấp quyền cho toàn bộ cây /app/uploads
RUN mkdir -p /app/uploads/product-images && \
    chown -R appuser:appgroup /app/uploads && \
    chmod -R 755 /app/uploads 
    # Thêm chmod để đảm bảo user có quyền execute (cần thiết để vào thư mục) và read/write.

# Sao chép file .jar đã được build của module 'product' từ giai đoạn 'builder'.
# Đường dẫn đến file JAR trong giai đoạn builder là /app/product/target/
# LƯU Ý QUAN TRỌNG VỀ TÊN FILE JAR:
# Lệnh này sử dụng wildcard "product-service-*.jar".
# Đảm bảo rằng <artifactId> trong file product/pom.xml của bạn (ví dụ: <artifactId>product-service</artifactId>)
# sẽ tạo ra một file JAR có tên khớp với mẫu này (ví dụ: product-service-0.0.1-SNAPSHOT.jar).
# Nếu artifactId của bạn chỉ là "product", thì file JAR có thể là "product-0.0.1-SNAPSHOT.jar",
# khi đó bạn cần điều chỉnh lệnh COPY này thành "product-*.jar" hoặc tên cụ thể.
COPY --from=builder /app/product/target/product-service-*.jar app.jar
# Hoặc một cách chung hơn nếu chỉ có một JAR được tạo trong thư mục đó:
# COPY --from=builder /app/product/target/*.jar app.jar

# Thay đổi quyền sở hữu file JAR cho user không phải root
RUN chown appuser:appgroup app.jar

# Chuyển sang user không phải root để chạy ứng dụng
USER appuser

# Mở cổng mà ứng dụng Product Service sẽ lắng nghe (ví dụ: 8081).
# Đảm bảo cổng này khớp với server.port trong application.properties của module 'product'.
EXPOSE 8081

# Lệnh để chạy ứng dụng khi container được khởi động
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

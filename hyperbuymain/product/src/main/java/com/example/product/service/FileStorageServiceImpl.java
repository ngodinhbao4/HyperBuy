package com.example.product.service; // Hoặc package service.impl của bạn

import com.example.product.exception.AppException; // Import AppException của bạn
import com.example.product.exception.ErrorCode;   // Import ErrorCode của bạn
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    // Constructor nhận giá trị từ application.properties
    public FileStorageServiceImpl(@Value("${app.upload.dir:uploads/product-images}") String uploadDir) {
        // Nếu uploadDir là đường dẫn tương đối, nó sẽ được giải quyết từ thư mục gốc của ứng dụng khi chạy
        // Ví dụ: nếu ứng dụng chạy từ /opt/myapp, và uploadDir là "uploads", thì path sẽ là /opt/myapp/uploads
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        // Việc tạo thư mục sẽ được thực hiện trong phương thức init() với @PostConstruct
    }

    @Override
    @PostConstruct // Đảm bảo thư mục được tạo khi bean này được Spring khởi tạo và quản lý
    public void init() {
        try {
            Files.createDirectories(fileStorageLocation);
        } catch (IOException e) {
            // Ném AppException với ErrorCode tương ứng
            throw new AppException(ErrorCode.FILE_STORAGE_INITIALIZATION_ERROR);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            // Ném AppException cho trường hợp file rỗng
            throw new AppException(ErrorCode.EMPTY_FILE_ERROR);
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        try {
            // Kiểm tra null cho originalFilename trước khi gọi lastIndexOf
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
        } catch (Exception e) {
            // Bỏ qua nếu không có phần mở rộng hoặc có lỗi khi lấy phần mở rộng
            // Bạn có thể log lỗi này nếu muốn
        }

        // Tạo tên file duy nhất để tránh trùng lặp và các vấn đề về tên file
        String generatedFileName = UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = this.fileStorageLocation.resolve(generatedFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            // Ném AppException với ErrorCode tương ứng
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR);
        }

        return generatedFileName; // Trả về tên file đã lưu (không bao gồm đường dẫn đầy đủ)
    }

    @Override
    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                // Ném AppException nếu file không tồn tại hoặc không đọc được
                throw new AppException(ErrorCode.FILE_NOT_FOUND_ERROR);
            }
        } catch (MalformedURLException ex) {
            // Ném AppException cho trường hợp đường dẫn file bị lỗi
            throw new AppException(ErrorCode.MALFORMED_FILE_PATH_ERROR);
        }
    }

    @Override
    public Path getFilePath(String filename) {
        // Phương thức này vẫn hữu ích để lấy Path, không cần thay đổi nhiều
        return this.fileStorageLocation.resolve(filename).normalize();
    }

    @Override
    public void deleteFile(String filename) {
        if (filename == null || filename.isBlank()) {
            return; // Không làm gì nếu tên file trống hoặc null
        }
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            // Ném AppException với ErrorCode tương ứng
            throw new AppException(ErrorCode.FILE_DELETION_ERROR);
        }
    }
}
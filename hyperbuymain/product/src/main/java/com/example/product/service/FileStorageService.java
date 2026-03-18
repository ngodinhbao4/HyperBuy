package com.example.product.service; // Hoặc package service của bạn

import com.example.product.exception.AppException; // Import AppException của bạn
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
// Bỏ import java.io.IOException và java.net.MalformedURLException nếu không dùng trực tiếp ở interface

public interface FileStorageService {
    /**
     * Khởi tạo các thư mục lưu trữ cần thiết.
     * Phương thức này nên được gọi khi ứng dụng khởi động.
     */
    void init();

    /**
     * Lưu một file được tải lên.
     * @param file file được tải lên
     * @return tên file đã được tạo và lưu trữ duy nhất
     * @throws AppException nếu có lỗi khi lưu file
     */
    String storeFile(MultipartFile file);

    /**
     * Tải một file dưới dạng Resource.
     * @param filename tên của file cần tải
     * @return Resource tương ứng với file
     * @throws AppException nếu file không tìm thấy hoặc có lỗi khi tạo resource
     */
    Resource loadFileAsResource(String filename);

    /**
     * Lấy đường dẫn đầy đủ đến file.
     * @param filename tên file
     * @return Path đến file
     */
    Path getFilePath(String filename);

    /**
     * Xóa một file đã lưu trữ.
     * @param filename tên của file cần xóa
     * @throws AppException nếu có lỗi khi xóa file
     */
    void deleteFile(String filename);
}
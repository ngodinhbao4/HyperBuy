package com.example.product.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private static final Logger logger = LoggerFactory.getLogger(CloudinaryService.class);
    
    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Tải file lên Cloudinary và trả về URL ảnh an toàn (HTTPS).
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            logger.warn("Không có file nào được cung cấp hoặc file rỗng.");
            return null;
        }

        try {
            // Upload file to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "auto",
                    "folder", "hyperbuy_products" // Lưu trong thư mục hyperbuy_products trên Cloudinary
            ));

            String secureUrl = uploadResult.get("secure_url").toString();
            logger.info("Đã upload ảnh thành công lên Cloudinary: {}", secureUrl);
            return secureUrl;
        } catch (IOException e) {
            logger.error("Lỗi khi đọc file để upload lên Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Tải ảnh thất bại", e);
        } catch (Exception e) {
            logger.error("Lỗi từ Cloudinary khi upload: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi service tải ảnh", e);
        }
    }

    /**
     * Xóa ảnh trên Cloudinary bằng public_id.
     */
    public void deleteFile(String publicId) {
        if (publicId == null || publicId.isEmpty()) {
            return;
        }
        try {
            // Xác thực và loại bỏ public_id từ full URL nếu user lỡ truyền vào full url
            String exactPublicId = extractPublicId(publicId);
            if (exactPublicId == null) return;
            
            Map destroyResult = cloudinary.uploader().destroy(exactPublicId, ObjectUtils.emptyMap());
            logger.info("Đã xóa ảnh trên Cloudinary: {} - Kết quả: {}", exactPublicId, destroyResult.get("result"));
        } catch (Exception e) {
            logger.error("Gặp lỗi trong quá trình xóa ảnh trên Cloudinary (publicId: {}): {}", publicId, e.getMessage());
        }
    }
    
    /**
     * Helper pattern matching để lấy public_id (bao gồm cả thư mục) từ URL đầy đủ.
     * Chạy an toàn với các giá trị null hay URL không hợp lệ.
     */
    private String extractPublicId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        // Nếu đã là public_id chuẩn ko phải URL (vd: hyperbuy_products/abc1234)
        if (!url.startsWith("http")) {
            return url;
        }

        // Nếu là dường link (https://res.cloudinary.com/cloud_name/image/upload/v12345/hyperbuy_products/abcd.jpg)
        try {
            String[] parts = url.split("/");
            StringBuilder publicIdBuilder = new StringBuilder();
            
            boolean startAppend = false;
            for (String part : parts) {
                if (startAppend) {
                    if (publicIdBuilder.length() > 0) {
                        publicIdBuilder.append("/");
                    }
                    publicIdBuilder.append(part);
                }
                // Thường public_id bắt đầu sau phần `upload/v.../` 
                // hoặc sau `upload/` nếu không có version
                if (part.matches("v\\d+") || part.equals("upload")) {
                    if (part.equals("upload")) {
                         // wait for version or actual folder
                         startAppend = false; // logic simplified: we'll find folder like "hyperbuy_products" next
                    }
                    if (part.matches("v\\d+")) {
                        startAppend = true;
                    }
                } else if (part.equals("hyperbuy_products")) { // fallback
                     startAppend = true;
                     publicIdBuilder.append(part);
                }
            }
            
            String rawId = publicIdBuilder.toString();
            // Lọc bỏ phần mở rộng (vd: .jpg, .png)
            if (rawId.contains(".")) {
                rawId = rawId.substring(0, rawId.lastIndexOf('.'));
            }
            
            if (rawId.isEmpty()) {
                logger.warn("Không thể tách publicId từ URL: {}", url);
                return null; // fall back to not deleting rather than error
            }

            return rawId;
        } catch (Exception e) {
             logger.warn("Không thể lấy publicId từ: {}", url, e);
             return null;
        }
    }
}

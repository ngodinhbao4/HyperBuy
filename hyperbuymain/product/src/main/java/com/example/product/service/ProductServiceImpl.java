package com.example.product.service;

import com.example.product.client.UserServiceClient;
import com.example.product.dto.request.ProductRequest;
import com.example.product.dto.response.ProductResponse;
import com.example.product.dto.response.UserServiceResponse;
import com.example.product.entity.Product;
import com.example.product.entity.Category;
import com.example.product.repository.ProductRepository;
import com.example.product.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserServiceClient userServiceClient;
    private final Path rootLocation;
    private final CloudinaryService cloudinaryService; // Added CloudinaryService

    @Value("${app.static-resource.public-path-pattern}")
    private String publicPathPattern;

    public ProductServiceImpl(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            UserServiceClient userServiceClient,
            CloudinaryService cloudinaryService,
            @Value("${app.upload.dir}") String uploadDirConfiguration) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userServiceClient = userServiceClient;
        this.cloudinaryService = cloudinaryService;
        this.rootLocation = Paths.get(uploadDirConfiguration);
    }

    @PostConstruct
    public void init() {
        ensureUploadDirectoryExists();
    }

    private void ensureUploadDirectoryExists() {
        try {
            Files.createDirectories(rootLocation);
            logger.info("Thư mục upload đã được kiểm tra/tạo: {}", rootLocation.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Không thể khởi tạo vị trí lưu trữ: {}", rootLocation.toAbsolutePath(), e);
            throw new RuntimeException("Không thể khởi tạo vị trí lưu trữ", e);
        }
    }

    private String storeFile(MultipartFile file) {
        return cloudinaryService.uploadFile(file);
    }

    private void deleteStoredFile(String filename) {
        cloudinaryService.deleteFile(filename);
    }

    private ProductResponse convertToProductResponseWithImageUrl(Product product, String token) {
    ProductResponse dto = new ProductResponse();
    dto.setId(product.getId());
    dto.setName(product.getName());
    if (product.getDescription() != null) dto.setDescription(product.getDescription());
    if (product.getPrice() != null) dto.setPrice(product.getPrice());
    if (product.getSku() != null) dto.setSku(product.getSku());
    dto.setStockQuantity(product.getStockQuantity());
    dto.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
    dto.setActive(product.isActive());

    // Luôn khởi tạo sellerInfo với storeId
    ProductResponse.SellerInfo sellerInfo = new ProductResponse.SellerInfo();
    sellerInfo.setStoreId(product.getStoreId());

    // Gọi UserService để lấy userId và username
    if (token != null) {
        try {
            UserServiceResponse response = userServiceClient.getUserByStoreId(product.getStoreId(), token);
            if (response != null && response.getResult() != null) {
                sellerInfo.setUserId(response.getResult().getUserId());
                sellerInfo.setUsername(response.getResult().getUsername());
            }
        } catch (Exception e) {
            logger.warn("Không thể lấy userId và username cho storeId: {}. Lỗi: {}", product.getStoreId(), e.getMessage(), e);
        }
    }
    dto.setSellerInfo(sellerInfo);

    String storedImageIdentifier = product.getImageUrl();
    if (storedImageIdentifier != null && !storedImageIdentifier.isEmpty()) {
        if (storedImageIdentifier.startsWith("http")) {
            // Cloudinary URL (or any external URL)
            dto.setImageUrl(storedImageIdentifier);
        } else {
            // Legacy local filesystem image support
            // 🧹 Chuẩn hóa path, loại bỏ "files/" thừa
            storedImageIdentifier = storedImageIdentifier.replace("\\", "/");
            if (storedImageIdentifier.startsWith("/")) {
                storedImageIdentifier = storedImageIdentifier.substring(1);
            }
            if (storedImageIdentifier.startsWith("files/")) {
                storedImageIdentifier = storedImageIdentifier.substring("files/".length());
            }

            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            String cleanPublicPath = publicPathPattern.endsWith("/**")
                    ? publicPathPattern.substring(0, publicPathPattern.length() - 3)
                    : publicPathPattern;

            if (cleanPublicPath.endsWith("/")) {
                cleanPublicPath = cleanPublicPath.substring(0, cleanPublicPath.length() - 1);
            }
            if (!cleanPublicPath.startsWith("/")) {
                cleanPublicPath = "/" + cleanPublicPath;
            }

            String fullImageUrl = baseUrl + cleanPublicPath + "/" + storedImageIdentifier;
            dto.setImageUrl(fullImageUrl);
        }
    } else {
        dto.setImageUrl(null);
    }
    return dto;
}

    private Map<String, Object> convertToProductMap(Product product) {
    Map<String, Object> productMap = new HashMap<>();
    productMap.put("id", product.getId());
    productMap.put("name", product.getName());
    productMap.put("description", product.getDescription());
    productMap.put("price", product.getPrice());
    productMap.put("sku", product.getSku());
    productMap.put("stockQuantity", product.getStockQuantity());
    productMap.put("categoryId", product.getCategory() != null ? product.getCategory().getId() : null);
    productMap.put("active", product.isActive());

    Map<String, Object> sellerInfo = new HashMap<>();
    sellerInfo.put("storeId", product.getStoreId());
    productMap.put("sellerInfo", sellerInfo);

    String storedImageIdentifier = product.getImageUrl();
    if (storedImageIdentifier != null && !storedImageIdentifier.isEmpty()) {
        if (storedImageIdentifier.startsWith("http")) {
            // Cloudinary URL (or any external URL)
            productMap.put("imageUrl", storedImageIdentifier);
        } else {
            // Legacy local filesystem image support
            // 🧹 Chuẩn hóa path, loại bỏ "files/" thừa
            storedImageIdentifier = storedImageIdentifier.replace("\\", "/");
            if (storedImageIdentifier.startsWith("/")) {
                storedImageIdentifier = storedImageIdentifier.substring(1);
            }
            if (storedImageIdentifier.startsWith("files/")) {
                storedImageIdentifier = storedImageIdentifier.substring("files/".length());
            }

            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            String cleanPublicPath = publicPathPattern.endsWith("/**")
                    ? publicPathPattern.substring(0, publicPathPattern.length() - 3)
                    : publicPathPattern;

            if (cleanPublicPath.endsWith("/")) {
                cleanPublicPath = cleanPublicPath.substring(0, cleanPublicPath.length() - 1);
            }
            if (!cleanPublicPath.startsWith("/")) {
                cleanPublicPath = "/" + cleanPublicPath;
            }

            productMap.put("imageUrl", baseUrl + cleanPublicPath + "/" + storedImageIdentifier);
        }
    } else {
        productMap.put("imageUrl", null);
    }
    return productMap;
}

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest, MultipartFile imageFile, String token) {
        String storedFileName = storeFile(imageFile);

        String storeId = productRequest.getStoreId();
        if (storeId == null) {
            throw new RuntimeException("storeId không được để trống");
        }

        Product product = new Product();
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        if (productRequest.getPrice() != null) {
            product.setPrice(productRequest.getPrice());
        }
        product.setSku(productRequest.getSku());
        if (productRequest.getStockQuantity() != null) {
            product.setStockQuantity(productRequest.getStockQuantity());
        }

        if (productRequest.getCategoryId() != null) {
            Category category = categoryRepository.findById(productRequest.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Category với ID: " + productRequest.getCategoryId()));
            product.setCategory(category);
        }

        product.setStoreId(storeId);
        product.setActive(true);
        product.setDeleted(false);
        product.setImageUrl(storedFileName);

        Product savedProduct = productRepository.save(product);
        logger.info("Đã tạo sản phẩm với ID: {} và file ảnh (nếu có): {}", savedProduct.getId(), storedFileName);
        return convertToProductResponseWithImageUrl(savedProduct, token);
    }

    @Override
@Transactional
public ProductResponse updateProduct(Long id, ProductRequest productRequest, MultipartFile imageFile, String token) {
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

    String storeId = productRequest.getStoreId();

    // Nếu FE gửi storeId nhưng không trùng với storeId của sản phẩm -> chặn
    if (storeId != null && !product.getStoreId().equals(storeId)) {
        logger.error("Cửa hàng với storeId: {} không có quyền cập nhật sản phẩm với ID: {}", storeId, id);
        throw new RuntimeException("Bạn không có quyền cập nhật sản phẩm này");
    }

    // Nếu FE không gửi storeId -> chỉ log cảnh báo, không chặn (tránh 500)
    if (storeId == null) {
        logger.warn("Không nhận được storeId từ FE khi cập nhật sản phẩm ID: {}. Tạm thời bỏ qua kiểm tra quyền theo storeId.", id);
    }

    if (imageFile != null && !imageFile.isEmpty()) {
        deleteStoredFile(product.getImageUrl());
        String newStoredFileName = storeFile(imageFile);
        product.setImageUrl(newStoredFileName);
    }

    product.setName(productRequest.getName());
    product.setDescription(productRequest.getDescription());
    if (productRequest.getPrice() != null) {
        product.setPrice(productRequest.getPrice());
    }
    product.setSku(productRequest.getSku());
    if (productRequest.getStockQuantity() != null) {
        product.setStockQuantity(productRequest.getStockQuantity());
    }

    if (productRequest.getCategoryId() != null) {
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Category với ID: " + productRequest.getCategoryId()));
        product.setCategory(category);
    } else {
        product.setCategory(null);
    }

    Product updatedProduct = productRepository.save(product);
    logger.info("Đã cập nhật sản phẩm với ID: {}", updatedProduct.getId());
    return convertToProductResponseWithImageUrl(updatedProduct, token);
}

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id, String token) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        return convertToProductResponseWithImageUrl(product, token);
    }

    @Override
@Transactional(readOnly = true)
public Page<ProductResponse> findProducts(
        Long categoryId,
        String nameQuery,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Pageable pageable,
        String token
) {
    // 1) Chuẩn hoá các tham số filter
    Long normalizedCategoryId = normalizeCategoryId(categoryId);
    String normalizedNameQuery = normalizeNameQuery(nameQuery);
    BigDecimal[] normalizedPrices = normalizePriceRange(minPrice, maxPrice);
    BigDecimal normalizedMinPrice = normalizedPrices[0];
    BigDecimal normalizedMaxPrice = normalizedPrices[1];

    logger.info(
            "Tìm kiếm sản phẩm với categoryId={}, q='{}', minPrice={}, maxPrice={}, page={}, size={}",
            normalizedCategoryId, normalizedNameQuery, normalizedMinPrice, normalizedMaxPrice,
            pageable.getPageNumber(), pageable.getPageSize()
    );

    // 2) Gọi repository với tham số đã chuẩn hoá
    Page<Product> productPage = productRepository.searchProducts(
            normalizedCategoryId,
            normalizedNameQuery,
            normalizedMinPrice,
            normalizedMaxPrice,
            pageable
    );

    // 3) Map sang ProductResponse và giữ nguyên thông tin phân trang
    return productPage.map(product -> convertToProductResponseWithImageUrl(product, token));
}


    @Override
    @Transactional
    public void deleteProduct(Long id, String storeId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        if (storeId == null || !product.getStoreId().equals(storeId)) {
            logger.error("Cửa hàng với storeId: {} không có quyền xóa sản phẩm với ID: {}", storeId, id);
            throw new RuntimeException("Bạn không có quyền xóa sản phẩm này");
        }

        // Soft delete: keep the image for historical references
        product.setDeleted(true);
        product.setActive(false);
        productRepository.save(product);
        
        logger.info("Đã xóa (soft delete) sản phẩm với ID: {} bởi cửa hàng: {}", id, storeId);
    }

    @Override
    @Transactional
    public ProductResponse updateStock(Long id, int quantityChange, String token) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        int newStock = product.getStockQuantity() + quantityChange;
        if (newStock < 0) {
            throw new RuntimeException("Số lượng tồn kho không thể âm.");
        }
        product.setStockQuantity(newStock);
        Product updatedProduct = productRepository.save(product);
        logger.info("Đã cập nhật tồn kho cho sản phẩm ID: {} thành {}", id, newStock);
        return convertToProductResponseWithImageUrl(updatedProduct, token);
    }

    @Override
    @Transactional
    public void activateProduct(Long id, String storeId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        if (storeId == null || !product.getStoreId().equals(storeId)) {
            logger.error("Cửa hàng với storeId: {} không có quyền kích hoạt sản phẩm với ID: {}", storeId, id);
            throw new RuntimeException("Bạn không có quyền kích hoạt sản phẩm này");
        }

        product.setActive(true);
        product.setDeleted(false);
        productRepository.save(product);
        logger.info("Đã kích hoạt sản phẩm với ID: {} bởi cửa hàng: {}", id, storeId);
    }

    @Override
    @Transactional
    public void deactivateProduct(Long id, String storeId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        if (storeId == null || !product.getStoreId().equals(storeId)) {
            logger.error("Cửa hàng với storeId: {} không có quyền hủy kích hoạt sản phẩm với ID: {}", storeId, id);
            throw new RuntimeException("Bạn không có quyền hủy kích hoạt sản phẩm này");
        }

        product.setActive(false);
        productRepository.save(product);
        logger.info("Đã hủy kích hoạt sản phẩm với ID: {} bởi cửa hàng: {}", id, storeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> findMyProducts(Long categoryId, String nameQuery, Pageable pageable, String token) {
        logger.warn("findMyProducts chưa được triển khai đầy đủ với ngữ cảnh người dùng, hoạt động giống findProducts.");
        return findProducts(categoryId, nameQuery, null, null, pageable, token);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findProductsByStoreId(String storeId, Pageable pageable) {
        Page<Product> productPage = productRepository.findByStoreIdAndIsDeletedFalseAndIsActiveTrue(storeId, pageable);
        return productPage.getContent().stream()
                .map(this::convertToProductMap)
                .collect(Collectors.toList());
    }

    private String normalizeNameQuery(String nameQuery) {
    if (nameQuery == null) return null;
    nameQuery = nameQuery.trim();
    return nameQuery.isBlank() ? null : nameQuery;
}

// Chuẩn hoá categoryId: cho phép FE gửi 0 hoặc âm để hiểu là "tất cả"
private Long normalizeCategoryId(Long categoryId) {
    if (categoryId == null) return null;
    return (categoryId <= 0) ? null : categoryId;
}

// Chuẩn hoá min/max price: nếu bị đảo thì tự động đổi chỗ
private BigDecimal[] normalizePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
    if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
        BigDecimal tmp = minPrice;
        minPrice = maxPrice;
        maxPrice = tmp;
    }
    return new BigDecimal[]{minPrice, maxPrice};
}
}
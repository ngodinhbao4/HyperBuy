@FeignClient(name = "product-service", url = "${PRODUCT_SERVICE_URL}")
public interface ProductClient {
    @GetMapping("/api/v1/products/category/{categoryId}")
    List<ProductDTO> getByCategory(@PathVariable("categoryId") Long categoryId);
}

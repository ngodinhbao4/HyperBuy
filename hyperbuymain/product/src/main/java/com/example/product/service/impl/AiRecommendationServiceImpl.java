package com.example.product.service.impl;

import com.example.product.dto.response.ProductResponse;
import com.example.product.entity.Product;
import com.example.product.repository.ProductRepository;
import com.example.product.service.AiRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiRecommendationServiceImpl implements AiRecommendationService {

    // URL Python FastAPI recommender
    // Ví dụ: http://localhost:8000
    private static final String RECOMMENDER_BASE_URL = "http://host.docker.internal:8000";

    private final ProductRepository productRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<ProductResponse> getRecommendationsForUser(String username, int limit) {
        List<Long> productIds = callRecommenderForUser(username, limit);
        System.out.println("AI /recommend ids = " + productIds);

        if (productIds == null || productIds.isEmpty()) {
            productIds = callRecommenderForGuest(limit);
            System.out.println("AI /guest ids = " + productIds);
        }

        List<ProductResponse> rs = fetchProductsKeepOrder(productIds);
        System.out.println("Returned products size = " + rs.size());
        return rs;
    }


    @Override
    public List<ProductResponse> getRecommendationsForGuest(int limit) {
        List<Long> productIds = callRecommenderForGuest(limit);
        return fetchProductsKeepOrder(productIds);
    }

    // =========================
    // CALL PYTHON API
    // =========================
    private List<Long> callRecommenderForUser(String username, int limit) {
        try {
            String url = RECOMMENDER_BASE_URL + "/recommend?username=" + username + "&n=" + limit;

            ResponseEntity<List<Long>> res = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Long>>() {}
            );

            return res.getBody() != null ? res.getBody() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Long> callRecommenderForGuest(int limit) {
        try {
            String url = RECOMMENDER_BASE_URL + "/guest?n=" + limit;

            ResponseEntity<List<Long>> res = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Long>>() {}
            );

            return res.getBody() != null ? res.getBody() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    // =========================
    // DB FETCH + KEEP ORDER
    // =========================
    private List<ProductResponse> fetchProductsKeepOrder(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return List.of();

        System.out.println("AI ids = " + productIds);
        List<Product> products = productRepository.findAllById(productIds);
        System.out.println("Found products = " + products.size());


        Map<Long, Product> map = new HashMap<>();
        for (Product p : products) {
            map.put(p.getId(), p);
        }

        List<ProductResponse> result = new ArrayList<>();
        for (Long id : productIds) {
            Product p = map.get(id);
            if (p != null) {
                result.add(toProductResponse(p));
            }
        }
        return result;
        
    }

    // =========================
    // MAPPING (KHÔNG DÙNG builder)
    // =========================
    private ProductResponse toProductResponse(Product p) {
        // ⚠️ Nếu ProductResponse của bạn là record hoặc constructor khác,
        // bạn chỉnh lại đúng fields đang có.
        ProductResponse dto = new ProductResponse();

        // dưới đây là ví dụ phổ biến, bạn sửa theo class thực tế:
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setPrice(p.getPrice());
        dto.setImageUrl(p.getImageUrl());

        return dto;
    }

    @Override
    public List<ProductResponse> getSimilarProducts(Long productId, int limit) {
        List<Long> ids = callRecommenderSimilar(productId, limit);
        return fetchProductsKeepOrder(ids);
    }

    private List<Long> callRecommenderSimilar(Long productId, int limit) {
        try {
            String url = RECOMMENDER_BASE_URL + "/similar?product_id=" + productId + "&n=" + limit;

            ResponseEntity<List<Long>> res = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Long>>() {}
            );

            return res.getBody() != null ? res.getBody() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
}

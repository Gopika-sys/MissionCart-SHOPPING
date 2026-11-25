package com.missioncart.lifecycle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.missioncart.common.ApiResponse;
import com.missioncart.products.Product;
import com.missioncart.products.ProductRepository;
import com.missioncart.security.AuthenticatedUser;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@RestController
@RequestMapping("/api/lifecycle")
public class LifecycleController {

    public record LifecycleSubmitRequest(
        Long productId,
        Long orderId,
        String issueOrReason,
        String condition,
        Integer rentalDays,
        BigDecimal askingPrice,
        List<String> evidenceUrls
    ) {}

    public record LifecycleView(
        Long id,
        Long productId,
        String productName,
        Long orderId,
        String requestType,
        String status,
        String providerAdapterStatus,
        BigDecimal estimatedValue,
        String currency,
        Map<String, Object> details,
        java.time.Instant createdAt
    ) {}

    private final LifecycleRequestRepository repo;
    private final ProductRepository productRepo;
    private final ObjectMapper objectMapper;

    public LifecycleController(LifecycleRequestRepository repo, ProductRepository productRepo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.productRepo = productRepo;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ApiResponse<List<LifecycleView>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        List<LifecycleRequest> list = repo.findByUserIdOrderByCreatedAtDesc(user.id());
        Map<Long, String> names = new HashMap<>();
        productRepo.findAllById(list.stream().map(LifecycleRequest::getProductId).toList())
            .forEach(p -> names.put(p.getId(), p.getName()));

        return ApiResponse.ok(list.stream().map(lr -> new LifecycleView(
            lr.getId(), lr.getProductId(), names.getOrDefault(lr.getProductId(), "Product #" + lr.getProductId()),
            lr.getOrderId(), lr.getRequestType(), lr.getStatus(), lr.getProviderAdapterStatus(),
            lr.getEstimatedValue(), lr.getCurrency(), parseMap(lr.getDetailsJson()), lr.getCreatedAt()
        )).toList());
    }

    @PostMapping("/repair")
    public ApiResponse<LifecycleRequest> repair(@AuthenticationPrincipal AuthenticatedUser user, @RequestBody LifecycleSubmitRequest req) {
        return ApiResponse.ok("Repair request logged", create(user.id(), "REPAIR", req));
    }

    @PostMapping("/exchange")
    public ApiResponse<LifecycleRequest> exchange(@AuthenticationPrincipal AuthenticatedUser user, @RequestBody LifecycleSubmitRequest req) {
        return ApiResponse.ok("Exchange request logged", create(user.id(), "EXCHANGE", req));
    }

    @PostMapping("/rent")
    public ApiResponse<LifecycleRequest> rent(@AuthenticationPrincipal AuthenticatedUser user, @RequestBody LifecycleSubmitRequest req) {
        return ApiResponse.ok("Rental request logged", create(user.id(), "RENT", req));
    }

    @PostMapping("/resell")
    public ApiResponse<LifecycleRequest> resell(@AuthenticationPrincipal AuthenticatedUser user, @RequestBody LifecycleSubmitRequest req) {
        return ApiResponse.ok("Resale listing submitted", create(user.id(), "RESELL", req));
    }

    private LifecycleRequest create(Long userId, String type, LifecycleSubmitRequest req) {
        Product p = productRepo.findById(req.productId())
            .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        List<String> supported = parseList(p.getLifecycleOptionsJson());
        if (!supported.isEmpty() && supported.stream().noneMatch(s -> s.equalsIgnoreCase(type))) {
            throw new IllegalArgumentException(p.getName() + " does not support " + type + " lifecycle workflow.");
        }

        BigDecimal estimate = switch (type) {
            case "REPAIR" -> p.getPrice().multiply(BigDecimal.valueOf(0.12)).setScale(2, RoundingMode.HALF_UP);
            case "RENT" -> p.getPrice().multiply(BigDecimal.valueOf(0.03))
                .multiply(BigDecimal.valueOf(Math.max(1, req.rentalDays() != null ? req.rentalDays() : 7)))
                .setScale(2, RoundingMode.HALF_UP);
            case "RESELL" -> req.askingPrice() != null ? req.askingPrice()
                : p.getPrice().multiply(BigDecimal.valueOf(0.55)).setScale(2, RoundingMode.HALF_UP);
            default -> p.getPrice();
        };

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("issueOrReason", req.issueOrReason() != null ? req.issueOrReason() : "");
        if (req.condition() != null) details.put("condition", req.condition());
        if (req.rentalDays() != null) details.put("rentalDays", req.rentalDays());
        details.put("workflowNote", "Logged in transparent request queue — external service provider adapter pending.");

        LifecycleRequest lr = new LifecycleRequest();
        lr.setUserId(userId);
        lr.setProductId(p.getId());
        lr.setOrderId(req.orderId());
        lr.setRequestType(type);
        lr.setStatus("REQUEST_LOGGED_TRANSPARENT");
        lr.setProviderAdapterStatus("AWAITING_PROVIDER_ADAPTER");
        lr.setEstimatedValue(estimate);
        lr.setCurrency("INR");
        lr.setDetailsJson(writeJson(details));
        lr.setEvidenceUrlsJson(writeJson(req.evidenceUrls() != null ? req.evidenceUrls() : List.of()));
        return repo.save(lr);
    }

    private Map<String, Object> parseMap(String json) {
        try { return objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception e) { return Map.of(); }
    }

    private List<String> parseList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception e) { return List.of(); }
    }

    private String writeJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "{}"; }
    }
}

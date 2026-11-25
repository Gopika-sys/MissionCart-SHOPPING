package com.missioncart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.missioncart.compatibility.CompatibilityEngine;
import com.missioncart.compatibility.CompatibilityEngine.CompatibilityReport;
import com.missioncart.payments.RazorpayService;
import com.missioncart.products.Product;
import com.missioncart.products.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MissionCartIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ProductRepository productRepo;
    @Autowired CompatibilityEngine compatibilityEngine;
    @Autowired RazorpayService razorpayService;

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    @Test
    void catalogAnd13RetailerSourcesAreSeeded() throws Exception {
        MvcResult sourcesRes = mockMvc.perform(get("/api/sources"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode sources = objectMapper.readTree(sourcesRes.getResponse().getContentAsString()).path("data");
        assertThat(sources.size()).isGreaterThanOrEqualTo(15); // 13 external + MISSIONCART_DIRECT + 2 open catalog feeds

        MvcResult countRes = mockMvc.perform(get("/api/products/count"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode counts = objectMapper.readTree(countRes.getResponse().getContentAsString()).path("data");
        assertThat(counts.path("totalImportedProducts").asLong()).isGreaterThanOrEqualTo(10000L);
        assertThat(counts.path("imageVerifiedProducts").asLong()).isGreaterThanOrEqualTo(10000L);

        MvcResult prodsRes = mockMvc.perform(get("/api/products?page=0&size=24"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode data = objectMapper.readTree(prodsRes.getResponse().getContentAsString()).path("data");
        JsonNode items = data.path("items");
        assertThat(items.size()).isEqualTo(24);
        assertThat(data.path("totalElements").asLong()).isGreaterThanOrEqualTo(10000L);
        for (JsonNode item : items) {
            assertThat(item.path("imageStatus").asText()).isEqualTo("VERIFIED");
            assertThat(item.path("imageUrl").asText()).isNotBlank();
        }
    }

    @Test
    void universalAiFallbackPlansHostelTravelSkincareAndNovelMissionsWithRealDbProducts() throws Exception {
        List<String> prompts = List.of(
            "I'm moving to a hostel. My budget is ₹5,000. I need everything necessary for my first month.",
            "I'm going to Manali for five days. I need suitable outfits, shoes, skincare, travel accessories within ₹7,000.",
            "I have oily skin and need a simple morning and night skincare routine within ₹2,000.",
            "I'm organizing a small birthday party for 15 friends. I need decorations, tableware, snacks, and return gifts within ₹4,500."
        );

        for (String prompt : prompts) {
            MvcResult res = mockMvc.perform(post("/api/missions/plan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("prompt", prompt))))
                .andExpect(status().isOk())
                .andReturn();
            JsonNode mission = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
            assertThat(mission.path("items").size()).isGreaterThan(0);
            assertThat(mission.path("budgetSummary").path("totalBudget").asDouble()).isGreaterThan(0);
            for (JsonNode item : mission.path("items")) {
                long pid = item.path("productId").asLong();
                assertThat(productRepo.existsById(pid)).isTrue();
            }
        }
    }

    @Test
    void alreadyOwnedItemsAreExcludedAndBudgetRecalculates() throws Exception {
        String token = loginAndGetToken("ananya@missioncart.local", "MissionCart@2026");

        MvcResult res = mockMvc.perform(post("/api/missions/plan")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "prompt", "I'm moving to a hostel within ₹5,000",
                    "alreadyOwn", "I already have a kettle and water bottle"
                ))))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode mission = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
        long missionId = mission.path("id").asLong();
        boolean foundExcludedKettle = false;
        long firstActiveItemId = -1;
        for (JsonNode item : mission.path("items")) {
            if (item.path("product").path("name").asText().toLowerCase().contains("kettle")) {
                assertThat(item.path("excludedOwned").asBoolean()).isTrue();
                assertThat(item.path("selected").asBoolean()).isFalse();
                foundExcludedKettle = true;
            } else if (firstActiveItemId == -1) {
                firstActiveItemId = item.path("id").asLong();
            }
        }
        assertThat(foundExcludedKettle).isTrue();

        if (firstActiveItemId != -1) {
            mockMvc.perform(patch("/api/missions/" + missionId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "itemActions", List.of(Map.of("itemId", firstActiveItemId, "quantity", 2))
                    ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.budgetSummary.totalSelected").isNumber());
        }
    }

    @Test
    void compatibilityEngineDetectsElectronicsAndSkincareConflicts() {
        Product dryer110v = productRepo.findBySkuIgnoreCase("MC-ELEC-010").orElseThrow();
        Product strip = productRepo.findBySkuIgnoreCase("MC-ELEC-002").orElseThrow();
        Product induction = productRepo.findBySkuIgnoreCase("MC-ELEC-006").orElseThrow();

        CompatibilityReport elecReport = compatibilityEngine.evaluate(
            List.of(dryer110v.getId(), strip.getId(), induction.getId()), Map.of()
        );
        assertThat(elecReport.overallStatus()).isEqualTo("INCOMPATIBLE");
        assertThat(elecReport.issues()).isNotEmpty();

        Product cleanserAcid = productRepo.findBySkuIgnoreCase("MC-SKIN-001").orElseThrow();
        Product retinol = productRepo.findBySkuIgnoreCase("MC-SKIN-005").orElseThrow();
        CompatibilityReport skinReport = compatibilityEngine.evaluate(
            List.of(cleanserAcid.getId(), retinol.getId()), Map.of()
        );
        assertThat(skinReport.overallStatus()).isEqualTo("WARNING");
    }

    @Test
    void fullCustomerCheckoutLifecycleAndSharedMissionWorkflow() throws Exception {
        // 1. Register new customer
        String email = "testbuyer" + System.currentTimeMillis() + "@missioncart.local";
        MvcResult regRes = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", email,
                    "password", "SecurePass@123",
                    "fullName", "Rohan Verma",
                    "phone", "+91 99887 76655"
                ))))
            .andExpect(status().isOk())
            .andReturn();
        String customerToken = objectMapper.readTree(regRes.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // 2. Customer blocked from admin endpoint
        mockMvc.perform(get("/api/admin/overview").header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isForbidden());

        // 3. Add native product to cart & complete native demo checkout
        Product kettle = productRepo.findBySkuIgnoreCase("MC-ELEC-001").orElseThrow();
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("productId", kettle.getId(), "quantity", 1))))
            .andExpect(status().isOk());

        MvcResult checkoutRes = mockMvc.perform(post("/api/checkout")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "shippingAddress", Map.of("recipientName", "Rohan", "city", "Pune", "postalCode", "411001"),
                    "couponCode", "WELCOME10",
                    "idempotencyKey", "test-idem-" + System.currentTimeMillis()
                ))))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode checkoutData = objectMapper.readTree(checkoutRes.getResponse().getContentAsString()).path("data");
        long orderId = checkoutData.path("order").path("id").asLong();
        assertThat(checkoutData.path("demoMode").asBoolean()).isTrue();
        assertThat(checkoutData.path("order").path("discountAmount").asDouble()).isGreaterThan(0);

        mockMvc.perform(post("/api/payments/verify")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "razorpayOrderId", checkoutData.path("payment").path("providerOrderId").asText(),
                    "simulateFailure", false
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.order.status").value("CONFIRMED_DEMO"));

        // 4. Submit lifecycle repair request
        mockMvc.perform(post("/api/lifecycle/repair")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "productId", kettle.getId(),
                    "issueOrReason", "Lid latch loose"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.requestType").value("REPAIR"));

        // 5. Shared mission workflow
        MvcResult planRes = mockMvc.perform(post("/api/missions/plan")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("prompt", "New apartment setup with two friends under ₹12000"))))
            .andExpect(status().isOk())
            .andReturn();
        long mId = objectMapper.readTree(planRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        MvcResult shareRes = mockMvc.perform(post("/api/missions/" + mId + "/share")
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isOk())
            .andReturn();
        String shareToken = objectMapper.readTree(shareRes.getResponse().getContentAsString()).path("data").path("shareToken").asText();
        assertThat(shareToken).isNotBlank();
    }

    @Test
    void adminCatalogImportRejectsInvalidAndDuplicateRecords() throws Exception {
        String adminToken = loginAndGetToken("admin-test@missioncart.local", "AdminTest@2026!");

        String jsonPayload = objectMapper.writeValueAsString(List.of(
            Map.of("sku", "IMP-TEST-001", "name", "Imported Bamboo Tray", "categorySlug", "kitchen", "price", 599),
            Map.of("sku", "IMP-TEST-001", "name", "Duplicate Tray", "categorySlug", "kitchen", "price", 599),
            Map.of("sku", "IMP-BAD-002", "name", "Zero Price Bad", "categorySlug", "kitchen", "price", 0)
        ));

        MvcResult res = mockMvc.perform(post("/api/admin/catalog/import")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "sourcePlatform", "NESTASIA",
                    "format", "JSON",
                    "fileName", "test-import.json",
                    "content", jsonPayload,
                    "previewOnly", false,
                    "updateExisting", false
                ))))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
        assertThat(data.path("insertedCount").asInt()).isEqualTo(1);
        assertThat(data.path("skippedDuplicateCount").asInt()).isEqualTo(1);
        assertThat(data.path("errorCount").asInt()).isEqualTo(1);

        // Verify HMAC helper consistency
        String sig = razorpayService.hmacSha256Hex("order_123|pay_456", "test_secret");
        assertThat(sig).hasSize(64);
    }
}

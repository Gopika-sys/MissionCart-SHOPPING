package com.missioncart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.missioncart.budget.BudgetPlannerService;
import com.missioncart.budget.BudgetPlannerService.BudgetSummary;
import com.missioncart.config.AppProperties;
import com.missioncart.missions.MissionItem;
import com.missioncart.payments.RazorpayService;
import com.missioncart.products.Product;
import com.missioncart.products.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "missioncart.razorpay.key-id=rzp_test_missioncart123",
    "missioncart.razorpay.key-secret=rzp_test_secret_abc123456",
    "missioncart.razorpay.webhook-secret=rzp_webhook_secret_xyz789"
})
class UniversalMissionBudgetAndPaymentTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ProductRepository productRepo;
    @Autowired BudgetPlannerService budgetService;
    @MockitoSpyBean RazorpayService razorpayService;

    private String registerCustomer(String prefix) throws Exception {
        String email = prefix + System.nanoTime() + "@missioncart.local";
        MvcResult res = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", email,
                    "password", "StrongPass@123",
                    "fullName", "Test Buyer"
                ))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    @Test
    void universalPlannerHandles20DiverseSituationsWithoutHallucinatingProducts() throws Exception {
        List<String> twentySituations = List.of(
            "I need affordable furniture and lighting for my small bedroom within ₹9,000.",
            "I'm a college student building a productive study setup within ₹8,000.",
            "Suggest outfits and accessories for a five-day trip to Manali within ₹7,000.",
            "I need a complete oily skincare routine under ₹2,000.",
            "I need smart fashion outfits and formal shoes under ₹6,000.",
            "I need a complete kitchen setup for three people under ₹6,000.",
            "I need products for a beginner home workout under ₹3,000.",
            "I need everyday electronics like power strip, headphones and power bank under ₹5,000.",
            "I need a budget-friendly wedding outfit with accessories within ₹6,000.",
            "I need to organize a birthday party for 30 people under ₹5,000.",
            "I am moving into a new house. Help me buy essentials within ₹15,000.",
            "I need monthly grocery staples like coffee, tea, and oats within ₹1,500.",
            "I want to create a productive work-from-home desk setup within ₹6,000.",
            "I need pet essentials for my puppy under ₹2,000.",
            "Suggest a suitable gift for my best friend's birthday under ₹1,500.",
            "Help me prepare for my first office job within ₹10,000.",
            "I need compact storage solutions for my closet within ₹2,500.",
            "I want a home entertainment speaker and cozy lighting setup within ₹4,000.",
            "I am preparing for an outdoor weekend camping trip within ₹5,000.",
            "I am hosting an evening pottery and chai gathering on my balcony within ₹3,000."
        );

        assertThat(twentySituations).hasSize(20);

        for (String prompt : twentySituations) {
            MvcResult res = mockMvc.perform(post("/api/missions/plan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("prompt", prompt))))
                .andExpect(status().isOk())
                .andReturn();
            JsonNode mission = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
            assertThat(mission.path("id").asLong()).isPositive();
            // Every recommended product ID must exist in DB
            for (JsonNode item : mission.path("items")) {
                assertThat(productRepo.existsById(item.path("productId").asLong())).isTrue();
            }
            // Note: pet essentials / camping tent may honestly return unmatchedRequirements rather than fake matches
            int totalSignals = mission.path("items").size() + mission.path("unmatchedRequirements").size();
            assertThat(totalSignals).isGreaterThan(0);
        }
    }

    @Test
    void budgetEngineHandlesMissingExpiredPricesShippingTaxesCouponsAndDuplicates() {
        Product pricedKettle = productRepo.findBySkuIgnoreCase("MC-ELEC-001").orElseThrow();
        Product unavailableOpenProd = new Product();
        unavailableOpenProd.setSku("UNAVAIL-1");
        unavailableOpenProd.setName("Unpriced Open Product");
        unavailableOpenProd.setPrice(null);
        unavailableOpenProd.setPriceStatus("UNAVAILABLE");

        Product expiredProd = new Product();
        expiredProd.setSku("EXP-1");
        expiredProd.setName("Expired Price Product");
        expiredProd.setPrice(BigDecimal.valueOf(500));
        expiredProd.setPriceStatus("EXPIRED");

        MissionItem i1 = new MissionItem();
        i1.setProductId(pricedKettle.getId());
        i1.setPriority("ESSENTIAL");
        i1.setQuantity(1);
        i1.setSelected(true);

        // Duplicate product item should never double-count
        MissionItem i1Dup = new MissionItem();
        i1Dup.setProductId(pricedKettle.getId());
        i1Dup.setPriority("ESSENTIAL");
        i1Dup.setQuantity(1);
        i1Dup.setSelected(true);

        MissionItem iUnavail = new MissionItem();
        iUnavail.setProductId(999991L);
        iUnavail.setPriority("OPTIONAL");
        iUnavail.setQuantity(1);
        iUnavail.setSelected(true);

        MissionItem iExp = new MissionItem();
        iExp.setProductId(999992L);
        iExp.setPriority("OPTIONAL");
        iExp.setQuantity(1);
        iExp.setSelected(true);

        Map<Long, Product> map = Map.of(
            pricedKettle.getId(), pricedKettle,
            999991L, unavailableOpenProd,
            999992L, expiredProd
        );

        // ₹5,000 budget check
        BudgetSummary summary = budgetService.calculateWithCoupon(
            BigDecimal.valueOf(5000),
            List.of(i1, i1Dup, iUnavail, iExp),
            map,
            BigDecimal.valueOf(100)
        );
        // Kettle ₹1299 (counted once!), shipping ₹0 (>=999), tax 18% of 1299 = ₹233.82, coupon ₹100
        assertThat(summary.productSubtotal()).isEqualByComparingTo("1299.00");
        assertThat(summary.shippingCost()).isEqualByComparingTo("0.00");
        assertThat(summary.applicableTaxes()).isEqualByComparingTo("233.82");
        assertThat(summary.finalPayable()).isEqualByComparingTo("1432.82");
        assertThat(summary.remainingBudget()).isEqualByComparingTo("3567.18");
        assertThat(summary.unpricedItemCount()).isEqualTo(2);
        assertThat(summary.affordableConfirmed()).isFalse();

        // ₹1,000 budget overflow check
        BudgetSummary tight = budgetService.calculate(BigDecimal.valueOf(1000), List.of(i1), Map.of(pricedKettle.getId(), pricedKettle));
        assertThat(tight.overBudget()).isTrue();
        assertThat(tight.remainingBudget()).isNegative();
    }

    @Test
    void razorpayTestModeSignatureWebhookIdempotencyRefundAndSecurityChecks() throws Exception {
        doReturn("order_rzp_test_999").when(razorpayService).createProviderOrder(any(), any());
        doReturn(Map.of("refundId", "rfnd_test_001", "status", "PROCESSED"))
            .when(razorpayService).initiateGatewayRefund(any(), any());
        // Payment config never exposes secret keys
        mockMvc.perform(get("/api/payments/config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.mode").value("TEST_MODE"))
            .andExpect(jsonPath("$.data.razorpayConfigured").value(true))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("rzp_test_secret"))));

        String buyerAToken = registerCustomer("buyera");
        String buyerBToken = registerCustomer("buyerb");

        Product strip = productRepo.findBySkuIgnoreCase("MC-ELEC-002").orElseThrow();
        int initialStock = strip.getStockQuantity();

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + buyerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("productId", strip.getId(), "quantity", 1))))
            .andExpect(status().isOk());

        // Invalid PIN rejected
        mockMvc.perform(post("/api/checkout")
                .header("Authorization", "Bearer " + buyerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "shippingAddress", Map.of("recipientName", "A", "postalCode", "00012"),
                    "idempotencyKey", "bad-pin-test"
                ))))
            .andExpect(status().isBadRequest());

        String idemKey = "rzp-idem-" + System.nanoTime();
        MvcResult initRes = mockMvc.perform(post("/api/checkout")
                .header("Authorization", "Bearer " + buyerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "shippingAddress", Map.of("recipientName", "Buyer A", "city", "Bengaluru", "postalCode", "560038"),
                    "idempotencyKey", idemKey
                ))))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode initJson = objectMapper.readTree(initRes.getResponse().getContentAsString()).path("data");
        long orderId = initJson.path("order").path("id").asLong();
        String rzpOrderId = initJson.path("payment").path("providerOrderId").asText();
        BigDecimal serverTotal = initJson.path("order").path("totalAmount").decimalValue();

        // Duplicate order with same idempotency key returns same order ID
        MvcResult dupRes = mockMvc.perform(post("/api/checkout")
                .header("Authorization", "Bearer " + buyerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "shippingAddress", Map.of("recipientName", "Buyer A", "city", "Bengaluru", "postalCode", "560038"),
                    "idempotencyKey", idemKey
                ))))
            .andExpect(status().isOk())
            .andReturn();
        assertThat(objectMapper.readTree(dupRes.getResponse().getContentAsString()).path("data").path("order").path("id").asLong()).isEqualTo(orderId);

        // Invalid Razorpay signature rejected
        mockMvc.perform(post("/api/payments/verify")
                .header("Authorization", "Bearer " + buyerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "razorpayOrderId", rzpOrderId,
                    "razorpayPaymentId", "pay_test_111",
                    "razorpaySignature", "tampered_invalid_sig"
                ))))
            .andExpect(status().isBadRequest());

        // Payment amount mismatch rejected
        mockMvc.perform(post("/api/payments/verify")
                .header("Authorization", "Bearer " + buyerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "razorpayOrderId", rzpOrderId,
                    "razorpayPaymentId", "pay_test_111",
                    "razorpaySignature", razorpayService.hmacSha256Hex(rzpOrderId + "|pay_test_111", "rzp_test_secret_abc123456"),
                    "paidAmountInr", 1.00
                ))))
            .andExpect(status().isBadRequest());

        // Valid Razorpay Test Mode HMAC signature confirms order & deducts stock once
        String validSig = razorpayService.hmacSha256Hex(rzpOrderId + "|pay_test_111", "rzp_test_secret_abc123456");
        mockMvc.perform(post("/api/payments/verify")
                .header("Authorization", "Bearer " + buyerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "razorpayOrderId", rzpOrderId,
                    "razorpayPaymentId", "pay_test_111",
                    "razorpaySignature", validSig,
                    "paidAmountInr", serverTotal
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.order.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.data.payment.status").value("CAPTURED"));

        assertThat(productRepo.findById(strip.getId()).orElseThrow().getStockQuantity()).isEqualTo(initialStock - 1);

        // Duplicate webhook notification does NOT deduct stock twice
        String webhookBody = objectMapper.writeValueAsString(Map.of(
            "event", "payment.captured",
            "payload", Map.of("payment", Map.of("entity", Map.of(
                "id", "pay_test_111",
                "order_id", rzpOrderId,
                "amount", serverTotal.multiply(BigDecimal.valueOf(100)).longValueExact()
            )))
        ));
        String whSig = razorpayService.hmacSha256Hex(webhookBody, "rzp_webhook_secret_xyz789");
        mockMvc.perform(post("/api/webhooks/razorpay")
                .header("X-Razorpay-Signature", whSig)
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookBody))
            .andExpect(status().isOk());

        assertThat(productRepo.findById(strip.getId()).orElseThrow().getStockQuantity()).isEqualTo(initialStock - 1);

        // Security: Buyer B cannot view Buyer A's order or invoice
        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + buyerBToken))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/orders/" + orderId + "/invoice").header("Authorization", "Bearer " + buyerBToken))
            .andExpect(status().isBadRequest());

        // Buyer A can retrieve GST invoice
        mockMvc.perform(get("/api/orders/" + orderId + "/invoice").header("Authorization", "Bearer " + buyerAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderNumber").isNotEmpty())
            .andExpect(jsonPath("$.data.totalAmount").value(serverTotal.doubleValue()));

        // Buyer A can request refund through gateway adapter
        mockMvc.perform(post("/api/orders/" + orderId + "/refund")
                .header("Authorization", "Bearer " + buyerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("reason", "Changed mind"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.order.status").value("REFUNDED"))
            .andExpect(jsonPath("$.data.payment.refundId").value("rfnd_test_001"));
    }
}

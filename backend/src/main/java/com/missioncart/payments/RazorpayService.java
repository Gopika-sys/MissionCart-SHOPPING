package com.missioncart.payments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.missioncart.config.AppProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

@Service
public class RazorpayService {

    private final AppProperties props;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public RazorpayService(AppProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return props.razorpay().keyId() != null && !props.razorpay().keyId().isBlank()
            && props.razorpay().keySecret() != null && !props.razorpay().keySecret().isBlank();
    }

    public boolean isWebhookConfigured() {
        return props.razorpay().webhookSecret() != null && !props.razorpay().webhookSecret().isBlank();
    }

    public String getPaymentMode() {
        if (!isConfigured()) return "LOCAL_DEMO";
        if (props.razorpay().keyId().startsWith("rzp_live_") && isWebhookConfigured()) {
            return "LIVE_MODE";
        }
        return "TEST_MODE";
    }

    public String getKeyId() {
        return props.razorpay().keyId();
    }

    public String createProviderOrder(String receiptNumber, BigDecimal amountInr) {
        if (!isConfigured()) {
            return "demo_rzp_order_" + receiptNumber;
        }
        try {
            long paise = amountInr.multiply(BigDecimal.valueOf(100)).longValueExact();
            String auth = Base64.getEncoder().encodeToString((props.razorpay().keyId() + ":" + props.razorpay().keySecret()).getBytes(StandardCharsets.UTF_8));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + auth);
            Map<String, Object> body = Map.of(
                "amount", paise,
                "currency", "INR",
                "receipt", receiptNumber,
                "payment_capture", 1
            );
            String res = restTemplate.postForObject("https://api.razorpay.com/v1/orders", new HttpEntity<>(body, headers), String.class);
            JsonNode node = objectMapper.readTree(res);
            return node.path("id").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Razorpay order: " + e.getMessage());
        }
    }

    public Map<String, String> initiateGatewayRefund(String razorpayPaymentId, BigDecimal amountInr) {
        if (!isConfigured()) {
            return Map.of("status", "PENDING_GATEWAY_CONFIGURATION", "refundId", "");
        }
        try {
            long paise = amountInr.multiply(BigDecimal.valueOf(100)).longValueExact();
            String auth = Base64.getEncoder().encodeToString((props.razorpay().keyId() + ":" + props.razorpay().keySecret()).getBytes(StandardCharsets.UTF_8));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + auth);
            String url = "https://api.razorpay.com/v1/payments/" + razorpayPaymentId + "/refund";
            String res = restTemplate.postForObject(url, new HttpEntity<>(Map.of("amount", paise), headers), String.class);
            JsonNode node = objectMapper.readTree(res);
            return Map.of(
                "refundId", node.path("id").asText(""),
                "status", node.path("status").asText("processed").toUpperCase()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Razorpay refund failed: " + e.getMessage());
        }
    }

    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String signature) {
        if (!isConfigured() || razorpayOrderId == null || razorpayPaymentId == null || signature == null) return false;
        String payload = razorpayOrderId + "|" + razorpayPaymentId;
        return constantTimeEquals(hmacSha256Hex(payload, props.razorpay().keySecret()), signature);
    }

    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        String secret = props.razorpay().webhookSecret();
        if (secret == null || secret.isBlank() || signatureHeader == null) return false;
        return constantTimeEquals(hmacSha256Hex(rawBody, secret), signatureHeader);
    }

    public String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC calculation failed", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}

package com.missioncart.checkout;

import com.missioncart.checkout.CheckoutAndOrderService.*;
import com.missioncart.common.ApiResponse;
import com.missioncart.payments.RazorpayService;
import com.missioncart.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CheckoutController {

    private final CheckoutAndOrderService service;
    private final RazorpayService razorpayService;

    public CheckoutController(CheckoutAndOrderService service, RazorpayService razorpayService) {
        this.service = service;
        this.razorpayService = razorpayService;
    }

    public record CheckoutRequest(Map<String, Object> shippingAddress, String couponCode, Long missionId, String idempotencyKey) {}
    public record VerifyPaymentRequest(Long orderId, String razorpayOrderId, String razorpayPaymentId, String razorpaySignature, BigDecimal paidAmountInr, boolean simulateFailure) {}
    public record CancelRequest(String reason) {}
    public record RefundRequest(BigDecimal amount, String reason) {}

    @GetMapping("/payments/config")
    public ApiResponse<Map<String, Object>> paymentConfig() {
        String mode = razorpayService.getPaymentMode();
        boolean configured = razorpayService.isConfigured();
        return ApiResponse.ok(Map.of(
            "mode", mode,
            "razorpayConfigured", configured,
            "webhookConfigured", razorpayService.isWebhookConfigured(),
            "methods", configured ? List.of("UPI", "CARDS", "NETBANKING", "WALLETS") : List.of("LOCAL_DEMO_ONLY")
        ));
    }

    @PostMapping("/checkout")
    public ApiResponse<CheckoutInitResponse> checkout(@AuthenticationPrincipal AuthenticatedUser user, @RequestBody CheckoutRequest req) {
        return ApiResponse.ok("Checkout initialized", service.initiateCheckout(user.id(), req.shippingAddress(), req.couponCode(), req.missionId(), req.idempotencyKey()));
    }

    @PostMapping("/payments/create-order")
    public ApiResponse<CheckoutInitResponse> createOrder(@AuthenticationPrincipal AuthenticatedUser user, @RequestBody CheckoutRequest req) {
        return ApiResponse.ok("Payment order created", service.initiateCheckout(user.id(), req.shippingAddress(), req.couponCode(), req.missionId(), req.idempotencyKey()));
    }

    @PostMapping("/payments/verify")
    public ApiResponse<OrderDetailDto> verify(@AuthenticationPrincipal AuthenticatedUser user, @RequestBody VerifyPaymentRequest req) {
        return ApiResponse.ok("Payment processed", service.verifyPayment(
            user.id(), req.orderId(), req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature(), req.paidAmountInr(), req.simulateFailure()
        ));
    }

    @PostMapping("/webhooks/razorpay")
    public ApiResponse<Void> webhook(@RequestBody String rawBody, @RequestHeader(value = "X-Razorpay-Signature", required = false) String sig) {
        service.handleRazorpayWebhook(rawBody, sig);
        return ApiResponse.ok("Webhook verified", null);
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderDetailDto>> myOrders(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.listOrders(user.id()));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<OrderDetailDto> orderDetail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return ApiResponse.ok(service.getOrderDetail(id, user.id()));
    }

    @PostMapping("/orders/{id}/cancel")
    public ApiResponse<OrderDetailDto> cancelOrder(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, @RequestBody(required = false) CancelRequest req) {
        return ApiResponse.ok("Order cancelled", service.cancelOrder(user.id(), id, req != null ? req.reason() : null));
    }

    @PostMapping("/orders/{id}/refund")
    public ApiResponse<OrderDetailDto> requestRefund(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, @RequestBody(required = false) RefundRequest req) {
        return ApiResponse.ok("Refund processed", service.requestRefund(user.id(), id, req != null ? req.amount() : null, req != null ? req.reason() : null));
    }

    @GetMapping("/orders/{id}/invoice")
    public ApiResponse<InvoiceDto> invoice(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return ApiResponse.ok(service.generateInvoice(user.id(), id));
    }
}

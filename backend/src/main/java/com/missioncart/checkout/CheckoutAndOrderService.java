package com.missioncart.checkout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.missioncart.cart.*;
import com.missioncart.orders.*;
import com.missioncart.payments.RazorpayService;
import com.missioncart.products.InventoryHistory;
import com.missioncart.products.InventoryHistoryRepository;
import com.missioncart.products.Product;
import com.missioncart.products.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class CheckoutAndOrderService {

    public record CheckoutInitResponse(
        Order order,
        Payment payment,
        boolean demoMode,
        String paymentMode,
        String razorpayKeyId,
        long amountPaise,
        String notice
    ) {}

    public record OrderDetailDto(
        Order order,
        List<OrderItem> items,
        Payment payment,
        List<PaymentAuditLog> auditLogs
    ) {}

    public record InvoiceDto(
        String invoiceNumber,
        String orderNumber,
        Instant issuedAt,
        String orderStatus,
        String paymentStatus,
        String paymentProvider,
        String paymentReference,
        List<OrderItem> items,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String currency,
        String shippingAddressJson
    ) {}

    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ProductRepository productRepo;
    private final CouponRepository couponRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final PaymentRepository paymentRepo;
    private final PaymentAuditLogRepository auditLogRepo;
    private final InventoryHistoryRepository invHistoryRepo;
    private final RazorpayService razorpayService;
    private final ObjectMapper objectMapper;

    public CheckoutAndOrderService(
        CartRepository cartRepo,
        CartItemRepository cartItemRepo,
        ProductRepository productRepo,
        CouponRepository couponRepo,
        OrderRepository orderRepo,
        OrderItemRepository orderItemRepo,
        PaymentRepository paymentRepo,
        PaymentAuditLogRepository auditLogRepo,
        InventoryHistoryRepository invHistoryRepo,
        RazorpayService razorpayService,
        ObjectMapper objectMapper
    ) {
        this.cartRepo = cartRepo;
        this.cartItemRepo = cartItemRepo;
        this.productRepo = productRepo;
        this.couponRepo = couponRepo;
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.paymentRepo = paymentRepo;
        this.auditLogRepo = auditLogRepo;
        this.invHistoryRepo = invHistoryRepo;
        this.razorpayService = razorpayService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CheckoutInitResponse initiateCheckout(Long userId, Map<String, Object> shippingAddress, String couponCode, Long missionId, String idempotencyKey) {
        String idemKey = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : UUID.randomUUID().toString();
        Optional<Payment> existingPayment = paymentRepo.findByIdempotencyKey(idemKey);
        if (existingPayment.isPresent()) {
            Order ord = orderRepo.findById(existingPayment.get().getOrderId()).orElseThrow();
            long paise = ord.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
            return new CheckoutInitResponse(ord, existingPayment.get(), existingPayment.get().isDemo(), razorpayService.getPaymentMode(), razorpayService.getKeyId(), paise, "Idempotent checkout retrieved");
        }

        // Validate Indian delivery PIN code when provided
        Object pinObj = shippingAddress != null ? shippingAddress.get("postalCode") : null;
        if (pinObj != null && !String.valueOf(pinObj).trim().matches("^[1-9][0-9]{5}$")) {
            throw new IllegalArgumentException("Please enter a valid 6-digit Indian PIN code.");
        }

        Cart cart = cartRepo.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("Your cart is empty."));
        List<CartItem> active = cartItemRepo.findByCartIdOrderByCreatedAtAsc(cart.getId()).stream()
            .filter(ci -> !ci.isSavedForLater()).toList();
        if (active.isEmpty()) throw new IllegalArgumentException("No active items in your cart.");

        List<CartItem> nativeItems = new ArrayList<>();
        Map<Long, Product> lockedProducts = new LinkedHashMap<>();

        for (CartItem ci : active) {
            Product p = productRepo.findByIdForUpdate(ci.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + ci.getProductId()));
            boolean isNative = "MISSIONCART_DIRECT".equalsIgnoreCase(p.getProductType()) || "SELLER_MANAGED".equalsIgnoreCase(p.getProductType());
            if (!isNative) continue;
            if (!"IN_STOCK".equalsIgnoreCase(p.getAvailabilityStatus()) || p.getStockQuantity() < ci.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for " + p.getName() + " (available: " + p.getStockQuantity() + ")");
            }
            if (p.getPrice() == null || p.getPrice().compareTo(BigDecimal.ZERO) <= 0 || "UNAVAILABLE".equalsIgnoreCase(p.getPriceStatus()) || "EXPIRED".equalsIgnoreCase(p.getPriceStatus())) {
                throw new IllegalStateException("Verified price is currently unavailable for " + p.getName());
            }
            nativeItems.add(ci);
            lockedProducts.put(p.getId(), p);
        }

        if (nativeItems.isEmpty()) {
            throw new IllegalArgumentException("Your cart contains only external retailer items. Use the grouped external retailer checkout links.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        for (CartItem ci : nativeItems) {
            Product p = lockedProducts.get(ci.getProductId());
            BigDecimal line = p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(line);
            BigDecimal rate = p.getTaxRate() != null ? p.getTaxRate() : BigDecimal.valueOf(18);
            taxAmount = taxAmount.add(line.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }

        BigDecimal shippingFee = subtotal.compareTo(BigDecimal.valueOf(999)) >= 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : BigDecimal.valueOf(79.00);
        BigDecimal discount = computeDiscount(subtotal, couponCode);
        BigDecimal total = subtotal.add(shippingFee).add(taxAmount).subtract(discount).max(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);

        String mode = razorpayService.getPaymentMode();
        boolean demo = "LOCAL_DEMO".equals(mode);

        Order order = new Order();
        order.setOrderNumber("MC-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        order.setUserId(userId);
        order.setMissionId(missionId);
        order.setCheckoutMode(demo ? "NATIVE_LOCAL_DEMO" : ("LIVE_MODE".equals(mode) ? "RAZORPAY_LIVE" : "RAZORPAY_TEST"));
        order.setStatus("PENDING_PAYMENT");
        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee);
        order.setTaxAmount(taxAmount);
        order.setDiscountAmount(discount);
        order.setTotalAmount(total);
        order.setCurrency("INR");
        order.setCouponCode(couponCode);
        order.setShippingAddressJson(writeJson(shippingAddress));
        order.setDeliveryEstimateNote("Admin-managed fulfillment (live courier tracking API not connected)");
        order = orderRepo.save(order);

        for (CartItem ci : nativeItems) {
            Product p = lockedProducts.get(ci.getProductId());
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setSku(p.getSku());
            oi.setSourcePlatform(p.getSourcePlatform());
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(p.getPrice()); // Immutable purchase-price snapshot
            oi.setLineTotal(p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())).setScale(2, RoundingMode.HALF_UP));
            orderItemRepo.save(oi);
        }

        String providerOrderId = razorpayService.createProviderOrder(order.getOrderNumber(), total);
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setProvider(demo ? "LOCAL_DEMO" : "RAZORPAY");
        payment.setProviderOrderId(providerOrderId);
        payment.setIdempotencyKey(idemKey);
        payment.setAmount(total);
        payment.setCurrency("INR");
        payment.setStatus("CREATED");
        payment.setDemo(demo);
        payment = paymentRepo.save(payment);

        logPaymentAudit(order.getId(), payment.getId(), "ORDER_CREATED", payment.getProvider(), "CREATED", "Verified total INR " + total);

        long amountPaise = total.multiply(BigDecimal.valueOf(100)).longValueExact();
        String notice = demo
            ? "Local Demo Checkout active (Razorpay credentials not configured — no real payment is processed)."
            : ("LIVE_MODE".equals(mode) ? "Razorpay Live Checkout ready." : "Razorpay Test Mode initialized (no real money charged).");

        return new CheckoutInitResponse(order, payment, demo, mode, razorpayService.getKeyId(), amountPaise, notice);
    }

    @Transactional
    public OrderDetailDto verifyPayment(
        Long userId,
        Long orderId,
        String providerOrderId,
        String providerPaymentId,
        String signature,
        BigDecimal paidAmountInr,
        boolean simulateFailure
    ) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (!order.getUserId().equals(userId)) throw new IllegalArgumentException("Not authorized");

        Payment payment = paymentRepo.findByOrderId(orderId).orElseThrow(() -> new EntityNotFoundException("Payment record not found"));

        // Idempotent guard: never deduct stock or mark paid twice
        if ("CAPTURED".equals(payment.getStatus()) || "DEMO_CAPTURED".equals(payment.getStatus())) {
            logPaymentAudit(orderId, payment.getId(), "DUPLICATE_VERIFY_IGNORED", payment.getProvider(), payment.getStatus(), "Already captured");
            return getOrderDetail(orderId, userId);
        }

        if (simulateFailure) {
            payment.setStatus("FAILED");
            order.setStatus("PAYMENT_FAILED");
            paymentRepo.save(payment);
            orderRepo.save(order);
            logPaymentAudit(orderId, payment.getId(), "PAYMENT_FAILED", payment.getProvider(), "FAILED", "Checkout payment failed or was cancelled");
            return getOrderDetail(orderId, userId);
        }

        if (paidAmountInr != null && paidAmountInr.compareTo(order.getTotalAmount()) != 0) {
            payment.setStatus("FAILED");
            order.setStatus("PAYMENT_FAILED");
            paymentRepo.save(payment);
            orderRepo.save(order);
            logPaymentAudit(orderId, payment.getId(), "AMOUNT_MISMATCH", payment.getProvider(), "FAILED", "Expected " + order.getTotalAmount() + " got " + paidAmountInr);
            throw new IllegalArgumentException("Payment amount mismatch detected.");
        }

        if (payment.isDemo()) {
            payment.setProviderPaymentId("demo_pay_" + UUID.randomUUID().toString().substring(0, 8));
            payment.setStatus("DEMO_CAPTURED");
            order.setStatus("CONFIRMED_DEMO");
        } else {
            if (!razorpayService.verifyPaymentSignature(providerOrderId, providerPaymentId, signature)) {
                payment.setStatus("FAILED");
                order.setStatus("PAYMENT_FAILED");
                paymentRepo.save(payment);
                orderRepo.save(order);
                logPaymentAudit(orderId, payment.getId(), "SIGNATURE_INVALID", "RAZORPAY", "FAILED", "Signature verification failed");
                throw new IllegalArgumentException("Razorpay payment signature verification failed.");
            }
            payment.setProviderPaymentId(providerPaymentId);
            payment.setStatus("CAPTURED");
            order.setStatus("CONFIRMED");
        }

        order.setTrackingNumber("MC-TRK-" + order.getId() + "001");
        order.setTrackingStatus("PACKING");
        paymentRepo.save(payment);
        orderRepo.save(order);
        logPaymentAudit(orderId, payment.getId(), "PAYMENT_VERIFIED", payment.getProvider(), payment.getStatus(), "Payment ref: " + payment.getProviderPaymentId());

        deductInventoryAndClearCart(userId, orderId);
        return getOrderDetail(orderId, userId);
    }

    @Transactional
    public void handleRazorpayWebhook(String rawBody, String signatureHeader) {
        if (!razorpayService.verifyWebhookSignature(rawBody, signatureHeader)) {
            logPaymentAudit(null, null, "WEBHOOK_SIGNATURE_INVALID", "RAZORPAY", "FAILED", "Rejected invalid webhook signature");
            throw new IllegalArgumentException("Invalid Razorpay webhook signature");
        }
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText();
            JsonNode payEntity = root.path("payload").path("payment").path("entity");
            String rzpOrderId = payEntity.path("order_id").asText();
            String rzpPaymentId = payEntity.path("id").asText();
            long webhookPaise = payEntity.path("amount").asLong(-1);

            if (rzpOrderId != null && !rzpOrderId.isBlank()) {
                paymentRepo.findByProviderOrderId(rzpOrderId).ifPresent(p -> {
                    Order ord = orderRepo.findById(p.getOrderId()).orElse(null);
                    if (ord == null) return;
                    long expectedPaise = ord.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValueExact();

                    if ("payment.authorized".equals(event) && "CREATED".equals(p.getStatus())) {
                        p.setStatus("AUTHORIZED");
                        p.setProviderPaymentId(rzpPaymentId);
                        paymentRepo.save(p);
                        logPaymentAudit(ord.getId(), p.getId(), "WEBHOOK_AUTHORIZED", "RAZORPAY", "AUTHORIZED", rzpPaymentId);
                    } else if ("payment.captured".equals(event)) {
                        if ("CAPTURED".equals(p.getStatus())) {
                            logPaymentAudit(ord.getId(), p.getId(), "WEBHOOK_DUPLICATE_IGNORED", "RAZORPAY", "CAPTURED", rzpPaymentId);
                            return;
                        }
                        if (webhookPaise > 0 && webhookPaise != expectedPaise) {
                            p.setStatus("FAILED");
                            paymentRepo.save(p);
                            logPaymentAudit(ord.getId(), p.getId(), "WEBHOOK_AMOUNT_MISMATCH", "RAZORPAY", "FAILED", "Expected paise " + expectedPaise + " got " + webhookPaise);
                            return;
                        }
                        p.setStatus("CAPTURED");
                        p.setProviderPaymentId(rzpPaymentId);
                        p.setRawResponseJson(rawBody);
                        paymentRepo.save(p);
                        ord.setStatus("CONFIRMED");
                        ord.setTrackingNumber("MC-TRK-" + ord.getId() + "001");
                        ord.setTrackingStatus("PACKING");
                        orderRepo.save(ord);
                        deductInventoryAndClearCart(ord.getUserId(), ord.getId());
                        logPaymentAudit(ord.getId(), p.getId(), "WEBHOOK_CAPTURED", "RAZORPAY", "CAPTURED", rzpPaymentId);
                    } else if ("payment.failed".equals(event) && !"CAPTURED".equals(p.getStatus())) {
                        p.setStatus("FAILED");
                        p.setRawResponseJson(rawBody);
                        paymentRepo.save(p);
                        ord.setStatus("PAYMENT_FAILED");
                        orderRepo.save(ord);
                        logPaymentAudit(ord.getId(), p.getId(), "WEBHOOK_FAILED", "RAZORPAY", "FAILED", rzpPaymentId);
                    } else if ("refund.processed".equals(event)) {
                        p.setStatus("REFUNDED");
                        p.setRefundStatus("REFUNDED");
                        p.setRefundAmount(ord.getTotalAmount());
                        paymentRepo.save(p);
                        ord.setStatus("REFUNDED");
                        ord.setRefundStatus("REFUNDED");
                        ord.setRefundAmount(ord.getTotalAmount());
                        orderRepo.save(ord);
                        logPaymentAudit(ord.getId(), p.getId(), "WEBHOOK_REFUNDED", "RAZORPAY", "REFUNDED", rzpPaymentId);
                    }
                });
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed webhook payload");
        }
    }

    @Transactional
    public OrderDetailDto cancelOrder(Long userId, Long orderId, String reason) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (!order.getUserId().equals(userId)) throw new IllegalArgumentException("Not authorized");
        if ("SHIPPED".equalsIgnoreCase(order.getTrackingStatus()) || "DELIVERED".equalsIgnoreCase(order.getTrackingStatus())) {
            throw new IllegalStateException("Order already shipped and cannot be cancelled directly.");
        }
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            return getOrderDetail(orderId, userId);
        }

        boolean wasConfirmed = "CONFIRMED".equalsIgnoreCase(order.getStatus()) || "CONFIRMED_DEMO".equalsIgnoreCase(order.getStatus());
        order.setStatus("CANCELLED");
        order.setTrackingStatus("CANCELLED");
        order.setCancellationReason(reason != null ? reason : "Cancelled by customer");
        orderRepo.save(order);

        if (wasConfirmed) {
            for (OrderItem oi : orderItemRepo.findByOrderId(orderId)) {
                productRepo.findByIdForUpdate(oi.getProductId()).ifPresent(p -> {
                    p.setStockQuantity(p.getStockQuantity() + oi.getQuantity());
                    p.setAvailabilityStatus("IN_STOCK");
                    productRepo.save(p);
                });
            }
        }
        logPaymentAudit(orderId, null, "ORDER_CANCELLED", order.getCheckoutMode(), "CANCELLED", order.getCancellationReason());
        return getOrderDetail(orderId, userId);
    }

    @Transactional
    public OrderDetailDto requestRefund(Long userId, Long orderId, BigDecimal amount, String reason) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (!order.getUserId().equals(userId)) throw new IllegalArgumentException("Not authorized");
        Payment payment = paymentRepo.findByOrderId(orderId).orElseThrow(() -> new EntityNotFoundException("Payment not found"));
        if (!"CAPTURED".equals(payment.getStatus()) && !"DEMO_CAPTURED".equals(payment.getStatus())) {
            throw new IllegalStateException("Only captured payments can be refunded.");
        }

        BigDecimal refundAmt = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(order.getTotalAmount()) <= 0)
            ? amount
            : order.getTotalAmount();
        boolean partial = refundAmt.compareTo(order.getTotalAmount()) < 0;

        if (!payment.isDemo() && razorpayService.isConfigured() && payment.getProviderPaymentId() != null) {
            Map<String, String> res = razorpayService.initiateGatewayRefund(payment.getProviderPaymentId(), refundAmt);
            payment.setRefundId(res.get("refundId"));
            payment.setRefundStatus(res.get("status"));
            payment.setStatus(partial ? "PARTIALLY_REFUNDED" : "REFUNDED");
            order.setRefundStatus(res.get("status"));
            order.setStatus(partial ? "PARTIALLY_REFUNDED" : "REFUNDED");
        } else {
            // Honest transparent refund request queue when gateway isn't connected
            payment.setRefundStatus("REQUESTED_DEMO_QUEUE");
            order.setRefundStatus("REQUESTED_DEMO_QUEUE");
        }
        payment.setRefundAmount(refundAmt);
        order.setRefundAmount(refundAmt);
        paymentRepo.save(payment);
        orderRepo.save(order);
        logPaymentAudit(orderId, payment.getId(), "REFUND_REQUESTED", payment.getProvider(), payment.getRefundStatus(), "Amount INR " + refundAmt + (reason != null ? " — " + reason : ""));
        return getOrderDetail(orderId, userId);
    }

    public InvoiceDto generateInvoice(Long userId, Long orderId) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (!o.getUserId().equals(userId)) throw new IllegalArgumentException("Not authorized");
        Payment p = paymentRepo.findByOrderId(orderId).orElse(null);
        return new InvoiceDto(
            "INV-" + o.getOrderNumber(),
            o.getOrderNumber(),
            o.getCreatedAt(),
            o.getStatus(),
            p != null ? p.getStatus() : "UNKNOWN",
            p != null ? p.getProvider() : "UNKNOWN",
            p != null ? p.getProviderPaymentId() : null,
            orderItemRepo.findByOrderId(o.getId()),
            o.getSubtotal(),
            o.getShippingFee(),
            o.getTaxAmount(),
            o.getDiscountAmount(),
            o.getTotalAmount(),
            o.getCurrency(),
            o.getShippingAddressJson()
        );
    }

    public List<OrderDetailDto> listOrders(Long userId) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(o -> new OrderDetailDto(o, orderItemRepo.findByOrderId(o.getId()), paymentRepo.findByOrderId(o.getId()).orElse(null), auditLogRepo.findByOrderIdOrderByCreatedAtDesc(o.getId())))
            .toList();
    }

    public OrderDetailDto getOrderDetail(Long orderId, Long userId) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (!o.getUserId().equals(userId)) throw new IllegalArgumentException("Not authorized");
        return new OrderDetailDto(o, orderItemRepo.findByOrderId(o.getId()), paymentRepo.findByOrderId(o.getId()).orElse(null), auditLogRepo.findByOrderIdOrderByCreatedAtDesc(o.getId()));
    }

    private void deductInventoryAndClearCart(Long userId, Long orderId) {
        Cart cart = cartRepo.findByUserId(userId).orElse(null);
        List<OrderItem> oItems = orderItemRepo.findByOrderId(orderId);
        for (OrderItem oi : oItems) {
            productRepo.findByIdForUpdate(oi.getProductId()).ifPresent(p -> {
                if (p.getStockQuantity() < oi.getQuantity()) {
                    throw new IllegalStateException("Out of stock during payment capture for " + p.getName());
                }
                p.setStockQuantity(p.getStockQuantity() - oi.getQuantity());
                if (p.getStockQuantity() == 0) p.setAvailabilityStatus("OUT_OF_STOCK");
                productRepo.save(p);

                InventoryHistory ih = new InventoryHistory();
                ih.setProductId(p.getId());
                ih.setDelta(-oi.getQuantity());
                ih.setReason("Order #" + orderId + " payment verified");
                invHistoryRepo.save(ih);
            });
            if (cart != null) {
                cartItemRepo.findByCartIdAndProductId(cart.getId(), oi.getProductId()).ifPresent(cartItemRepo::delete);
            }
        }
    }

    private void logPaymentAudit(Long orderId, Long paymentId, String eventType, String provider, String status, String details) {
        PaymentAuditLog log = new PaymentAuditLog();
        log.setOrderId(orderId);
        log.setPaymentId(paymentId);
        log.setEventType(eventType);
        log.setProvider(provider);
        log.setStatus(status);
        log.setDetails(details);
        auditLogRepo.save(log);
    }

    private BigDecimal computeDiscount(BigDecimal subtotal, String couponCode) {
        if (couponCode == null || couponCode.isBlank()) return BigDecimal.ZERO;
        Coupon c = couponRepo.findByCodeIgnoreCaseAndActiveTrue(couponCode.trim()).orElse(null);
        if (c == null) return BigDecimal.ZERO;
        if (c.getExpiresAt() != null && c.getExpiresAt().isBefore(Instant.now())) return BigDecimal.ZERO;
        if (c.getMinOrderAmount() != null && subtotal.compareTo(c.getMinOrderAmount()) < 0) return BigDecimal.ZERO;

        BigDecimal disc = "PERCENT".equalsIgnoreCase(c.getDiscountType())
            ? subtotal.multiply(c.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            : c.getDiscountValue();
        if (c.getMaxDiscountAmount() != null) disc = disc.min(c.getMaxDiscountAmount());
        return disc.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    private String writeJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj != null ? obj : Map.of()); } catch (Exception e) { return "{}"; }
    }
}

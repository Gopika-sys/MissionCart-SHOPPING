-- V4: Accurate Price Statuses, Taxes, Shipping, Refunds, and Payment Audit Logs

ALTER TABLE products MODIFY COLUMN price DECIMAL(12,2) NULL;
ALTER TABLE mission_items MODIFY COLUMN unit_price DECIMAL(12,2) NULL;
ALTER TABLE products ADD COLUMN price_status VARCHAR(25) NOT NULL DEFAULT 'UNAVAILABLE';
ALTER TABLE products ADD COLUMN tax_rate DECIMAL(5,2) NOT NULL DEFAULT 18.00;
ALTER TABLE products ADD COLUMN shipping_estimate DECIMAL(10,2) NOT NULL DEFAULT 0.00;
ALTER TABLE products ADD COLUMN availability_verified_at TIMESTAMP NULL;
ALTER TABLE products ADD COLUMN price_expires_at TIMESTAMP NULL;

UPDATE products
SET price_status = 'VERIFIED',
    price_verified = TRUE,
    price_last_verified_at = CURRENT_TIMESTAMP,
    availability_verified_at = CURRENT_TIMESTAMP
WHERE source_platform = 'MISSIONCART_DIRECT' AND price IS NOT NULL AND price > 0;

UPDATE products
SET price_status = 'ESTIMATED',
    price_verified = FALSE,
    price_last_verified_at = CURRENT_TIMESTAMP
WHERE source_platform NOT IN ('MISSIONCART_DIRECT', 'OPEN_BEAUTY_FACTS', 'OPEN_PRODUCTS_FACTS')
  AND price IS NOT NULL AND price > 0;

UPDATE products
SET price = NULL,
    price_status = 'UNAVAILABLE',
    price_verified = FALSE
WHERE price IS NOT NULL AND price <= 0;

ALTER TABLE orders ADD COLUMN tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE orders ADD COLUMN refund_status VARCHAR(30) DEFAULT 'NONE';
ALTER TABLE orders ADD COLUMN refund_amount DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE orders ADD COLUMN cancellation_reason VARCHAR(300);
ALTER TABLE orders ADD COLUMN delivery_estimate_note VARCHAR(255);

ALTER TABLE payments ADD COLUMN refund_id VARCHAR(150);
ALTER TABLE payments ADD COLUMN refund_status VARCHAR(30) DEFAULT 'NONE';
ALTER TABLE payments ADD COLUMN refund_amount DECIMAL(12,2) DEFAULT 0.00;

CREATE TABLE payment_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    payment_id BIGINT,
    event_type VARCHAR(80) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

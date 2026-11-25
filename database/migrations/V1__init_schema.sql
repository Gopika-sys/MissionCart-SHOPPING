CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    role VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    avatar_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    label VARCHAR(60) NOT NULL,
    recipient_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(80) NOT NULL DEFAULT 'India',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    parent_id BIGINT,
    description TEXT,
    purpose_group VARCHAR(100),
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE TABLE brands (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    slug VARCHAR(120) NOT NULL UNIQUE,
    country VARCHAR(80),
    website_url VARCHAR(300),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE retailer_sources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    official_website VARCHAR(300) NOT NULL,
    logo_url VARCHAR(500),
    category_focus VARCHAR(255) NOT NULL,
    supported_categories TEXT NOT NULL,
    import_method VARCHAR(80) NOT NULL,
    product_url_pattern VARCHAR(300),
    image_sourcing_method VARCHAR(200) NOT NULL,
    connection_status VARCHAR(60) NOT NULL DEFAULT 'CONFIGURED_FEED_PENDING',
    is_native_checkout BOOLEAN NOT NULL DEFAULT FALSE,
    last_successful_import TIMESTAMP NULL,
    last_price_verification TIMESTAMP NULL,
    feed_config_json TEXT,
    normalization_rules_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    brand_id BIGINT,
    category_id BIGINT NOT NULL,
    subcategory_id BIGINT,
    source_platform VARCHAR(60) NOT NULL,
    source_product_id VARCHAR(150),
    source_url VARCHAR(600),
    seller_id BIGINT,
    product_type VARCHAR(60) NOT NULL DEFAULT 'EXTERNAL_CATALOG',
    description TEXT NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    original_price DECIMAL(12,2),
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    price_verified BOOLEAN NOT NULL DEFAULT FALSE,
    price_last_verified_at TIMESTAMP NULL,
    availability_status VARCHAR(40) NOT NULL DEFAULT 'IN_STOCK',
    stock_quantity INT NOT NULL DEFAULT 0,
    rating DECIMAL(3,2),
    review_count INT DEFAULT 0,
    rating_verified BOOLEAN NOT NULL DEFAULT FALSE,
    data_verification_status VARCHAR(60) NOT NULL DEFAULT 'SAMPLE_IMPORT_PENDING',
    specifications_json TEXT,
    attributes_json TEXT,
    mission_tags TEXT,
    priority_tier VARCHAR(30) NOT NULL DEFAULT 'BALANCED',
    compatibility_rules_json TEXT,
    lifecycle_options_json TEXT,
    image_url VARCHAR(700),
    image_source VARCHAR(150),
    image_attribution VARCHAR(300),
    image_license VARCHAR(150),
    image_status VARCHAR(40) NOT NULL DEFAULT 'REQUIRES_IMAGE',
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    is_approved BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE SET NULL,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_products_subcategory FOREIGN KEY (subcategory_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_source ON products(source_platform, source_product_id);
CREATE INDEX idx_products_price ON products(price);
CREATE INDEX idx_products_published ON products(is_published, is_approved);

CREATE TABLE product_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(700) NOT NULL,
    image_source VARCHAR(150) NOT NULL,
    image_attribution VARCHAR(300),
    image_license VARCHAR(150),
    image_status VARCHAR(40) NOT NULL DEFAULT 'PENDING_REVIEW',
    image_verified_at TIMESTAMP NULL,
    image_width INT,
    image_height INT,
    image_alt_text VARCHAR(255) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE product_attributes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    attr_key VARCHAR(100) NOT NULL,
    attr_value VARCHAR(255) NOT NULL,
    attr_unit VARCHAR(40),
    CONSTRAINT fk_product_attrs_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    quantity_available INT NOT NULL DEFAULT 0,
    low_stock_threshold INT NOT NULL DEFAULT 5,
    warehouse_location VARCHAR(150) DEFAULT 'Bengaluru Hub',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE inventory_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    delta INT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    actor_email VARCHAR(190),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_history_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    title VARCHAR(180),
    comment TEXT NOT NULL,
    is_verified_purchase BOOLEAN NOT NULL DEFAULT FALSE,
    is_approved BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_possessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    item_name VARCHAR(150) NOT NULL,
    category_slug VARCHAR(120),
    keywords VARCHAR(300),
    matched_product_id BIGINT,
    notes VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_possessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE compatibility_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code VARCHAR(80) NOT NULL UNIQUE,
    domain VARCHAR(40) NOT NULL,
    title VARCHAR(180) NOT NULL,
    description TEXT NOT NULL,
    condition_json TEXT NOT NULL,
    severity VARCHAR(30) NOT NULL DEFAULT 'WARNING',
    recommended_action VARCHAR(400) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE missions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL UNIQUE,
    raw_prompt TEXT NOT NULL,
    goal VARCHAR(300) NOT NULL,
    situation_summary TEXT,
    purpose VARCHAR(150),
    context_json TEXT,
    budget DECIMAL(12,2),
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    timeframe VARCHAR(120),
    people_count INT DEFAULT 1,
    ai_provider_used VARCHAR(60) NOT NULL,
    ai_explanation TEXT,
    requirements_json TEXT,
    constraints_json TEXT,
    assumptions_json TEXT,
    follow_up_question VARCHAR(350),
    status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    share_token VARCHAR(120) UNIQUE,
    is_shared BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_missions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE mission_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mission_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    requirement_label VARCHAR(150) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'ESSENTIAL',
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL,
    is_selected BOOLEAN NOT NULL DEFAULT TRUE,
    is_excluded_owned BOOLEAN NOT NULL DEFAULT FALSE,
    compatibility_status VARCHAR(30) NOT NULL DEFAULT 'COMPATIBLE',
    compatibility_reason VARCHAR(400),
    recommendation_reason VARCHAR(400),
    assigned_to_user_id BIGINT,
    claimed_by_name VARCHAR(150),
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mission_items_mission FOREIGN KEY (mission_id) REFERENCES missions(id) ON DELETE CASCADE,
    CONSTRAINT fk_mission_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE mission_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mission_id BIGINT NOT NULL,
    user_id BIGINT,
    member_name VARCHAR(150) NOT NULL,
    member_email VARCHAR(190),
    role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mission_members_mission FOREIGN KEY (mission_id) REFERENCES missions(id) ON DELETE CASCADE
);

CREATE TABLE mission_invitations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mission_id BIGINT NOT NULL,
    token VARCHAR(120) NOT NULL UNIQUE,
    created_by_user_id BIGINT,
    expires_at TIMESTAMP NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mission_inv_mission FOREIGN KEY (mission_id) REFERENCES missions(id) ON DELETE CASCADE
);

CREATE TABLE mission_contributions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mission_id BIGINT NOT NULL,
    member_id BIGINT,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(40) NOT NULL DEFAULT 'DEMO_PENDING',
    note VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mission_contrib_mission FOREIGN KEY (mission_id) REFERENCES missions(id) ON DELETE CASCADE
);

CREATE TABLE carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE,
    session_id VARCHAR(120) UNIQUE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    mission_id BIGINT,
    quantity INT NOT NULL DEFAULT 1,
    saved_for_later BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE wishlists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL DEFAULT 'Saved Items',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE wishlist_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wishlist_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlist_items_wishlist FOREIGN KEY (wishlist_id) REFERENCES wishlists(id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uq_wishlist_product UNIQUE (wishlist_id, product_id)
);

CREATE TABLE coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    discount_type VARCHAR(20) NOT NULL DEFAULT 'PERCENT',
    discount_value DECIMAL(12,2) NOT NULL,
    min_order_amount DECIMAL(12,2) DEFAULT 0,
    max_discount_amount DECIMAL(12,2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP NULL
);

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(80) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    mission_id BIGINT,
    checkout_mode VARCHAR(40) NOT NULL DEFAULT 'NATIVE_DEMO',
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_PAYMENT',
    subtotal DECIMAL(12,2) NOT NULL,
    shipping_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    coupon_code VARCHAR(60),
    shipping_address_json TEXT NOT NULL,
    tracking_number VARCHAR(100),
    tracking_status VARCHAR(60) DEFAULT 'ORDER_PLACED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    sku VARCHAR(120) NOT NULL,
    source_platform VARCHAR(60) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    line_total DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL,
    provider_order_id VARCHAR(150),
    provider_payment_id VARCHAR(150),
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(40) NOT NULL DEFAULT 'CREATED',
    is_demo BOOLEAN NOT NULL DEFAULT FALSE,
    raw_response_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE TABLE lifecycle_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    order_id BIGINT,
    request_type VARCHAR(30) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'SUBMITTED_DEMO_WORKFLOW',
    details_json TEXT NOT NULL,
    estimated_value DECIMAL(12,2),
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    evidence_urls_json TEXT,
    provider_adapter_status VARCHAR(60) NOT NULL DEFAULT 'TRANSPARENT_REQUEST_QUEUE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lifecycle_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_lifecycle_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE catalog_import_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_platform VARCHAR(60) NOT NULL,
    format VARCHAR(30) NOT NULL,
    file_name VARCHAR(255),
    status VARCHAR(40) NOT NULL DEFAULT 'COMPLETED',
    total_records INT NOT NULL DEFAULT 0,
    inserted_count INT NOT NULL DEFAULT 0,
    updated_count INT NOT NULL DEFAULT 0,
    skipped_duplicate_count INT NOT NULL DEFAULT 0,
    error_count INT NOT NULL DEFAULT 0,
    errors_json TEXT,
    initiated_by VARCHAR(190) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL
);

CREATE TABLE activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_email VARCHAR(190),
    actor_role VARCHAR(40),
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(80),
    action VARCHAR(80) NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE system_settings (
    setting_key VARCHAR(120) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description VARCHAR(300),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

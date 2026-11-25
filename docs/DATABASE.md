# MissionCart Database Schema

## Overview
- **Engine:** MySQL 8.4 (Docker) / H2 MySQL-compatibility mode (`local` dev profile)
- **Migrations:** Flyway (`backend/src/main/resources/db/migration/` mirrored in `database/migrations/` and `database/seed/`)
- **Currency:** INR stored as `DECIMAL(12,2)`

## Core Tables (30 Tables)
- **Identity & Addresses:** `users`, `addresses`
- **Catalog & Sources:** `categories`, `brands`, `retailer_sources`, `products`, `product_images`, `product_attributes`, `inventory`, `inventory_history`, `reviews`
- **Missions & AI:** `missions`, `mission_items`, `user_possessions`, `compatibility_rules`
- **Collaboration:** `mission_members`, `mission_invitations`, `mission_contributions`, `activity_logs`
- **Commerce:** `carts`, `cart_items`, `wishlists`, `wishlist_items`, `coupons`, `orders`, `order_items`, `payments`
- **Lifecycle & Import & Settings:** `lifecycle_requests`, `catalog_import_jobs`, `system_settings`

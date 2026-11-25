# MissionCart — Shopping by Purpose, Not Products

MissionCart is a full-stack AI-powered purpose-first e-commerce platform built with **React 19 + TypeScript + Vite + Framer Motion** and **Java 21 + Spring Boot 3.4 + Spring Security + Flyway + MySQL 8**.

## Key Implemented Capabilities
- **Universal AI Mission Planner:** Parses any natural-language situation into essential/optional requirements, INR budgets, and grounded catalog bundles (`GeminiAIProvider`, `OpenAIProvider`, and zero-key `DeterministicFallbackEngine`).
- **13 Retailer Source Architecture + MissionCart Direct:** Configuration-driven source directory and JSON/CSV catalog importer (`scripts/catalog-import/`) with SKU/source deduplication.
- **Real Verified Product Photography:** Curated Wikimedia Commons open-license assets with attribution/license metadata; unverified items are transparently flagged `REQUIRES_IMAGE`.
- **Smart Budget Planner & Already-Own Exclusion:** Live INR recalculation, budget optimization suggestions, and natural-language possession tracking.
- **Attribute-Driven Compatibility Engine:** Validates 230V/16A electrical wattage & plug compatibility, dimensions, fashion sizes, and skincare active conflicts.
- **Shared Group Missions:** Real-time STOMP WebSocket rooms with invite tokens, item claims, activity logs, and demo pledge tracking.
- **Split-Aware Checkout & Payments:** Server-verified INR native checkout (Razorpay Test Mode + explicit Local Demo Checkout) isolated from grouped external retailer checkout.
- **Product Lifecycle Manager:** Repair, Exchange, Rent, and Resell tracking workflows.
- **Admin Console & Seller Studio:** Role-protected management for products, images, categories, 13 sources, catalog imports, inventory, orders, compatibility rules, and analytics.

See [`docs/SETUP.md`](./docs/SETUP.md) for commands and environment configuration.

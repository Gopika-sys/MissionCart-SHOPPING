# MissionCart Implementation Plan

## Phase 1: Foundation & Infrastructure
- Project scaffolding (`frontend/`, `backend/`, `database/`, `scripts/catalog-import/`, `docs/`)
- `.env.example`, `.gitignore`, `docker-compose.yml` (MySQL 8 + Redis + health checks)
- Flyway migrations covering all 30+ entities with INR `DECIMAL(12,2)` precision

## Phase 2: Backend Modular Monolith (Spring Boot 3.4 / Java 21)
- Security: BCrypt, JWT access + refresh tokens in HTTP-only cookies, RBAC (`CUSTOMER`, `ADMIN`, `SELLER`), rate limiting, audit logging
- Core Catalog: Categories, Subcategories, Brands, 13 Retailer Sources, Products, ProductImages, Attributes, Inventory, Reviews
- AI Mission Engine: `AIProvider` (`GeminiProvider`, `OpenAIProvider`, `DeterministicFallbackEngine`), `MissionIntentParser`, `RequirementExtractor`, `ProductRetrievalService`, `MissionBundleBuilder`, `MissionExplanationService`
- Smart Engines: `BudgetPlannerService`, `PossessionService`, `CompatibilityEngine` (Electronics, Furniture, Fashion, Skincare rules), `ReplacementService`
- Collaboration & Lifecycle: `SharedMissionService` + STOMP WebSocket, `LifecycleService` (Repair, Exchange, Rent, Resell)
- Commerce: `CartService`, `WishlistService`, `CheckoutService`, `PaymentService` (Razorpay HMAC verification + Webhook + Local Demo mode), `OrderService`
- Ingestion & Admin/Seller: `CatalogImportService` (JSON/CSV/API + duplicate detection), Admin & Seller controllers, OpenAPI docs

## Phase 3: Catalog Data & Real Verified Imagery
- 65+ structured products across 13 categories in INR
- All 13 retailer source configurations (Amazon India, Flipkart, Meesho, JioMart, Tata CLiQ, Myntra, AJIO, Savana, Nykaa, Purplle, DailyObjects, Nestasia, Croma)
- Verified open-license real photographs with attribution & license metadata; unverified items strictly flagged `REQUIRES_IMAGE`
- CLI + Admin catalog importer in `/scripts/catalog-import/`

## Phase 4: Editorial Frontend (React + TypeScript + Framer Motion)
- Centralized CSS design tokens (`#FAF7F2` cream, `#EFE8DC` linen, `#C86D51` terracotta, `#6E7F6B` sage, `#E8C4C7` blush, `#1C1917` espresso)
- Typography: `Fraunces` & `Cormorant Garamond` serif display + `Plus Jakarta Sans` & `Inter` sans-serif UI
- Moving visuals: Hero layered drift/zoom/crossfade, Infinite product marquee (pause on hover/focus), Asymmetric collections, Hover image swap/reveal, `prefers-reduced-motion` support
- Complete public, customer, admin, and seller pages (51 routes) with role-based guards

## Phase 5: Testing & Verification
- Backend unit, security, AI fallback, compatibility, payment verification, and importer tests (JUnit 5 + MockMvc)
- Frontend unit & component tests (Vitest + Testing Library)
- Production build checks (`mvn test`, `npm run build`, `tsc --noEmit`) & live full-stack smoke test

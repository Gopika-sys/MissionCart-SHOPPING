# CLAUDE.md — MissionCart Engineering & Execution Guide

**Tagline:** Shopping by Purpose, Not Products.

## Architecture Overview
MissionCart is a purpose-first AI e-commerce platform built as a modular monolith:
- **Frontend:** React 19 + TypeScript + Vite + React Router + TanStack Query + Framer Motion + Lucide React + Zod + React Hook Form (`frontend/`)
- **Backend:** Java 21 + Spring Boot 3.4 + Spring Data JPA + Spring Security (JWT + HTTP-only cookies) + Flyway + STOMP WebSocket + OpenAPI (`backend/`)
- **Database:** MySQL 8 (`database/migrations/`, `backend/src/main/resources/db/migration/`) with `local` H2-MySQL compatibility profile for zero-dependency local startup.
- **Catalog Importer:** Node/TS + Backend ingestion pipeline (`scripts/catalog-import/`) supporting JSON, CSV, and source adapter normalization across 13 Indian retail platforms.

## Feature Checklist
- [x] Workspace & architecture plan initialized
- [x] Database schema & Flyway migrations (MySQL 8 compatible)
- [x] Seed dataset (65 multi-category products, 13 retailer sources + MissionCart Direct, compatibility rules, demo accounts)
- [x] Spring Boot backend core (Security, Auth, Users, Categories, Retailers, Products)
- [x] AI Mission Planner (Gemini, OpenAI, Deterministic Fallback, Intent Parser, Bundle Builder)
- [x] Smart Budget Planner, Already-Own Exclusion, Smart Replacements, Compatibility Engine
- [x] Shared Missions + STOMP WebSocket real-time updates
- [x] Product Lifecycle Manager (Repair, Exchange, Rent, Resell)
- [x] Cart, Native/Razorpay/Demo Checkout & External Retailer Grouped Checkout
- [x] Catalog Importer (JSON, CSV, API, validation, deduplication, history)
- [x] Admin Dashboard & Seller Portal
- [x] Editorial Frontend (Warm linen palette, Fraunces + Plus Jakarta Sans, Framer Motion animations, all routes)
- [x] Full automated verification (Backend JUnit 5 `mvn test`, Frontend Vitest `npm test`, `npm run build`, Importer CLI, Live full-stack smoke test)

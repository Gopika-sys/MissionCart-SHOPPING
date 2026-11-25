# MissionCart System Architecture

## 1. High-Level Architecture
MissionCart is designed as a clean **modular monolith** with a strict separation between the presentation layer (React SPA) and the domain API layer (Spring Boot 3 + MySQL 8).

```
[React + Vite + TypeScript SPA]
   |-- REST (Axios + HTTP-only JWT Cookies) --> [Spring Boot 3 API]
   |-- STOMP / WebSocket (/ws)               --> [STOMP Broker (Shared Missions)]
                                                    |
        +-------------------------------------------+-------------------------------------------+
        |                       |                   |                   |                       |
 [AI Mission Engine]   [Smart Commerce]    [Catalog & Sources]   [Collaboration]         [Lifecycle]
 - Gemini/OpenAI       - Budget Planner    - 13 Source Adapters  - Shared Rooms          - Repair
 - Rule Fallback       - Compatibility     - Importer (JSON/CSV) - Claims & Roles        - Exchange
 - Intent Parser       - Native/Razorpay   - Image Verification  - Contributions         - Rent/Resell
        |                       |                   |                   |                       |
        +-------------------------------------------+-------------------------------------------+
                                                    |
                                       [MySQL 8 / Flyway / Redis]
```

## 2. Key Architectural Decisions
- **Zero-Hallucination AI Grounding:** The `AIProvider` parses user intent into structured requirements, constraints, and categories, while `ProductRetrievalService` queries the MySQL catalog directly. The AI never invents product IDs, prices, or URLs.
- **Deterministic Fallback:** When `GEMINI_API_KEY` and `OPENAI_API_KEY` are absent, `DeterministicFallbackEngine` executes keyword/synonym extraction, INR budget parsing, quantity detection, possession exclusion, and weighted multi-tag catalog scoring.
- **Dual Checkout Isolation:** Products stocked by MissionCart or verified sellers enter Native Checkout (server-verified INR prices + Razorpay test mode or explicit Demo Checkout). External marketplace products are grouped per retailer with transparent handoff.
- **Image Integrity Pipeline:** Every `ProductImage` tracks `imageSource`, `imageLicense`, `imageAttribution`, and `imageStatus` (`VERIFIED`, `PENDING_REVIEW`, `REQUIRES_IMAGE`, `INVALID`). Only `VERIFIED` photos appear in featured editorial showcases.

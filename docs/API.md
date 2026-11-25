# MissionCart REST & WebSocket API

Interactive OpenAPI documentation is available at `/swagger-ui.html` and `/api-docs` when the backend is running.

## Endpoints Summary
- **Auth:** `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout`, `GET /api/auth/me`
- **Catalog:** `GET /api/products`, `GET /api/products/count`, `GET /api/products/search`, `GET /api/products/{id}`, `GET /api/products/{id}/alternatives`, `GET /api/categories`, `GET /api/brands`, `GET /api/sources`
- **Missions & Budget:** `POST /api/missions/plan`, `GET /api/missions`, `GET /api/missions/{id}`, `PATCH /api/missions/{id}`, `POST /api/missions/{id}/recalculate`, `POST /api/missions/{id}/optimize`, `DELETE /api/missions/{id}`
- **Possessions:** `GET /api/possessions`, `POST /api/possessions`, `DELETE /api/possessions/{id}`
- **Compatibility:** `POST /api/compatibility/check`, `GET /api/compatibility/rules`
- **Shared Missions (STOMP `/ws` -> `/topic/shared/{token}`):** `POST /api/missions/{id}/share`, `GET /api/shared/{token}`, `POST /api/shared/{token}/join`, `PATCH /api/shared/{token}/items/{itemId}`, `POST /api/shared/{token}/contributions`
- **Cart & Wishlist:** `GET /api/cart`, `POST /api/cart/items`, `PATCH /api/cart/items/{id}`, `DELETE /api/cart/items/{id}`, `GET /api/wishlist`, `POST /api/wishlist/{productId}`
- **Checkout & Orders:** `POST /api/checkout`, `POST /api/payments/create-order`, `POST /api/payments/verify`, `POST /api/webhooks/razorpay`, `GET /api/orders`, `GET /api/orders/{id}`
- **Lifecycle:** `GET /api/lifecycle`, `POST /api/lifecycle/repair`, `POST /api/lifecycle/exchange`, `POST /api/lifecycle/rent`, `POST /api/lifecycle/resell`
- **Admin (`ROLE_ADMIN`):** `/api/admin/overview`, `/api/admin/products`, `/api/admin/categories`, `/api/admin/sources`, `/api/admin/catalog/import`, `/api/admin/users`, `/api/admin/orders`, `/api/admin/lifecycle`, `/api/admin/compatibility/rules`, `/api/admin/analytics`, `/api/admin/reviews`, `/api/admin/settings`
- **Seller (`ROLE_SELLER`):** `/api/seller/products`

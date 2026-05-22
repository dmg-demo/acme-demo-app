# service-api 3.4.0

## New Features
- New `GET /api/v2/products` endpoint with cursor-based pagination and faceted filtering
- Adopted shared-libs 1.2.0 — consistent response formatting and price display across all endpoints

## Improvements
- Tightened JWT validation: tokens now rejected 30 s before stated expiry to prevent clock-skew abuse
- Deprecated legacy `POST /api/order`; replaced by `POST /api/v2/orders` with idempotency-key support

## Bug Fixes
- Fixed race condition in cart checkout that caused duplicate line-items under concurrent requests

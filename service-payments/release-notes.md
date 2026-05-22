# service-payments 1.8.2

## New Features
- Integrated Stripe webhook retry logic: failed payment events are now re-queued with exponential back-off
- Health endpoint `/payments/health` now reports upstream gateway latency and circuit-breaker state

## Improvements
- Adopted shared-libs 1.2.0; `PriceFormatter` replaces all inline currency formatting
- Idempotency key enforcement added to `POST /payments/charge` — prevents duplicate charges on network retry

## Bug Fixes
- Fixed edge case where a network timeout during Stripe confirmation caused a double-charge

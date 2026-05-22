# shared-libs 1.2.0

## New Features
- Added `Greeter.greet()` utility for personalised welcome messages across all services
- Introduced `PriceFormatter` helper with multi-currency (USD, EUR, GBP) support

## Improvements
- Upgraded to Java 17 baseline; internal dependency refresh across the board
- `SessionContext.getCurrent()` is now thread-safe under high concurrency

## Bug Fixes
- Fixed NPE in `DateUtils.parseIso()` when timezone offset was missing

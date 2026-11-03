# TradeCore delivery roadmap

Each stage groups engineering tickets. Use GitHub Issues and (optionally) a GitHub Project board with To do / In progress / In review / Done. Each change is delivered by a feature branch and pull request.

## Stage 1 — Foundation
- **TRADE-001** Bootstrap Spring Boot 4.1.2 and Java 21. Verify `/api/health` returns HTTP 200 and `{"status":"ok"}`; `mvn test` succeeds; PR CI runs.
- **TRADE-002** Define API error conventions. Stable error schema, status mappings, tests.
- **TRADE-003** Define Instrument with pure-Java validation; symbol/currency invariants, tests.

## Stage 2 — Finance domain and persistence
- **TRADE-004** Model Account, Instrument, Order, Trade and CashLedgerEntry. Document relationships and invariants.
- **TRADE-005** Add PostgreSQL and Flyway; local Docker DB and versioned migrations.
- **TRADE-006** Build create/query order REST API; validation, DTOs and integration tests.

## Stage 3 — DSA matching engine
- **TRADE-007** Brute-force matching baseline; exact/partial/no-match deterministic tests.
- **TRADE-008** Price-time order book using TreeMap + FIFO queues; correct best bid/ask and partial fills.
- **TRADE-009** Top-K instruments by volume with HashMap and min-heap; compare with sorting.

## Stage 4 — Portfolio analytics
- **TRADE-010** Positions via HashMap and trade events; buy/sell/zero tests.
- **TRADE-011** Rolling analytics using sliding windows and binary search on sorted timestamps.
- **TRADE-012** Prefix-sum range analytics, compare against brute force.

## Stage 5 — Concurrency and correctness
- **TRADE-013** Atomic order placement and cash reservation; idempotency; no overspending.
- **TRADE-014** Concurrency and transaction-isolation tests; document guarantees.

## Stage 6 — Production engineering
- **TRADE-015** Authentication and authorization; accounts isolated per user.
- **TRADE-016** Docker, metrics, load testing, deployment and runbook.

## Process
For each issue: business goal -> design -> implement -> test -> PR -> review -> merge. Avoid artificial use of algorithms.

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

## Planned later — Redis caching and read-path optimisation
- **[TRADE-021](https://github.com/popescu-alexandru-unilu/TradeCore/issues/26)** Add a hybrid Redis cache: lazy load/cache-aside on reads, then version-aware **post-commit write-through-style refresh** (or invalidation) after PostgreSQL writes. Start with instrument reference data; optionally cache non-authoritative order display views. Measure hit rate, p95 latency, and DB load; test Redis outages, stale writes, TTL and cross-user isolation.
- **Correctness boundary:** Redis is never authoritative for available funds, reservations, ledger changes, matching, or settlement. PostgreSQL remains the source of truth; cache updates are not part of the DB transaction. See [ADR-0004](decisions/0004-redis-hybrid-cache.md).
- **Schedule:** Implement after [TRADE-005](https://github.com/popescu-alexandru-unilu/TradeCore/issues/5) (PostgreSQL/Flyway), ideally after the read APIs exist. This is a later optimisation, not part of TRADE-004.

## Process
For each issue: business goal -> design -> implement -> test -> PR -> review -> merge. Avoid artificial use of algorithms.

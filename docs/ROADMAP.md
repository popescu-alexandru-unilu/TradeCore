# TradeCore delivery roadmap

Each stage groups engineering tickets. Use GitHub Issues and (optionally) a GitHub Project board with To do / In progress / In review / Done. Each change is delivered by a feature branch and pull request.

## Delivery decision (2026-11-22) — MVP first, extensions later

**Goal:** Finish a working, testable, local *simulation-only* trading backend **before** introducing Redis, Kafka, advanced analytics or AWS infrastructure. We are reducing scope, not abandoning planned features.

### Phase A: Minimum viable trading backend (current priority)

- **Domain — TRADE-004:** finish `Trade` and `CashLedgerEntry` and their relationships with the already merged `Account`, `Instrument` and `Order`.
- **Persistence — TRADE-005:** PostgreSQL and Flyway migrations to persist accounts, orders, executions and ledger events.
- **REST — TRADE-006:** create/query orders, validation and tests.
- **Matching — TRADE-007 and TRADE-008:** implement and test a basic price–time-priority order book that produces executions and updates partial/full fill status. `TRADE-009` ranking analytics is not an MVP dependency.
- **Correctness — TRADE-013 and TRADE-014:** atomic cash reservation, execution and ledger postings; duplicate/retry protection; concurrency and transaction tests. Ensure simulated SELL orders cannot create negative holdings.
- **Local operability — subset of TRADE-016:** an easy local startup (Docker Compose for required database, app instructions), `mvn clean test`, and a demonstrable end-to-end BUY/SELL flow.

**MVP completion criteria:** submit compatible buy/sell limit orders through the API; execute a partial or full match using deterministic price–time priority; persist trade history; update cash/holdings and append-only ledger consistently; reject invalid orders and prevent overspending or duplicate financial postings; show passing tests and a reproducible local demo. Keep this as a local simulation until appropriate authentication/authorization is implemented.

**Planning estimate:** roughly 50–80 hours of remaining coding, testing and learning for this reduced core, not a deadline or guarantee.

### Phase B: Extensions after the MVP works

- Advanced order-book/portfolio analytics — TRADE-009 through TRADE-012.
- Full authentication, authorization, production hardening, monitoring and load testing — TRADE-015 and remaining TRADE-016 work. **Do not expose the unprotected MVP to the public Internet.**
- Redis performance caching — [TRADE-021](https://github.com/popescu-alexandru-unilu/TradeCore/issues/26); cache-aside reads and post-commit refresh/invalidation only for non-authoritative read models.
- Kafka and transactional outbox — [TRADE-017](https://github.com/popescu-alexandru-unilu/TradeCore/issues/20) and [TRADE-018](https://github.com/popescu-alexandru-unilu/TradeCore/issues/21), after authoritative DB transactions are reliable.
- AWS deployment — [TRADE-019](https://github.com/popescu-alexandru-unilu/TradeCore/issues/22) and optional [TRADE-020](https://github.com/popescu-alexandru-unilu/TradeCore/issues/23); not part of the MVP.
- Kubernetes orchestration — [TRADE-022](https://github.com/popescu-alexandru-unilu/TradeCore/issues/31); learn with local `kind` after the MVP, then compare **Amazon EKS versus ECS Fargate** for AWS hosting. Keep PostgreSQL external (RDS on AWS), run Flyway migrations in a controlled step, and cost-gate EKS provisioning. Kubernetes is optional, not an MVP prerequisite.

**Sequence rule:** no new Redis, Kafka, Kubernetes or AWS infrastructure for the MVP. Existing designs/ADRs and tickets remain documented as deferred enhancements. Choose the smallest coherent implementation that demonstrates end-to-end correctness.

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

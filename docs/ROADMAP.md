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
- **TRADE-016** Docker, metrics, load testing, basic deployment and runbook.

## Stage 7 — Kafka and event-driven architecture
- **[TRADE-017](https://github.com/popescu-alexandru-unilu/TradeCore/issues/20)** Define versioned trade events and a PostgreSQL transactional outbox to keep events consistent with financial commits.
- **[TRADE-018](https://github.com/popescu-alexandru-unilu/TradeCore/issues/21)** Run Kafka locally via Docker Compose; Spring Kafka producer and idempotent analytics/notification consumer; retries and dead-letter handling.

## Stage 8 — AWS deployment
- **[TRADE-019](https://github.com/popescu-alexandru-unilu/TradeCore/issues/22)** Deploy Spring Boot to ECS Fargate, image to ECR and PostgreSQL to RDS with reproducible infrastructure, IAM, logs and cost controls.
- **[TRADE-020](https://github.com/popescu-alexandru-unilu/TradeCore/issues/23)** Optionally add Amazon MSK after a cost review; use VPC networking and IAM authentication for MSK Serverless.

## Event-driven design constraints
- Keep the matching engine and money ledger transactional and database-authoritative.
- Kafka powers secondary asynchronous concerns, such as trade notifications, portfolio read models and audit-event distribution.
- Use transactional outbox publishing and idempotent consumers: expect at-least-once delivery, retries and duplicates.
- Local Kafka should work without AWS. Managed Kafka on AWS is cost-gated and **off by default**.

See [Architecture](ARCHITECTURE.md) and [ADR-0002](decisions/0002-kafka-and-outbox.md).

## Process
For each issue: business goal -> design -> implement -> test -> PR -> review -> merge. Avoid artificial use of algorithms.

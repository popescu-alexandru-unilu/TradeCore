# Architecture (incremental)

```text
Client / curl
    |
HTTP request
    |
Spring MVC Controller -> Application Service -> Repository / PostgreSQL
                              |
                         Domain model
                              |
                      DSA engine (Stage 3)

Later, once financial transactions and correctness are established:

Trade execution ── transaction ──> PostgreSQL trade + cash ledger + outbox
                                                        |
                                                  Outbox publisher
                                                        |
                                                      Kafka
                                                        |
                               +------------------------+------------------+
                               |                                           |
                      Analytics read model                         Notification worker
```

## Bounded concepts

- **Account**: simulated customer account and cash ledger.
- **Instrument**: tradeable fictional symbol.
- **Order**: request to buy/sell a quantity at a limit price.
- **Trade**: execution after two compatible orders match.
- **Position**: quantity held, calculated from executions.
- **Order book**: buy and sell orders sorted by price then FIFO for equal prices.

## Financial correctness

- The simulated financial ledger and cash reservations stay within PostgreSQL transactions. Avoid using Kafka to atomically reserve/spend cash.
- Save the trade, ledger changes and outbox event in a **single database transaction**; an outbox relay publishes committed events later.
- Kafka consumers are idempotent, because at-least-once event delivery can produce duplicates.
- Keep event payloads versioned; include stable IDs and avoid credentials or sensitive personal data in Kafka events.

## Environments

**Local:** Spring Boot, PostgreSQL and later Kafka running with Docker Compose (Kafka KRaft). Testcontainers for integration tests.

**AWS target:** ECS Fargate runs the API/worker containers; ECR stores images; RDS PostgreSQL stores financial state; CloudWatch provides logs and metrics. Optionally, Amazon MSK Serverless supplies Kafka if the cost and AWS region are acceptable. Keep MSK disabled by default in infrastructure definitions and document teardown.

Avoid introducing microservices solely to justify Kafka: a single Spring Boot codebase can contain event producers and consumers initially. Split deployable services only when scaling or reliability requirements justify it.

See [ADR-0002](decisions/0002-kafka-and-outbox.md).

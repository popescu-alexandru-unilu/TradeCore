# ADR-0002: Use Kafka for asynchronous trade events, not as the financial ledger

- **Status:** Proposed (pending PR review)
- **Date:** 2026-11-16
- **Related tickets:** [TRADE-017](https://github.com/popescu-alexandru-unilu/TradeCore/issues/20), [TRADE-018](https://github.com/popescu-alexandru-unilu/TradeCore/issues/21), [TRADE-020](https://github.com/popescu-alexandru-unilu/TradeCore/issues/23)

## Context

TradeCore will simulate order placement, cash reservations, order matching and executions. Customers must never lose or duplicate money when a broker is unavailable, a request is retried or an application crashes. Meanwhile, analytics and notifications should process committed trade activity asynchronously.

The initial application is a modular Spring Boot monolith; Kafka will be introduced only after financial domain, PostgreSQL persistence and concurrency rules are tested.

## Decision

1. **PostgreSQL is authoritative** for trades, ledger entries, account balances and reservations.
2. A matching/execution operation atomically writes its financial changes **and** a versioned `TradeExecuted` outbox row in one PostgreSQL transaction.
3. An outbox relay asynchronously publishes committed records to Kafka. For local development, use a Kafka container in KRaft mode via Docker Compose.
4. Spring Kafka consumers project trade activity into analytics and simulated notifications. They must be idempotent and tolerate duplicate messages.
5. Prefer a stable `eventId`, `tradeId`, `instrumentSymbol`, `quantity`, `price`, `currency`, `executedAt` and `schemaVersion` event contract. Avoid sensitive PII or credentials.
6. For AWS, deploy the API and relay on ECS Fargate, PostgreSQL on RDS, and optionally Amazon MSK when a cost review has approved it. MSK Serverless requires IAM access control and VPC connectivity.
7. Don't require Kafka to respond to synchronous order placement or to commit authoritative financial changes. An unavailable broker may delay analytics/notifications but must not corrupt balances.

## Alternatives considered

| Approach | Advantages | Drawbacks |
| --- | --- | --- |
| Postgres + transactional outbox + Kafka (**chosen**) | Durable business state; decoupled consumers; replay/recovery; realistic event-driven skill | More components, outbox relay and idempotency logic to maintain |
| Publish directly to Kafka inside the database transaction | Simple code | Not atomic with PostgreSQL: a crash can leave either state or event missing |
| Synchronous HTTP calls to analytics/notification services | Simple local setup | Tightly couples trade execution latency/availability to secondary services |
| PostgreSQL only with scheduled analytics polling | Cheapest and simplest initially | Less realistic stream-processing experience and slower near-real-time delivery |
| AWS SNS/SQS instead of Kafka | Often simpler and potentially cheaper on AWS | Different platform and semantics; doesn't teach Kafka partitions and consumer groups |

## Failure cases

- **Database transaction fails:** no trade, balance change or outbox event is committed.
- **Database commits; relay crashes:** event remains in outbox and is retried.
- **Relay publishes but crashes before marking complete:** a duplicate publication is possible; consumer deduplicates by event ID.
- **Consumer crashes after processing:** retries can re-deliver; projection updates must remain idempotent.
- **Kafka unavailable:** outbox backlog grows; monitor lag/backlog and recover later.
- **Concurrent orders:** rely on database atomic updates/locking, not a Kafka queue alone, to prevent overspending.

## Consequences and trade-offs

- Event-driven consumers are eventually consistent; portfolio *read models* might lag behind authoritative balances.
- Kafka alone does **not** give end-to-end exactly-once financial side effects across PostgreSQL and consumers.
- New topics and event schemas need versioning and evolution rules.
- Partition ordering only applies within a partition; choose stable message keys based on required ordering boundaries.
- Monitoring should include outbox backlog, failed publishes, consumer lag, processing retries and dead-letter counts.

## AWS cost and operational decision

Use local Kafka and integration tests as the default learning environment. Amazon MSK is optional in deployed environments because both provisioned and serverless cluster options can incur substantial always-on hourly charges, plus storage, partitions and data transfer. Do not provision MSK without budget alarms, a teardown plan and explicit approval.

AWS references:
- [Amazon MSK pricing](https://aws.amazon.com/msk/pricing/)
- [MSK Serverless documentation](https://docs.aws.amazon.com/msk/latest/developerguide/serverless.html)
- [ECS Fargate task networking](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/fargate-task-networking.html)

## Possible improvements / revisit when

- Consider Debezium-based CDC outbox relay if polling becomes a bottleneck, after measuring throughput.
- Evaluate Amazon MSK Serverless versus provisioned MSK, or a smaller Kafka-compatible managed service, once the deployment budget and traffic are known.
- Consider Kafka topic schema registry and compatibility checks as the event vocabulary grows.
- Consider extracting worker deployments from the monolith only when scaling, deployment independence or fault isolation justify it.

## Verification

Integration tests must demonstrate rollback consistency, at-least-once publishing, retries, duplicate consumer handling and event payload versioning. An AWS smoke test should verify private connectivity and end-to-end publication only when managed Kafka is enabled.

# TradeCore

**Java 21 · Spring Boot 4.1.2 · Maven · JUnit · GitHub Actions**

TradeCore is a **simulation-only** brokerage backend and matching-engine learning project. No live trades or real money.

## Mission

Build a professional engineering portfolio demonstrating finance-domain modelling, Java backend development, data structures and algorithms, transaction correctness, concurrency, testing, Kafka event-driven architecture, and an optional AWS deployment.

## Quick start

Requires JDK 21 and Maven 3.9+, or IntelliJ IDEA with Maven integration.

```bash
mvn test
mvn spring-boot:run
```

Verify:

```bash
curl http://localhost:8080/api/health
# {"status":"ok"}
```

In IntelliJ: File > Open > select the project directory > load the Maven project > run TradeCoreApplication.

**Kafka is planned for Stage 7, not yet implemented.** Develop against a local Kafka broker before considering Amazon MSK in AWS.

## Team workflow

Every change follows **GitHub Issue -> feature branch -> code and tests -> pull request -> CI -> review -> squash merge**.

See [Working agreement](CONTRIBUTING.md), [Roadmap](docs/ROADMAP.md), [Architecture](docs/ARCHITECTURE.md), and [ADRs](docs/decisions/README.md).

## Design principles

- Use BigDecimal and explicit currencies for money; never floating point.
- Track ledger history for financial changes rather than blindly changing balances.
- Assume HTTP retries and concurrent requests; design idempotency and consistency.
- Start with an in-memory DSA engine, then introduce database boundaries.
- Use PostgreSQL as the source of truth for financial state. Kafka delivers committed trade events to analytics/notification consumers using an outbox pattern.
- Keep AWS resources, especially MSK, budget-controlled and disabled by default.
- The project uses fictional market data and simulated balances only.

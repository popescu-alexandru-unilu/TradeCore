# ADR-0004: Hybrid Redis cache-aside reads and post-commit write-through refresh

- **Status:** Proposed (pending review and implementation)
- **Date:** 2026-11-21
- **Related ticket:** [TRADE-021: Add Redis hybrid caching](https://github.com/popescu-alexandru-unilu/TradeCore/issues/26)

## Context

TradeCore is a simulation-only brokerage backend. PostgreSQL will hold authoritative financial and business state. A Redis cache can reduce repeated database reads and improve latency for frequently accessed, non-authoritative data. However, financial correctness must not depend on a cache, and PostgreSQL and Redis cannot participate in a normal single ACID transaction.

We want to combine two familiar cache strategies:
- **Lazy loading (cache-aside):** populate Redis only when a read misses.
- **Write-through-style refresh:** after a successful database write, proactively refresh the cached value instead of waiting for the next miss.

This ADR is a design proposal for implementation **after TRADE-005 (PostgreSQL/Flyway)** and an initial read API. It does not add a running Redis service or caching code yet.

## Decision

Use Redis as a **non-authoritative read-through optimization** for selected, read-heavy data, starting with instrument metadata.

### Read path — lazy loading

```text
GET /instruments/{id}
          |
     Redis GET(key)
       /      \
      hit     miss / Redis unavailable
       |            |
   return cache   PostgreSQL SELECT
                      |
                if found, best-effort
                SET cache with TTL/version
                      |
                 return DB result
```

1. Define deterministic namespaced keys such as `tradecore:instrument:v1:{instrumentId}`. Cache entries include a monotonic database revision/version plus the serialized value.
2. Read from Redis first. On miss (or Redis timeout), load from PostgreSQL; return the database result regardless of whether Redis population succeeds.
3. On miss, populate Redis with a bounded TTL **only if the value's database version is not older than what is already cached**; enforce with an atomic version-aware compare-and-set (for example a Lua script), including during cache population. This protects against a late reader overwriting a newer write.
4. Start with a configurable instrument metadata TTL around **5 minutes** (with modest jitter); tune after observing hit rate, staleness and load. Do not cache missing records initially unless a deliberate, very short-lived negative-caching policy is added.

### Write path — post-commit write-through-style refresh

```text
Write request -> validate -> PostgreSQL transaction -> COMMIT
                                                   |
                                         afterCommit callback
                                                   |
                                       Redis version-checked SET
                                      /                       \
                                  success                    failure
                              updated cache          best-effort invalidation
                                                   + metrics + TTL recovery
```

1. Write/update the authoritative record in PostgreSQL inside a transaction and increment a monotonic `version` (or equivalent DB-managed revision).
2. **Only after commit**, attempt to replace the Redis representation with the committed data. Never modify Redis before the authoritative transaction commits.
3. Reject stale cache refreshes through version-aware conditional writes, so out-of-order refreshes cannot overwrite newer cached records.
4. On Redis failure, serve future reads from PostgreSQL when possible, attempt best-effort invalidation and record a metric; stale entries that cannot be invalidated must expire by TTL. A later refresh or optional durable retry can reconcile them.
5. If the database transaction rolls back, do not refresh the cache. A cache timeout must not reverse an already successful database commit.
6. For frequently updated and latency-sensitive data, **post-commit invalidate + lazy reload** is a safer alternative to write-through refresh. Select this when versioned refresh costs or staleness risks outweigh cache-hit benefits.

**Terminology:** This is **cache-aside on reads plus after-commit write-through-style refresh on writes**, *not* strict atomic write-through. There is an unavoidable window between database commit and Redis refresh. Under concurrency or Redis failure, cached reads may briefly be stale. We make no strong-consistency or cross-store exactly-once guarantee.

### Data classification

| Data | Redis policy | Correctness boundary |
| --- | --- | --- |
| Instrument reference metadata (e.g., symbol, currency, display info) | First candidate; cache-aside read + post-commit refresh | DB is authoritative |
| Public market summaries / analytics read models | Candidate later, TTL or event-based updates | Eventual consistency acceptable |
| Customer order display/status views | Optional, short TTL, keys scoped by account/tenant and API authorization | Matching and execution must read authoritative state |
| Available cash, reserved funds, ledger entries, order matching/settlement decisions | **Do not use Redis as authority**; avoid initial caching | PostgreSQL transactions and constraints decide |

### Implementation boundaries

- Java/Spring Boot: Spring Data Redis with Lettuce; explicit `CacheService` / repository abstraction, serializer versioning, bounded timeouts, configurable enabled flag.
- Local development: Redis container with health check; Testcontainers Redis + PostgreSQL for integration tests. AWS/managed Redis is optional and cost-gated.
- Isolation: cache keys that represent customer-specific data include the account/tenant identifier; always authorize before returning private data. Never cache credentials, secrets or sensitive payment data.
- Redis outages must degrade to normal PostgreSQL reads, not failed order execution. Beware overload after cache outages; use timeouts, metrics, and optionally request coalescing to mitigate a thundering herd.
- Do not confuse a Redis lock with a durable database transaction; do not use Redis for financial concurrency control.

## Why

- Lazy loading avoids populating unused keys and reduces repeated PostgreSQL reads on hot keys.
- Refreshing after committed writes reduces the stale window for read-heavy keys versus TTL-only expiry.
- Version-aware refresh handles delayed or out-of-order work better than unconditional SET.
- TTLs, invalidation and database fallback bound the effects of cache failures.
- Keeping the financial ledger in PostgreSQL preserves the correctness model already planned for TradeCore.

## Alternatives considered

| Option | Advantages | Drawbacks |
| --- | --- | --- |
| Cache-aside reads + versioned post-commit refresh (selected for instrument metadata) | Lazy population, faster repeat reads and proactive updates | Additional code; no atomic DB/cache write; requires versioning and failure handling |
| Cache-aside reads + invalidate after commit | Simpler; less risk of old data being pushed into cache | Next read is a miss; stale window/invalidation failure still possible |
| TTL-only cache-aside | Very simple | Stale until expiry after changes |
| Strict write-through via cache as write coordinator | Cache can control write path | Cache/network outage couples into writes; still lacks cross-store ACID |
| Redis as primary financial ledger | Low-latency reads | Unacceptable source-of-truth/transaction and audit trade-offs for this project |
| PostgreSQL only | Simple, strong consistency | More repeated reads; baseline we should benchmark first |

## Consequences and risks

- There is no single transaction across PostgreSQL and Redis. The API may read an older cached representation after a successful DB write.
- A failed after-commit refresh can leave an old cache entry until invalidation or TTL expiry; monitor this explicitly.
- Cache stampedes, memory limits, eviction policy, schema evolution, and tenant isolation need operational design.
- Instrument metadata may change rarely enough that Redis complexity offers little benefit; measure before enabling in production.
- Financial workflows and order-matching constraints always use authoritative database state and transactions, regardless of what Redis reports.

## Possible improvements / revisit when

- Implement TRADE-005 and TRADE-006 first; assess whether latency/DB load warrants caching.
- Benchmark cache hit rate, p50/p95 latency, database query volume and Redis memory footprint.
- Consider invalidate-on-write as the default for mutable order status; retain versioned write-through refresh for suitable instrument data.
- Consider reliable outbox-based cache reconciliation only if measured stale windows or refresh failures require it.
- Review TTL, expiration jitter, retry strategy and authorization after introducing account-specific read models.

## Verification

Integration tests must cover:
- Cache hit and miss, TTL expiry, and fallback on Redis outage.
- PostgreSQL write success -> after-commit cache refresh; database rollback -> no cache refresh.
- Concurrent/out-of-order cache updates never overwriting a newer version.
- Cache failure never committing/rolling back cash transactions incorrectly.
- Account-scoped key isolation and authorized reads of any customer data.

A load test should compare the baseline (no Redis) with cache enabled using representative read/write ratios; do not assume Redis is a net win without measurement.

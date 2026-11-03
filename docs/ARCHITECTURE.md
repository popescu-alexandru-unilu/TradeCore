# Architecture (initial)

```text
Client / curl
    |
HTTP request
    |
Spring MVC Controller -> Application Service -> Repository / PostgreSQL (Stage 2)
                              |
                         Domain model
                              |
                      DSA engine (Stage 3)
```

## Bounded concepts

- **Account**: simulated customer account and cash ledger.
- **Instrument**: tradeable fictional symbol.
- **Order**: request to buy/sell a quantity at a limit price.
- **Trade**: execution after two compatible orders match.
- **Position**: quantity held, calculated from executions.
- **Order book**: buy and sell orders sorted by price then FIFO for equal prices.

Start as a Spring Boot monolith. Write a deterministic in-memory matching engine first. Add persistence, concurrency guarantees and external infrastructure as the requirements demand.

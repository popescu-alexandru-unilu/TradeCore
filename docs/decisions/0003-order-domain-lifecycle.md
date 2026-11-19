# ADR-0003: Model limit orders with explicit fill progress and lifecycle

- **Status:** Proposed (pending implementation and review)
- **Date:** 2026-11-18
- **Related ticket:** [TRADE-004: Design account, order, trade, ledger domain model](https://github.com/popescu-alexandru-unilu/TradeCore/issues/4)
- **Implementation branch:** `feature/004-order-model`

## Context

TradeCore needs to represent a customer's instruction to buy or sell an instrument separately from the trades that execute that instruction. An order can be executed in several parts, or cancelled after partial execution. The model must reject impossible quantities and transitions while preserving completed executions.

This decision covers the initial *limit-order* domain model. It does not implement a matching engine, portfolio balances, network retries or database locking.

## Decision

Model an `Order` with the following fields:

| Field | Java type | Meaning |
| --- | --- | --- |
| `id` | `Long` | Unique order identity (ID assignment/persistence to be decided later) |
| `accountId` | `Long` | Customer account that owns the order |
| `instrumentId` | `Long` | Instrument to buy or sell |
| `side` | `OrderSide` | `BUY` or `SELL` |
| `status` | `OrderStatus` | Current lifecycle state |
| `quantity` | `BigDecimal` | Original requested quantity |
| `filledQuantity` | `BigDecimal` | Total quantity already executed |
| `limitPrice` | `BigDecimal` | Maximum buy price or minimum sell price per unit |

`OrderSide` is an enum with `BUY` and `SELL`. `OrderStatus` is an enum with `PENDING`, `PARTIALLY_FILLED`, `FILLED`, and `CANCELLED`. For this initial model, `PENDING` means the validated order is active but no execution has occurred. We do not add a separate `ACCEPTED` state yet.

Use `limitPrice` rather than `bid` because the same field applies to buy *and* sell orders. Store `filledQuantity` because partial fills are possible. Do not store a mutable `remainingQuantity` or calculated total notional as redundant state: derive quantities and notional as needed. A future `version` for optimistic locking is distinct from the order ID.

## Java implementation decisions (2026-11-19)

The initial implementation uses a `public final class Order` with a **private constructor** and a **public static factory**:

```java
public static Order createOrder(
        Long id, Long accountId, Long instrumentId,
        OrderSide side, BigDecimal quantity, BigDecimal limitPrice) {
    return new Order(id, accountId, instrumentId, side, quantity, limitPrice);
}
```

- **Validate -> Assign -> Initialise:** check all three `Long` IDs are non-null and positive; require non-null `side`; require positive, non-null `quantity` and `limitPrice`; then assign the supplied fields. Initialise `status = OrderStatus.PENDING` and `filledQuantity = BigDecimal.ZERO` internally.
- **Trust boundary:** the application/service layer supplies order IDs and account IDs from trusted state (eventually generated order ID and authenticated account), while the customer chooses instrument, side, quantity and limit price. Never accept `status` or `filledQuantity` from the customer or as normal new-order factory inputs.
- **Field mutability:** prefer `private final` for `id`, `accountId`, `instrumentId`, `side`, `quantity`, and `limitPrice`. Keep `status` and `filledQuantity` mutable but encapsulated, changing them only through validated domain operations. A `final` class or static factory **does not** make an order immutable.
- **Derived data:** implement `getRemainingQuantity()` as `quantity.subtract(filledQuantity)` rather than maintaining an independently mutable field. For cancelled orders this is the unfilled amount, not permission to execute it.
- **Construction safeguards:** use `this.field = parameter` where names are shadowed, and report invalid input before mutating object state. Reuse validation helpers for positive `BigDecimal` amounts and positive `Long` IDs.

### Enum implementations

`OrderStatus` is a **finite-state machine expressed using a Java 21 switch expression** in `canTransitionTo(OrderStatus nextStatus)`. Return `false` for `null`; permit `PENDING -> PARTIALLY_FILLED|FILLED|CANCELLED`, `PARTIALLY_FILLED -> PARTIALLY_FILLED|FILLED|CANCELLED`, and no outgoing transitions from `FILLED` or `CANCELLED`. The `PARTIALLY_FILLED` self-transition represents additional executions that do not yet complete the order. Prefer a switch to referring to other enum constants in per-constant initialisation.

`OrderSide` is a **rich enum**: `BUY(1)` and `SELL(-1)`, with a private enum constructor, an immutable `sign` property, `getSign()`, `opposite()`, `isBuy()`, `isSell()`, and `apply(BigDecimal quantity)`. Signed quantities can represent a **change in asset holdings**, not a cash-flow direction. Use `BigDecimal` rather than `double` to avoid binary floating-point precision issues.

These are design choices for the initial domain implementation; persistence, execution deduplication, and atomic settlement are intentionally deferred.

## Invariants and state changes

- `quantity > 0`, `limitPrice > 0`, and `0 <= filledQuantity <= quantity`.
- A newly created order starts `PENDING` with `filledQuantity = 0`.
- A positive execution quantity cannot exceed `quantity - filledQuantity`.
- A partial execution changes an active order to `PARTIALLY_FILLED`. Once `filledQuantity == quantity`, the order becomes `FILLED`.
- `PENDING` may transition to `PARTIALLY_FILLED`, `FILLED` or `CANCELLED`.
- `PARTIALLY_FILLED` may remain partially filled after more execution, or transition to `FILLED` or `CANCELLED`.
- `FILLED` and `CANCELLED` are terminal states: no further fills or cancellation state changes.
- Cancelling a partially filled order **does not undo completed trades**. It stops execution of the unfilled portion; previously filled quantity remains unchanged.
- `quantity - filledQuantity` is the *unfilled quantity*, which can remain positive on a cancelled order. *Executable/open quantity* becomes zero when cancelled.

Do not expose arbitrary public setters for `status` or `filledQuantity`. The domain should expose behaviour such as `applyFill(quantity)` and `cancel()` that validates changes and maintains the invariants.

Example: an order for 10 shares fills 4 -> `filledQuantity=4`, `PARTIALLY_FILLED`. A subsequent 7-share fill is rejected because only 6 remain. A subsequent 6-share fill is accepted -> `filledQuantity=10`, `FILLED`. If instead the partially filled order is cancelled, the 4 executed shares remain executed and the 6 unfilled shares cannot be executed.

## Why

- Separating `Order` from `Trade` preserves the distinction between **intent** and **execution**.
- Explicit partial-fill state makes lifecycle behaviour readable for API clients and future matching logic.
- `BigDecimal` avoids binary floating-point issues for prices; quantities also use `BigDecimal` to permit fractional units if the chosen instrument supports them.
- Centralised domain validation prevents invalid transitions and oversubscribed fills.
- The initial design is small enough to implement and test before persistence and concurrency are introduced.

## Alternatives considered

| Option | Benefits | Drawbacks |
| --- | --- | --- |
| Explicit `filledQuantity` + lifecycle enum (selected) | Supports partial fills and cancellation; clear invariants | Must keep state and quantity consistent |
| Only `PENDING` / `FILLED` / `CANCELLED` | Fewer states | Cannot express progress on partially executed orders |
| Use `bid` for price | Familiar buy-order terminology | Misleading for sell orders |
| Store `remainingQuantity` as another mutable field | Fast lookup | Can become inconsistent with `quantity` and `filledQuantity` |
| Use `long` for quantity | Simple for whole-share products | Cannot represent fractional quantity without changing the model |

## Consequences and risks

- The model initially supports **limit orders**, not market orders or a full exchange order-status vocabulary.
- Quantity precision, minimum trade size, price tick size and asset-specific restrictions remain to be defined; `BigDecimal` by itself does not enforce those rules.
- BUY orders eventually need cash reservation and SELL orders need position/holdings checks. These are application/persistence concerns and must not be assumed guaranteed by this standalone `Order` object.
- Concurrency, atomic matching, idempotent execution ingestion, and order persistence require later design and database safeguards, not merely in-memory checks.
- Cancelling releases funds reserved for the unfilled part in the application flow, but does not reverse existing executions.

## Possible improvements / revisit when

- Add explicit order types (`MARKET`, stop orders) only when there are concrete use cases.
- Add a `version` field and optimistic locking for concurrent modifications when PostgreSQL persistence is introduced.
- Add precision/tick constraints associated with `Instrument` and trading venue.
- Expand statuses (e.g. rejected, expired) when API and order-processing requirements demand them.

## Verification

JUnit tests should verify valid creation, invalid quantities and prices, partial fill, repeated partial fills, exact full fill, rejection of excessive fills, cancellation before and after partial fill, preservation of filled quantity after cancellation, terminal state rejection, and invariant preservation after invalid operations.

Follow-up work under TRADE-004 will define `Trade` and `CashLedgerEntry` and their relationships to `Order` and `Account`.

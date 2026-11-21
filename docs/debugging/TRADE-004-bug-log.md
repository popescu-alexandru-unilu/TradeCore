# TRADE-004 Debugging Log — Account and Order Domain

- **Status:** Living learning log; update as new errors are confirmed and resolved
- **Last updated:** 2026-11-21
- **Related issue:** [TRADE-004](https://github.com/popescu-alexandru-unilu/TradeCore/issues/4)
- **Current implementation branch:** `feature/004-order-model`
- **Design reference:** [ADR-0003](../decisions/0003-order-domain-lifecycle.md)

> Evidence convention: **Confirmed Maven failure** means an actual error was pasted from a Maven run. **Caught during review** means a bug was identified in a shared code snippet; it may not have reached the compiler. **Fix proposed** means the correction was explained, but another successful `mvn clean test` run has not yet been shown. Do not mark the entire Order test suite green without that output.

## 1. Confirmed Maven compilation failures — current blockers

### BUG-001 — `final` fields reassigned during order lifecycle

- **Observed:** `maven-compiler-plugin:3.15.0:compile` failed on `Order.java` lines 50, 51 and 58.
- **Compiler text:**
  ```text
  cannot assign a value to final variable filledQuantity
  cannot assign a value to final variable status
  cannot assign a value to final variable status
  ```
- **Root cause:** `filledQuantity` and `status` were declared `final` although `applyFill()` and `cancel()` must reassign them. A `final` class does **not** imply that all of its fields must be final.
- **Resolution instructed:** Keep stable order details (`id`, `accountId`, `instrumentId`, `side`, `quantity`, `limitPrice`) `private final`; leave lifecycle fields mutable but private:
  ```java
  private OrderStatus status;
  private BigDecimal filledQuantity;
  ```
- **Verification:** A subsequent Maven invocation progressed to `testCompile`, which strongly indicates the original main-source compilation blockers were resolved. A full successful test run is still pending.

### BUG-002 — Private `cancel()` inaccessible from JUnit tests

- **Observed:** `maven-compiler-plugin:3.15.0:testCompile` failed at `OrderTest.java` lines 10, 18, 51 and 52:
  ```text
  cancel() has private access in dev.tradecore.domain.order.Order
  ```
- **Root cause:** `cancel()` remained `private`; it is a domain action meant to be called by test and application-service code.
- **Resolution instructed:** Change to `public void cancel()` while retaining FSM validation and private state:
  ```java
  public void cancel() {
      if (!status.canTransitionTo(OrderStatus.CANCELLED)) {
          throw new IllegalStateException("Cannot cancel this order");
      }
      status = OrderStatus.CANCELLED;
  }
  ```
- **Verification:** **Fix proposed; no new passing Maven result provided yet.**

## 2. Order model bugs caught during code review

| ID | Symptom / example | Root cause | Correction |
| --- | --- | --- | --- |
| BUG-003 | `public final Orders {` | Missing `class` keyword, inconsistent class/file name | `public final class Order {` in `Order.java` |
| BUG-004 | Constructor accepted `status` and `filledQuantity` | Exposed internally controlled lifecycle state; left real order fields uninitialised | Constructor/factory accept six fields: three IDs, `side`, `quantity`, `limitPrice`; initialise `PENDING` and zero internally |
| BUG-005 | `validatePositiveAmount(quantity)` called when `quantity` was not a constructor parameter | Uninitialised field is `null`; validation always fails | Include the supplied value as parameter and validate it before assigning `this.quantity` |
| BUG-006 | Positive-amount validation applied to `filledQuantity` at creation | Newly created orders must have **zero** fills | Never require initial `filledQuantity > 0`; set `BigDecimal.ZERO` |
| BUG-007 | `this.side = OrderSide.getSign()` | Static-looking call to an instance method returning `int`, not `OrderSide` | Validate non-null and use `this.side = side` |
| BUG-008 | `new Order(status, filledQuantity)` after changing constructor to six parameters | Stale factory signature; wrong arity/types | Update `createOrder()` to forward all six required parameters |
| BUG-009 | Missing ID validation; same error message for any bad ID | Invalid identity can enter domain state; diagnostics ambiguous | Reject `null`/non-positive `Long` IDs; use field-specific messages |
| BUG-010 | `getRemainingQuantity(BigDecimal quantity)` | Caller can substitute a quantity that differs from the order's stored quantity | No argument; return `quantity.subtract(filledQuantity)` |
| BUG-011 | `status.canTransitionTo(CANCELLED, FILLED)` | API takes **one** next state and returns a boolean; no state change occurs by calling it | Determine `newStatus`, then call `!status.canTransitionTo(newStatus)` to reject |
| BUG-012 | Unqualified `CANCELLED` and `FILLED` inside `Order.java` | Enum constants not in unqualified scope | Use `OrderStatus.CANCELLED` / `OrderStatus.FILLED` |
| BUG-013 | `if (newFill == quantity)` or `newFill < quantity` | `BigDecimal` is an object; `==` compares references and `<` does not compile | `newFill.compareTo(quantity) == 0` and `< 0` |
| BUG-014 | Partial fill set `status = CANCELLED` | Incorrect domain rule; a non-final fill is still active | `PARTIALLY_FILLED` for `0 < filled < quantity`; `FILLED` when equal |
| BUG-015 | Mixed `if` and ternary: `OrderStatus newStatus = if (...) { ... }` | Java grammar: ternary is an expression; `if` is a statement | `condition ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED` |
| BUG-016 | `filledQuantity = newFill` before checking `canTransitionTo()` | Failed transition can leave partially mutated object | **Validate -> calculate -> validate transition -> commit both fields** |
| BUG-017 | `throw IllegalStateException("...")` | Missing `new` keyword for exception construction | `throw new IllegalStateException("...");` |
| BUG-018 | `private void cancel()` / `canTransitionTo(newStatus)` where `newStatus` not defined | Wrong visibility and undefined variable | Use public `cancel()`, validate against the literal `OrderStatus.CANCELLED`, then set state |
| BUG-019 | No getters for `filledQuantity` / `status` | JUnit tests need to inspect state while preserving encapsulation | Add `public BigDecimal getFilledQuantity()` and `public OrderStatus getStatus()`; no setters |

### OrderSide / OrderStatus enum problems

- **BUG-020 — Rich enum constructor was public:** `public OrderSide(int sign)` is invalid for a Java enum. Use `private OrderSide(int sign)` (or package-private).
- **BUG-021 — `double` used for signed quantity:** `apply(double quantity)` invites binary floating-point issues. Use `apply(BigDecimal quantity)` and import `java.math.BigDecimal`.
- **BUG-022 — Direct enum self-references in `Set.of(PARTIALLY_FILLED, FILLED, CANCELLED)` constant initialisers:** problematic forward-initialisation design (flagged during review; not captured as a Maven failure). Replaced with a Java 21 `switch (this)` transition function.
- **BUG-023 — FSM self-transition confusion:** another execution can leave an order `PARTIALLY_FILLED`. `PARTIALLY_FILLED -> PARTIALLY_FILLED` is permitted for successive partial fills. `FILLED` and `CANCELLED` remain terminal.

## 3. Order unit-test and fixture mistakes caught during review

| ID | Symptom | Correction / learning |
| --- | --- | --- |
| BUG-024 | Test expected `getRemainingQuantity() == 4` after fill 4 of 10 and cancellation | `filledQuantity == 4`, **unfilled** `getRemainingQuantity() == 6`; cancellation does not undo executions |
| BUG-025 | Test expected `PARTIALLY_FILLED` after fill 4 then fill 6 on order of 10 | Entire order has executed: expect `FILLED`, filled 10, remaining 0 |
| BUG-026 | `BigDecimal.value(10)` | Use `BigDecimal.valueOf(10)` |
| BUG-027 | Missing semicolon after JUnit `assertEquals(...)`, incomplete `@Test void should` | Correct syntax and remove unfinished method |
| BUG-028 | Fixture `buyOrder(BigDecimal quantity)` ignored its parameter and hardcoded `BigDecimal.TEN` | Forward the supplied `quantity` |
| BUG-029 | Fixture passed `BigDecimal.ZERO` as a seventh factory argument | Factory takes six inputs; `filledQuantity` is internal, initialized to zero |
| BUG-030 | Inconsistent `OrderFixture` vs `OrderFixtures` and method names | Pick one convention, keep filename equal to public class name, and update test references |
| BUG-031 | `shouldRejectFillExceedingRemainingQuantity()` only checked exception | Also assert filled quantity stayed 4 and status stayed `PARTIALLY_FILLED` |
| BUG-032 | Terminal-state tests only checked exception | Assert status and filled quantity remain unchanged after rejected execution/cancellation |

## 4. Earlier Account model issues during TRADE-004

These were found while building the `Account` portion, which was later merged to `main` in [PR #25](https://github.com/popescu-alexandru-unilu/TradeCore/pull/25).

| ID | Mistake | Fix / invariant |
| --- | --- | --- |
| BUG-033 | `this.initialBalance = BigDecimal.ZERO` with no matching field | `this.balance = initialBalance`; `this.reserved = BigDecimal.ZERO` |
| BUG-034 | `BigDecimal.substract()` or `BigDecimal` compared with primitive `<` | Use `subtract()` and `compareTo(BigDecimal.ZERO)` |
| BUG-035 | Missing braces / extra semicolon in static factory, `Account`/ `Acount` typo | Correct Java method and class syntax |
| BUG-036 | `reserveFunds()` used inside `releaseFunds()` comparison | `reserveFunds()` is `void` and would mutate state; compare `amount` against `reserved` |
| BUG-037 | Release check used `amount.compareTo(reserved) >= 0` | Equal-amount release **must succeed**; reject only when `> 0` |
| BUG-038 | `getBalance()` / `getReserved()` declared to return `int` | Stored fields are `BigDecimal`; getter return type must match |
| BUG-039 | `assertEquals(..., acc.releaseFunds(...))` | `releaseFunds()` returns `void`; call first, then assert `getAvailableBalance()` |
| BUG-040 | `assertThrows(..., () -> BigDecimal.ZERO)` or `BigDecimal.valueOf(null)` | Lambda must invoke the code under test: `() -> acc.reserveFunds(BigDecimal.ZERO)` or `() -> acc.reserveFunds(null)` |
| BUG-041 | `assertThrows(..., acc.executeReservedFunds(...))` | Pass an executable lambda: `() -> acc.executeReservedFunds(...)` |
| BUG-042 | After a rejected execution, expected balances reflected a **successful** execution | Rejection leaves balance/reserved unchanged, e.g. total 1000, reserved 300, available 700 |
| BUG-043 | Partial release of 150 from 400 reserved (total 1000) expected available 1000 | Correct reserved 250, available 750, total 1000 |
| BUG-044 | First `executeReservedFunds()` implementation updated one field before calculating the other | Calculate both immutable `BigDecimal` results in locals before assigning fields; **not** a guarantee of thread-safety |

## 5. Common principles extracted from these bugs

**Domain invariants:** `available = balance - reserved`; `0 <= reserved <= balance`. For orders: `0 <= filledQuantity <= quantity`, and `FILLED` / `CANCELLED` reject new fills.

**Validate -> Calculate -> Commit:** reject bad input/state before changing fields, compute new `BigDecimal` values, verify FSM rules, then assign fields. This reduces partial mutation but **does not make two field assignments atomic across threads**.

**Java type and API contracts:** a `void` method cannot be an `assertEquals` value; a returned boolean must be examined; use `this.field = parameter` for shadowed names; use `BigDecimal.compareTo()` for numeric comparisons; Java method visibility must match how the domain API is used.

**JUnit:** call the operation inside `assertThrows` via a lambda. Also assert the unchanged object state after rejection, not only the exception type. A fresh test fixture instance avoids mutable cross-test contamination.

**Production concerns remain separate:** in-memory validations are **not** database atomicity, idempotency, or distributed thread safety. PostgreSQL transactions and concurrency protections are future implementation work; Redis and Kafka are not substitutes for them.

## 6. Current verification and next steps

- **Observed:** Maven main compilation failed on `final` assignments, then a later run reached test compilation and failed on private `cancel()`.
- **Not yet observed:** a successful `mvn clean test` run for the Order model. The tests and Java files may still only exist locally.
- **Next:** set `cancel()` to `public`, confirm `status` and `filledQuantity` are non-final, then run:
  ```bash
  mvn clean test
  ```
- Once green, review `git status`, commit the Java files and tests to `feature/004-order-model`, push and open a PR targeting `main`. Keep TRADE-004 open until `Trade` and `CashLedgerEntry` are completed.

**When adding another bug:** record **symptom -> evidence -> root cause -> smallest fix -> regression test -> verified result**. Keep code-review findings distinct from actual compiler/test failures.

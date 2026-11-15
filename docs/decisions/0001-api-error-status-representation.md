# ADR-0001: Represent HTTP status as an integer in API error responses

- **Status:** Proposed (pending review)
- **Date:** 2026-11-03
- **Related ticket:** [TRADE-002: Define consistent API error responses](https://github.com/popescu-alexandru-unilu/TradeCore/issues/2)

## Context

TradeCore should return a consistent JSON body for errors. An illustrative response:

```json
{
  "status": 404,
  "code": "ORDER_NOT_FOUND",
  "message": "Order 123 does not exist",
  "path": "/api/orders/123"
}
```

In Java, we could store the `status` field as an `int` or as a restricted type such as Spring's `HttpStatus` enum. Why choose an integer?

## Decision

The external `ApiError` response DTO has an **`int status`** field, representing the numeric HTTP status code (e.g. `400`, `404`, `500`). Internal Spring controller/exception handling uses **`HttpStatus`** (or Spring's `HttpStatusCode` interface as appropriate), converting to the numeric value at the HTTP response boundary.

Example:

```java
HttpStatus status = HttpStatus.NOT_FOUND; // internal type-safe representation
int value = status.value();             // external JSON representation: 404
```

The status in the JSON body must match the actual HTTP response status. The `code` field is a separate machine-readable **application error code**, not an HTTP status name.

## Why

- External API clients across languages already understand standard numeric HTTP codes.
- The DTO expresses the wire contract rather than internal Java/Spring types.
- Internal use of `HttpStatus` maintains readable, restricted choices where application code makes the decision.
- Avoid relying on default enum JSON serialisation (which might produce a string such as `"NOT_FOUND"` without custom configuration).

## Alternatives considered

| Option | Pros | Cons |
| --- | --- | --- |
| `int` in DTO (selected) | Standard wire format; simple JSON; independent of Java enum names | An unvalidated integer permits values such as `999` |
| `HttpStatus` enum in DTO | Type safety and discoverability | Leaks framework type into DTO; requires explicit serialisation control for numeric output |
| String enum name (`"NOT_FOUND"`) | Human-readable | Nonstandard as HTTP status, Java-specific naming, extra mapping for clients |
| Omit `status` from body | Avoids duplicating HTTP status line | Clients reading stored/logged error JSON lack this context |
| RFC 9457 `ProblemDetail` | Standard problem details format and Spring support | Different response contract; evaluate separately before API consumers depend on the current schema |

## Consequences and risks

- **Validation:** `int` is not inherently type-safe. Exception handlers should create the DTO from a validated `HttpStatus`/`HttpStatusCode`, rather than accept arbitrary status numbers.
- **Consistency:** Ensure `ResponseEntity`/HTTP status and `ApiError.status` are generated from the *same* status object to prevent mismatches.
- **Safety:** Unexpected exceptions must map to HTTP 500 with a generic public message. Never expose stack traces or credentials.
- **Duplication:** `status` repeats the HTTP status, intentionally, for self-contained error bodies.

## Possible improvements / revisit when

- Add a dedicated factory or record constructor validation if developers construct `ApiError` in multiple places.
- Consider standardising on Spring's `ProblemDetail` (RFC 9457) before the external contract becomes stable; document migration/versioning impact.
- Re-evaluate whether the `status` field is needed if consumers universally use status from the HTTP response.
- Add API contract/integration tests to assert consistency between the actual HTTP status and JSON status, including 400, 404 and 500.

## Verification

Under TRADE-002, test 400, 404 and 500 responses, stable JSON fields and non-disclosure of internal error details.

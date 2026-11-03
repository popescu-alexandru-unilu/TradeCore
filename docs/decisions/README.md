# Architecture Decision Records (ADRs)

We document **why** TradeCore makes important technical choices, including trade-offs and what we would change if requirements or scale evolve.

## Decision log

| ID | Decision | Status | Related ticket |
| --- | --- | --- | --- |
| [ADR-0001](0001-api-error-status-representation.md) | API error responses expose numeric HTTP status | Proposed | [TRADE-002](https://github.com/popescu-alexandru-unilu/TradeCore/issues/2) |

## How to use

1. For a non-trivial design choice, copy [TEMPLATE.md](TEMPLATE.md) into a numbered ADR.
2. Discuss options before implementation or during review. Describe the context, choice, alternatives, consequences, and possible optimisations.
3. Create a PR to review the ADR alongside the implementation (or as a documentation-only PR).
4. Mark **Accepted** when the team approves/merges the decision. Never silently rewrite an accepted decision; create a superseding ADR and link to the old one if the design changes.

Status vocabulary: **Proposed**, **Accepted**, **Superseded**.

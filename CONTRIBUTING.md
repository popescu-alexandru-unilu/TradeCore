# TradeCore engineering agreement

1. Pick one ticket from the GitHub Issues or [Roadmap](docs/ROADMAP.md).
2. Discuss the requirements and design before coding.
3. Create a feature branch: `feature/NNN-short-description` or `fix/NNN-short-description`.
4. Make focused commits and write tests: `feat: validate instrument symbol`.
5. Run `mvn test` locally.
6. Open a pull request with `Closes #<issue number>` and explain what changed.
7. Review correctness, API contracts, failure modes, race conditions, and algorithm complexity.
8. Merge only after tests pass; squash merge preferred.

**Definition of Done:** acceptance criteria met, tests passing, reasoning documented, and no secrets or real financial data.

For DSA tasks, implement a brute-force baseline, identify wasted work, optimize, cover edge cases, and explain Big-O. Benchmark when useful.

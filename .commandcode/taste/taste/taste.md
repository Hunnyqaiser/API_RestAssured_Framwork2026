# Taste
- Prefers fixes that preserve the framework's core components and the API request flow; when fixing failing tests, change test config/data rather than core or request-flow code. Confidence: 0.9
- Asks for comprehensive QA-style coverage: positive, negative, edge, boundary, security, schema, and NFR (response-time) cases — not just happy paths. Thinks like a senior (8+ years) QA engineer. Confidence: 0.9
- For Java source files, uses tabs (not spaces) and javadoc on public test methods explaining the scenario and case type. Confidence: 0.85
- Prefers probing live API behavior with curl before writing assertions, instead of guessing status codes or response shapes; documents live behavior in test names/comments when the API is non-obvious. Confidence: 0.85
- Expects state-creating tests to clean up after themselves (try/finally delete on gorest users, contacts, etc.) so the suite can be re-run idempotently. Confidence: 0.8
- Reads project documentation files (e.g. AI_CONTEXT.md) before starting work to understand framework components, gotchas, and conventions. Confidence: 0.8
- Tolerates direct `RestAssured` calls in tests when the framework's response spec can't represent an expected status code (422, 429, 400, etc.); the framework `post`/`get`/`patch` specs only cover a narrow set. Confidence: 0.8
- Wants honest reporting of environmental flakes (e.g. external rate limiting) rather than silently masking them with skips or retries inside tests. Confidence: 0.75

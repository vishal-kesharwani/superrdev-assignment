# Handwritten notes — index

Drafts to handwrite for the submission, with the screenshots each one references.
Screenshots live in `screenshots/`.

| File | Bug | Screenshots used |
|------|-----|------------------|
| `bug1-explanation.md` | SQL AND/OR precedence (archived/status leak) | `beforefix`, `proof`, `beforestatusdone`, `query`, `afterfix`, `h2query`, `afterstatusdone` |
| `bug2-explanation.md` | Artificial `Thread.sleep` latency | `beforebug2-timing`, `afterbug2-timing` |
| `bug5-explanation.md` | 500 instead of 400 on bad input | `beforebug5-500`, `afterbug5-400` |
| `bug3-4-6-7-explanations.md` | useTasks loading state · page reset · LIKE escaping · ORDER BY tie-breaker | — (evidence: curl runs, network log, `mvnw test` 10/10) |

All before/after screenshots were taken live against the running app — backend on
`localhost:8080`, frontend on `localhost:5173`.

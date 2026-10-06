# NOTES

## How I worked

README first, project run as documented. Smoke-tested with curl, browser and H2 console —
every symptom reproduced before touching code and after the fix.
One bug per commit, focused diff. Finished with `mvnw test` (10/10) and `npm run build`.

## Summary of changes

- **SQL AND/OR precedence** (`TaskRepository`, both `.sql` files): parenthesised the `OR` group so `archived`/`status` bind to every row — `?q=api` 10 → 8 rows (no archived); `?q=api&status=DONE` 5 mixed rows → 0.
- **Artificial latency** (`TaskController`): removed `Thread.sleep((10 - len) * 100)` — empty query 1.02s → 0.01s; `System.out` → SLF4J.
- **`useTasks` hook**: `AbortController` cancels stale requests, `error` cleared per request, `loading` reset in `finally`, 300ms debounce (6 keystrokes → 1 request). Backend down shows an error, not a spinner.
- **Page reset** (`App.jsx`): query/status changes reset `page` to 1 — searching from page 3 no longer shows "No tasks found".
- **Validation** (`TaskController`): bad `status`, `page < 1`, `pageSize < 1`, oversized offset → 400 with a message; `pageSize` clamped to 100 (clamp satisfiable, reject nonsense).
- **Found independently**: `?q=%` returned every row (unescaped LIKE wildcards) and `ORDER BY` lacked a tie-breaker — fixed with `ESCAPE` and `id DESC`.
- **Improvements**: DB-level pagination (`Pageable` + count query), removed `console.log`, 10 tests.

## What I chose not to change and why

`@CrossOrigin` and the H2 console are dev-only: flagged, not reconfigured. Oracle's `VARCHAR2(257)` overflow and NULL `p_page` guards never run locally — out of timebox.

## Biggest remaining risk

No authentication, no frontend tests; reads hit in-memory H2 that won't hold at volume.

## Tools/AI used and how

Used opencode (GenAI) to explore the codebase and draft patches. I reproduced every bug myself (curl, headless Chrome/CDP, H2 console), reviewed and split each diff myself, and ran build and tests.

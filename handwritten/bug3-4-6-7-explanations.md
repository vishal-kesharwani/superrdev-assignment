# Bugs 3, 4, 6, 7 — explanation drafts

Drafts for the remaining four fixes. No browser screenshots exist for these; the evidence
below is from curl runs, network logs and the test suite (numbers as verified in the
session). Handwrite these in the same style as bugs 1, 2 and 5.

---

# Bug 3 — Task list stuck on "Loading…" when the request fails

**Layer:** frontend
**Files:** `frontend/src/hooks/useTasks.js`, `frontend/src/api.js`, `frontend/src/hooks/useDebouncedValue.js` (new)

## How I found it

I stopped the backend mid-session and clicked Refresh in the UI. Instead of an error, the
table spun forever. Rapid typing also fired a request per keystroke, and an *older*
response could overwrite a newer one.

## Root cause

In `useTasks`, `loading` was set to `true` when a request started but never reset in the
failure path — no `finally`. There was also no request cancellation, so out-of-order
responses raced each other, and no debounce, so six keystrokes meant six requests.

## What I changed

- `AbortController` per request; superseded requests are aborted (`api.js` passes `signal`)
- `error` cleared at the start of each request, `loading` reset in `finally`
- New `useDebouncedValue(300ms)` on the query so six keystrokes → **1 request**

## After (verification)

- Backend down → `"Error: Request failed: 500"` card renders (screenshot taken with
  headless Chrome); backend restarted → Refresh recovers the list
- Six fast keystrokes → **1** request in the network log (was 6)

---

# Bug 4 — Page not reset when search or status changes

**Layer:** frontend
**Files:** `frontend/src/App.jsx`

## How I found it

I paged to page 3, then typed a search that matched a single task. The UI said
**"No tasks found"** while the pagination control still showed page 3 — the result was
there, just not on the page the app was asking for.

## Root cause

The query and status handlers updated their state but never reset `page`. The filtered
result set shrank to one page while `page` stayed at `3`, so the backend correctly
returned an empty slice.

## What I changed

Both handlers call `setPage(1)` alongside their update (query input and status select).

## After (verification)

On page 3, typing "login" → the matching row appears immediately (headless Chrome, before:
"No tasks found" / after: row visible). Covered by the UI flow; the backend contract
(`page` is 1-based) is covered by tests.

---

# Bug 6 — LIKE wildcards in user input are not escaped *(found on my own)*

**Layer:** SQL / backend
**Files:** `TaskRepository.java`, `db/queries/search_tasks.sql`, `db/oracle/task_search_package.sql`

## How I found it

While testing the search I tried the wildcard characters users actually type:

```
curl "localhost:8080/api/tasks?q=%"   → total: 47  (every row)
curl "localhost:8080/api/tasks?q=_"   → also matched broadly
```

`%` means "match anything" to SQL — so searching for a literal percent sign returned the
entire table. The user's input was concatenated into the pattern as-is.

## What I changed

A small `escapeLike()` helper escapes `%`, `_` and `\` in the search term, and every
`LIKE … :term` in the H2 query, the reference `.sql` file and the Oracle package (COUNT +
cursor) got `ESCAPE '\'`.

## After (verification)

`?q=%` → **total: 0** (no task title/description contains a literal `%`) — was 47.

---

# Bug 7 — Unstable `ORDER BY` breaks pagination *(found on my own)*

**Layer:** SQL / backend
**Files:** `TaskRepository.java`, `db/queries/search_tasks.sql`, `db/oracle/task_search_package.sql`

## How I found it

Reviewing the search query I noticed `ORDER BY created_at DESC` with no tie-breaker.
Rows sharing a `created_at` have no defined relative order, and H2 is free to return them
differently between the page-1 query and the page-2 query — a row can then appear on two
pages or on none (duplicate / missing rows while paging).

## What I changed

`ORDER BY created_at DESC, id DESC` — `id` is unique, so every sort is total and page
boundaries are deterministic. Applied in the repository and both SQL files.

## After (verification)

`TaskRepositoryTest` asserts the exact ordering (ties broken by `id`), so a regression
fails the build: `mvnw test` → **10/10 pass**.

-- H2-compatible task search query
-- Used by the Spring Data repository layer (TaskRepository.searchTasks)
--
-- Parameters:
--   :term   — search term wrapped in wildcards and escaped, e.g. '%api%'
--   :status — status filter or NULL for all statuses
--
-- Notes:
--   * The OR group must stay parenthesised: AND binds tighter than OR, so without
--     the parentheses the archived and status filters would not apply to every row.
--   * ESCAPE '\' lets a literal % or _ in the user's term match itself.
--   * id DESC is a tie-breaker so pagination stays stable when created_at ties.
--   * Pagination is applied by the caller (LIMIT :pageSize OFFSET :offset).

SELECT *
FROM tasks
WHERE archived = FALSE
  AND (LOWER(title) LIKE :term ESCAPE '\' OR LOWER(description) LIKE :term ESCAPE '\')
  AND (:status IS NULL OR status = :status)
ORDER BY created_at DESC, id DESC;

package com.internal.tasktracker;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Shared WHERE clause for the paged query and its count query so the two can never
    // drift apart (a mismatched countQuery silently breaks pagination metadata).
    // Parentheses around the OR group are required: AND binds tighter than OR.
    // ESCAPE '\' makes a literal % or _ typed by the user match itself, not "any chars".
    String SEARCH_PREDICATES = "archived = FALSE "
            + "AND (LOWER(title) LIKE :term ESCAPE '\\' OR LOWER(description) LIKE :term ESCAPE '\\') "
            + "AND (:status IS NULL OR status = :status)";

    // id DESC tie-breaker keeps page 1/2/3 stable when created_at values are identical.
    // Pagination itself is done by the database via Pageable instead of loading every row.
    @Query(value = "SELECT * FROM tasks WHERE " + SEARCH_PREDICATES
                 + " ORDER BY created_at DESC, id DESC",
           countQuery = "SELECT COUNT(*) FROM tasks WHERE " + SEARCH_PREDICATES,
           nativeQuery = true)
    Page<Task> searchTasks(@Param("term") String term,
                           @Param("status") String status,
                           Pageable pageable);
}

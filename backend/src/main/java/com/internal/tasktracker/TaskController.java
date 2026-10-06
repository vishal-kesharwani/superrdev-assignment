package com.internal.tasktracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    /** Upper bound for pageSize so a single request cannot ask the DB for an unbounded result set. */
    static final int MAX_PAGE_SIZE = 100;

    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping("/api/tasks")
    public ResponseEntity<?> searchTasks(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {

        // Normalize query input
        String query = q == null ? "" : q.trim();
        String searchTerm = "%" + escapeLike(query.toLowerCase()) + "%";

        // --- Input validation -------------------------------------------------
        // page/pageSize are int, so a non-numeric value already fails in Spring with a 400.
        // What is left for us to check are the values that are numeric but nonsensical.
        if (page < 1) {
            return badRequest("page must be >= 1, got " + page);
        }
        if (pageSize < 1) {
            return badRequest("pageSize must be >= 1, got " + pageSize);
        }
        if (pageSize > MAX_PAGE_SIZE) {
            // Clamp instead of rejecting: the client simply wants "more", and we can
            // safely give it the maximum. Protects the DB from unbounded result sets.
            pageSize = MAX_PAGE_SIZE;
        }
        // Spring Data refuses offsets beyond Integer.MAX_VALUE, so reject instead of 500-ing.
        if ((long) (page - 1) * pageSize > Integer.MAX_VALUE) {
            return badRequest("page " + page + " with pageSize " + pageSize
                    + " exceeds the maximum supported offset");
        }

        // Parse status filter
        String normalizedStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                normalizedStatus = TaskStatus.valueOf(status.toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                return badRequest("Invalid status '" + status + "'. Allowed values: "
                        + Arrays.toString(TaskStatus.values()));
            }
        }

        log.debug("q=\"{}\" status={} page={} pageSize={}", query, normalizedStatus, page, pageSize);

        // DB-level pagination: the database returns one page plus an exact count,
        // instead of loading every matching row into memory and subList-ing it.
        Page<Task> result = taskRepository.searchTasks(
                searchTerm, normalizedStatus, PageRequest.of(page - 1, pageSize));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", result.getContent());
        response.put("total", result.getTotalElements());
        response.put("page", page);
        response.put("pageSize", pageSize);

        return ResponseEntity.ok(response);
    }

    /**
     * Escapes LIKE wildcards so user input is matched literally.
     * Backslash must be escaped first, otherwise it would escape the escapes we add after it.
     */
    private static String escapeLike(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * A plain 400 body rather than ResponseStatusException: Spring Boot 3 hides exception
     * messages in error bodies by default (server.error.include-message=never), so the
     * reason of a ResponseStatusException would never reach the client.
     */
    private static ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Bad Request");
        body.put("message", message);
        return ResponseEntity.badRequest().body(body);
    }
}

package com.internal.tasktracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for the search query. The archived/status parentheses bug
 * and the missing tie-breaker in ORDER BY both showed up in this query.
 */
@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void searchNeverReturnsArchivedTasks() {
        Page<Task> result = taskRepository.searchTasks("%api%", null, PageRequest.of(0, 100));

        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).noneMatch(Task::isArchived);
    }

    @Test
    void statusFilterAppliesToTitleAndDescriptionMatches() {
        Page<Task> result = taskRepository.searchTasks("%api%", "DONE", PageRequest.of(0, 100));

        assertThat(result.getContent()).allMatch(task -> "DONE".equals(task.getStatus()));
    }

    @Test
    void equalTimestampsAreOrderedByIdDescending() {
        LocalDateTime now = LocalDateTime.of(2030, 1, 1, 12, 0);
        Task first = task("Tie break A", now);
        Task second = task("Tie break B", now);
        taskRepository.save(first);
        taskRepository.save(second);

        List<Task> results = taskRepository
                .searchTasks("%tie break%", null, PageRequest.of(0, 10))
                .getContent();

        assertThat(results).extracting(Task::getTitle)
                .containsExactly("Tie break B", "Tie break A");
    }

    private static Task task(String title, LocalDateTime createdAt) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(title);
        task.setStatus("OPEN");
        task.setArchived(false);
        task.setCreatedAt(createdAt);
        return task;
    }
}

package com.internal.tasktracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void invalidStatusReturns400WithMessage() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid status")));
    }

    @Test
    void pageBelowOneReturns400() throws Exception {
        mockMvc.perform(get("/api/tasks").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("page must be >= 1")));
    }

    @Test
    void negativePageSizeReturns400() throws Exception {
        mockMvc.perform(get("/api/tasks").param("pageSize", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("pageSize must be >= 1")));
    }

    @Test
    void oversizedPageSizeIsClampedTo100() throws Exception {
        mockMvc.perform(get("/api/tasks").param("pageSize", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(100));
    }

    @Test
    void statusFilterIsAppliedTogetherWithSearch() throws Exception {
        mockMvc.perform(get("/api/tasks").param("q", "api").param("status", "OPEN").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].status", everyItem(is("OPEN"))));
    }

    @Test
    void archivedTasksNeverAppearInSearchResults() throws Exception {
        mockMvc.perform(get("/api/tasks").param("q", "api").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].archived", everyItem(is(false))));
    }

    @Test
    void percentInSearchTermIsMatchedLiterally() throws Exception {
        mockMvc.perform(get("/api/tasks").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }
}

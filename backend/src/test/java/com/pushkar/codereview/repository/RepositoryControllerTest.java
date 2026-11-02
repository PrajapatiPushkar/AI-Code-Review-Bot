package com.pushkar.codereview.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.codereview.exception.GlobalExceptionHandler;
import com.pushkar.codereview.exception.ResourceNotFoundException;
import com.pushkar.codereview.repository.dto.RepositoryCreateRequest;
import com.pushkar.codereview.repository.dto.RepositoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RepositoryControllerTest {

    private MockMvc mockMvc;
    private StubRepositoryService stubService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        stubService = new StubRepositoryService();
        RepositoryController controller = new RepositoryController(stubService);
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/repositories returns current user's repositories (200 OK)")
    void testGetCurrentUserRepositories_Returns200OK() throws Exception {
        RepositoryResponse repo = new RepositoryResponse(
                1L, 101L, 1111L, "repo-a", "usera/repo-a", "main", "https://github.com/usera/repo-a",
                true, Instant.now(), Instant.now()
        );
        stubService.currentUserRepositories.add(repo);

        mockMvc.perform(get("/api/v1/repositories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].fullName", is("usera/repo-a")))
                .andExpect(jsonPath("$[0].userId", is(101)));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id} for owned repository returns 200 OK")
    void testGetRepositoryById_Owned_Returns200OK() throws Exception {
        RepositoryResponse repo = new RepositoryResponse(
                1L, 101L, 1111L, "repo-a", "usera/repo-a", "main", "https://github.com/usera/repo-a",
                true, Instant.now(), Instant.now()
        );
        stubService.repositoryResponse = repo;

        mockMvc.perform(get("/api/v1/repositories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.fullName", is("usera/repo-a")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id} for unowned repository returns 404 Not Found without leaking existence")
    void testGetRepositoryById_Unowned_Returns404NotFound() throws Exception {
        stubService.throwNotFoundOnGetById = true;

        mockMvc.perform(get("/api/v1/repositories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Resource Not Found")))
                .andExpect(jsonPath("$.message", is("Repository not found with ID: 1")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/user/{userId} for own userId returns 200 OK")
    void testGetRepositoriesByUserId_OwnUserId_Returns200OK() throws Exception {
        RepositoryResponse repo = new RepositoryResponse(
                1L, 101L, 1111L, "repo-a", "usera/repo-a", "main", "https://github.com/usera/repo-a",
                true, Instant.now(), Instant.now()
        );
        stubService.userRepositories.add(repo);

        mockMvc.perform(get("/api/v1/repositories/user/101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName", is("usera/repo-a")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/user/{userId} for another user returns 404 Not Found")
    void testGetRepositoriesByUserId_OtherUser_Returns404NotFound() throws Exception {
        stubService.throwNotFoundOnGetByUserId = true;

        mockMvc.perform(get("/api/v1/repositories/user/102")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Resource Not Found")))
                .andExpect(jsonPath("$.message", is("User not found with ID: 102")));
    }

    @Test
    @DisplayName("POST /api/v1/repositories with malformed JSON returns 400 Bad Request without leaking internal details")
    void testCreateRepository_MalformedJson_Returns400BadRequest() throws Exception {
        String malformedJson = "{ userId: 101, name: ";

        mockMvc.perform(post("/api/v1/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Malformed or unreadable JSON request body")));
    }

    // --- Stub RepositoryService ---

    private static class StubRepositoryService extends RepositoryService {
        RepositoryResponse repositoryResponse;
        List<RepositoryResponse> currentUserRepositories = new ArrayList<>();
        List<RepositoryResponse> userRepositories = new ArrayList<>();
        boolean throwNotFoundOnGetById = false;
        boolean throwNotFoundOnGetByUserId = false;

        public StubRepositoryService() {
            super(null, null, null, null);
        }

        @Override
        public RepositoryResponse getRepositoryById(Long id) {
            if (throwNotFoundOnGetById) {
                throw new ResourceNotFoundException("Repository not found with ID: " + id);
            }
            return repositoryResponse;
        }

        @Override
        public List<RepositoryResponse> getCurrentUserRepositories() {
            return currentUserRepositories;
        }

        @Override
        public List<RepositoryResponse> getRepositoriesByUserId(Long userId) {
            if (throwNotFoundOnGetByUserId) {
                throw new ResourceNotFoundException("User not found with ID: " + userId);
            }
            return userRepositories;
        }

        @Override
        public RepositoryResponse createRepository(RepositoryCreateRequest request) {
            return new RepositoryResponse(
                    1L, request.getUserId(), request.getGithubRepositoryId(),
                    request.getName(), request.getFullName(), request.getDefaultBranch(),
                    request.getHtmlUrl(), request.getIsActive(), Instant.now(), Instant.now()
            );
        }
    }
}

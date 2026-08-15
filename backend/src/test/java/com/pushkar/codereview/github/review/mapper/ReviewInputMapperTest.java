package com.pushkar.codereview.github.review.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pushkar.codereview.github.client.dto.GithubPullRequestFileResponse;
import com.pushkar.codereview.github.client.dto.GithubPullRequestResponse;
import com.pushkar.codereview.github.client.dto.GithubRepositoryResponse;
import com.pushkar.codereview.github.review.dto.PullRequestReviewContext;
import com.pushkar.codereview.github.review.dto.ReviewFileInput;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewInputMapperTest {

    private ReviewInputMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mapper = new ReviewInputMapper();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testToReviewInput_CompleteContextMapping() {
        Instant now = Instant.now();
        GithubRepositoryResponse repo = new GithubRepositoryResponse(
                100L, "hello-world", "octocat/hello-world", false, "https://github.com/octocat/hello-world", "main"
        );
        GithubPullRequestResponse pr = new GithubPullRequestResponse(
                200L, 42L, "Add feature", "Feature description", "open",
                "https://github.com/octocat/hello-world/pull/42",
                new GithubPullRequestResponse.UserResponse("octocat"),
                new GithubPullRequestResponse.GitRefResponse("feature"),
                new GithubPullRequestResponse.GitRefResponse("main"),
                now, now
        );
        GithubPullRequestFileResponse file = new GithubPullRequestFileResponse(
                "sha123", "src/Main.java", "modified", 10, 2, 12, "@@ -1,2 +1,2 @@", null
        );

        PullRequestReviewContext context = new PullRequestReviewContext(repo, pr, List.of(file));

        ReviewInput result = mapper.toReviewInput(context);

        assertThat(result).isNotNull();
        // Repository mapping
        assertThat(result.getRepositoryId()).isEqualTo(100L);
        assertThat(result.getRepositoryName()).isEqualTo("hello-world");
        assertThat(result.getRepositoryFullName()).isEqualTo("octocat/hello-world");
        assertThat(result.getRepositoryUrl()).isEqualTo("https://github.com/octocat/hello-world");
        assertThat(result.getDefaultBranch()).isEqualTo("main");

        // Pull request mapping
        assertThat(result.getPullRequestId()).isEqualTo(200L);
        assertThat(result.getPullRequestNumber()).isEqualTo(42L);
        assertThat(result.getTitle()).isEqualTo("Add feature");
        assertThat(result.getBody()).isEqualTo("Feature description");
        assertThat(result.getState()).isEqualTo("open");
        assertThat(result.getPullRequestUrl()).isEqualTo("https://github.com/octocat/hello-world/pull/42");
        assertThat(result.getAuthorLogin()).isEqualTo("octocat");
        assertThat(result.getHeadBranch()).isEqualTo("feature");
        assertThat(result.getBaseBranch()).isEqualTo("main");
        assertThat(result.getCreatedAt()).isEqualTo(now);
        assertThat(result.getUpdatedAt()).isEqualTo(now);

        // File mapping
        assertThat(result.getFiles()).hasSize(1);
        ReviewFileInput fileInput = result.getFiles().get(0);
        assertThat(fileInput.getFilename()).isEqualTo("src/Main.java");
        assertThat(fileInput.getStatus()).isEqualTo("modified");
        assertThat(fileInput.getAdditions()).isEqualTo(10);
        assertThat(fileInput.getDeletions()).isEqualTo(2);
        assertThat(fileInput.getChanges()).isEqualTo(12);
        assertThat(fileInput.getPatch()).isEqualTo("@@ -1,2 +1,2 @@");
        assertThat(fileInput.getPreviousFilename()).isNull();
    }

    @Test
    void testToReviewInput_MultipleChangedFiles() {
        GithubRepositoryResponse repo = new GithubRepositoryResponse(1L, "repo", "owner/repo", false, "https://github.com/owner/repo", "main");
        GithubPullRequestResponse pr = new GithubPullRequestResponse(
                2L, 1L, "Multi-file PR", "Desc", "open",
                "https://github.com/owner/repo/pull/1",
                new GithubPullRequestResponse.UserResponse("author"),
                new GithubPullRequestResponse.GitRefResponse("dev"),
                new GithubPullRequestResponse.GitRefResponse("main"),
                Instant.now(), Instant.now()
        );

        GithubPullRequestFileResponse file1 = new GithubPullRequestFileResponse("sha1", "File1.java", "added", 5, 0, 5, "@@ +1,5 @@", null);
        GithubPullRequestFileResponse file2 = new GithubPullRequestFileResponse("sha2", "File2.java", "modified", 3, 1, 4, "@@ -1,2 +1,4 @@", null);
        GithubPullRequestFileResponse file3 = new GithubPullRequestFileResponse("sha3", "File3.java", "renamed", 0, 0, 0, null, "OldFile3.java");

        PullRequestReviewContext context = new PullRequestReviewContext(repo, pr, List.of(file1, file2, file3));

        ReviewInput result = mapper.toReviewInput(context);

        assertThat(result.getFiles()).hasSize(3);
        assertThat(result.getFiles().get(0).getFilename()).isEqualTo("File1.java");
        assertThat(result.getFiles().get(1).getFilename()).isEqualTo("File2.java");
        assertThat(result.getFiles().get(2).getFilename()).isEqualTo("File3.java");
        assertThat(result.getFiles().get(2).getPreviousFilename()).isEqualTo("OldFile3.java");
    }

    @Test
    void testToReviewInput_NullPatchRemainsNull() {
        GithubRepositoryResponse repo = new GithubRepositoryResponse(1L, "repo", "owner/repo", false, "https://github.com/owner/repo", "main");
        GithubPullRequestResponse pr = new GithubPullRequestResponse(
                2L, 1L, "Title", "Body", "open",
                "https://github.com/owner/repo/pull/1",
                null, null, null, Instant.now(), Instant.now()
        );

        GithubPullRequestFileResponse binaryFile = new GithubPullRequestFileResponse("sha_bin", "image.png", "added", 0, 0, 0, null, null);

        PullRequestReviewContext context = new PullRequestReviewContext(repo, pr, List.of(binaryFile));

        ReviewInput result = mapper.toReviewInput(context);

        assertThat(result.getFiles()).hasSize(1);
        ReviewFileInput fileInput = result.getFiles().get(0);
        assertThat(fileInput.getFilename()).isEqualTo("image.png");
        assertThat(fileInput.getPatch()).isNull();
    }

    @Test
    void testToReviewInput_NullAndEmptyChangedFiles() {
        GithubRepositoryResponse repo = new GithubRepositoryResponse(1L, "repo", "owner/repo", false, "https://github.com/owner/repo", "main");
        GithubPullRequestResponse pr = new GithubPullRequestResponse(
                2L, 1L, "Title", "Body", "open",
                "https://github.com/owner/repo/pull/1",
                null, null, null, Instant.now(), Instant.now()
        );

        // Empty changed files
        PullRequestReviewContext contextEmpty = new PullRequestReviewContext(repo, pr, Collections.emptyList());
        ReviewInput resultEmpty = mapper.toReviewInput(contextEmpty);
        assertThat(resultEmpty.getFiles()).isNotNull().isEmpty();

        // Null changed files
        PullRequestReviewContext contextNull = new PullRequestReviewContext(repo, pr, null);
        ReviewInput resultNull = mapper.toReviewInput(contextNull);
        assertThat(resultNull.getFiles()).isNotNull().isEmpty();
    }

    @Test
    void testToReviewInput_NullContext() {
        ReviewInput result = mapper.toReviewInput(null);

        assertThat(result).isNotNull();
        assertThat(result.getRepositoryId()).isNull();
        assertThat(result.getPullRequestNumber()).isNull();
        assertThat(result.getFiles()).isNotNull().isEmpty();
    }

    @Test
    void testSnakeCaseToCamelCaseMapping_ThroughResponseDtos() throws Exception {
        String repoJson = """
                {
                  "id": 999,
                  "name": "my-repo",
                  "full_name": "owner/my-repo",
                  "private": false,
                  "html_url": "https://github.com/owner/my-repo",
                  "default_branch": "develop"
                }
                """;

        String prJson = """
                {
                  "id": 888,
                  "number": 15,
                  "title": "Feature 15",
                  "body": "PR Description",
                  "state": "open",
                  "html_url": "https://github.com/owner/my-repo/pull/15",
                  "user": {
                    "login": "octocat"
                  },
                  "head": {
                    "ref": "feature-15"
                  },
                  "base": {
                    "ref": "develop"
                  },
                  "created_at": "2026-08-15T10:00:00Z",
                  "updated_at": "2026-08-15T11:00:00Z"
                }
                """;

        String fileJson = """
                {
                  "sha": "abc1234",
                  "filename": "src/App.java",
                  "status": "renamed",
                  "additions": 4,
                  "deletions": 1,
                  "changes": 5,
                  "patch": "@@ -1 +1 @@",
                  "previous_filename": "src/OldApp.java"
                }
                """;

        GithubRepositoryResponse repo = objectMapper.readValue(repoJson, GithubRepositoryResponse.class);
        GithubPullRequestResponse pr = objectMapper.readValue(prJson, GithubPullRequestResponse.class);
        GithubPullRequestFileResponse file = objectMapper.readValue(fileJson, GithubPullRequestFileResponse.class);

        PullRequestReviewContext context = new PullRequestReviewContext(repo, pr, List.of(file));

        ReviewInput result = mapper.toReviewInput(context);

        assertThat(result.getRepositoryId()).isEqualTo(999L);
        assertThat(result.getRepositoryName()).isEqualTo("my-repo");
        assertThat(result.getRepositoryFullName()).isEqualTo("owner/my-repo");
        assertThat(result.getRepositoryUrl()).isEqualTo("https://github.com/owner/my-repo");
        assertThat(result.getDefaultBranch()).isEqualTo("develop");

        assertThat(result.getPullRequestId()).isEqualTo(888L);
        assertThat(result.getPullRequestNumber()).isEqualTo(15L);
        assertThat(result.getTitle()).isEqualTo("Feature 15");
        assertThat(result.getBody()).isEqualTo("PR Description");
        assertThat(result.getState()).isEqualTo("open");
        assertThat(result.getPullRequestUrl()).isEqualTo("https://github.com/owner/my-repo/pull/15");
        assertThat(result.getAuthorLogin()).isEqualTo("octocat");
        assertThat(result.getHeadBranch()).isEqualTo("feature-15");
        assertThat(result.getBaseBranch()).isEqualTo("develop");
        assertThat(result.getCreatedAt()).isEqualTo(Instant.parse("2026-08-15T10:00:00Z"));
        assertThat(result.getUpdatedAt()).isEqualTo(Instant.parse("2026-08-15T11:00:00Z"));

        ReviewFileInput fileInput = result.getFiles().get(0);
        assertThat(fileInput.getFilename()).isEqualTo("src/App.java");
        assertThat(fileInput.getStatus()).isEqualTo("renamed");
        assertThat(fileInput.getAdditions()).isEqualTo(4);
        assertThat(fileInput.getDeletions()).isEqualTo(1);
        assertThat(fileInput.getChanges()).isEqualTo(5);
        assertThat(fileInput.getPatch()).isEqualTo("@@ -1 +1 @@");
        assertThat(fileInput.getPreviousFilename()).isEqualTo("src/OldApp.java");
    }
}

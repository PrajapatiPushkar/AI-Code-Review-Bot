package com.pushkar.codereview.github.review.mapper;

import com.pushkar.codereview.github.client.dto.GithubPullRequestFileResponse;
import com.pushkar.codereview.github.client.dto.GithubPullRequestResponse;
import com.pushkar.codereview.github.client.dto.GithubRepositoryResponse;
import com.pushkar.codereview.github.review.dto.PullRequestReviewContext;
import com.pushkar.codereview.github.review.dto.ReviewFileInput;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class ReviewInputMapper {

    public ReviewInput toReviewInput(PullRequestReviewContext context) {
        if (context == null) {
            return new ReviewInput(
                    null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null,
                    Collections.emptyList()
            );
        }

        GithubRepositoryResponse repo = context.getRepository();
        Long repositoryId = null;
        String repositoryName = null;
        String repositoryFullName = null;
        String repositoryUrl = null;
        String defaultBranch = null;

        if (repo != null) {
            repositoryId = repo.getId();
            repositoryName = repo.getName();
            repositoryFullName = repo.getFullName();
            repositoryUrl = repo.getHtmlUrl();
            defaultBranch = repo.getDefaultBranch();
        }

        GithubPullRequestResponse pr = context.getPullRequest();
        Long pullRequestId = null;
        Long pullRequestNumber = null;
        String title = null;
        String body = null;
        String state = null;
        String pullRequestUrl = null;
        String authorLogin = null;
        String headBranch = null;
        String baseBranch = null;
        Instant createdAt = null;
        Instant updatedAt = null;

        if (pr != null) {
            pullRequestId = pr.getId();
            pullRequestNumber = pr.getNumber();
            title = pr.getTitle();
            body = pr.getBody();
            state = pr.getState();
            pullRequestUrl = pr.getHtmlUrl();
            if (pr.getUser() != null) {
                authorLogin = pr.getUser().getLogin();
            }
            if (pr.getHead() != null) {
                headBranch = pr.getHead().getRef();
            }
            if (pr.getBase() != null) {
                baseBranch = pr.getBase().getRef();
            }
            createdAt = pr.getCreatedAt();
            updatedAt = pr.getUpdatedAt();
        }

        List<GithubPullRequestFileResponse> changedFiles = context.getChangedFiles();
        List<ReviewFileInput> files = new ArrayList<>();

        if (changedFiles != null && !changedFiles.isEmpty()) {
            for (GithubPullRequestFileResponse file : changedFiles) {
                if (file != null) {
                    files.add(toReviewFileInput(file));
                }
            }
        }

        return new ReviewInput(
                repositoryId,
                repositoryName,
                repositoryFullName,
                repositoryUrl,
                defaultBranch,
                pullRequestId,
                pullRequestNumber,
                title,
                body,
                state,
                pullRequestUrl,
                authorLogin,
                headBranch,
                baseBranch,
                createdAt,
                updatedAt,
                files
        );
    }

    public ReviewFileInput toReviewFileInput(GithubPullRequestFileResponse fileResponse) {
        if (fileResponse == null) {
            return null;
        }

        int additions = fileResponse.getAdditions() != null ? fileResponse.getAdditions() : 0;
        int deletions = fileResponse.getDeletions() != null ? fileResponse.getDeletions() : 0;
        int changes = fileResponse.getChanges() != null ? fileResponse.getChanges() : 0;

        return new ReviewFileInput(
                fileResponse.getFilename(),
                fileResponse.getStatus(),
                additions,
                deletions,
                changes,
                fileResponse.getPatch(),
                fileResponse.getPreviousFilename()
        );
    }
}

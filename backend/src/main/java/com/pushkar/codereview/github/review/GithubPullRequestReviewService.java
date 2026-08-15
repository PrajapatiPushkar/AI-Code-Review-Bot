package com.pushkar.codereview.github.review;

import com.pushkar.codereview.github.client.GithubPullRequestClient;
import com.pushkar.codereview.github.client.GithubPullRequestFilesClient;
import com.pushkar.codereview.github.client.GithubRepositoryClient;
import com.pushkar.codereview.github.client.dto.GithubPullRequestFileResponse;
import com.pushkar.codereview.github.client.dto.GithubPullRequestResponse;
import com.pushkar.codereview.github.client.dto.GithubRepositoryResponse;
import com.pushkar.codereview.github.review.dto.PullRequestReviewContext;
import com.pushkar.codereview.github.review.dto.ReviewInput;
import com.pushkar.codereview.github.review.mapper.ReviewInputMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class GithubPullRequestReviewService {

    private final GithubRepositoryClient repositoryClient;
    private final GithubPullRequestClient pullRequestClient;
    private final GithubPullRequestFilesClient filesClient;
    private final ReviewInputMapper reviewInputMapper;

    public GithubPullRequestReviewService(GithubRepositoryClient repositoryClient,
                                         GithubPullRequestClient pullRequestClient,
                                         GithubPullRequestFilesClient filesClient) {
        this(repositoryClient, pullRequestClient, filesClient, new ReviewInputMapper());
    }

    @Autowired
    public GithubPullRequestReviewService(GithubRepositoryClient repositoryClient,
                                         GithubPullRequestClient pullRequestClient,
                                         GithubPullRequestFilesClient filesClient,
                                         ReviewInputMapper reviewInputMapper) {
        this.repositoryClient = repositoryClient;
        this.pullRequestClient = pullRequestClient;
        this.filesClient = filesClient;
        this.reviewInputMapper = reviewInputMapper != null ? reviewInputMapper : new ReviewInputMapper();
    }

    public PullRequestReviewContext getReviewContext(Long installationId,
                                                    String owner,
                                                    String repository,
                                                    int pullRequestNumber) {
        return getReviewContext(installationId, owner, repository, (long) pullRequestNumber);
    }

    public PullRequestReviewContext getReviewContext(Long installationId,
                                                    String owner,
                                                    String repository,
                                                    long pullRequestNumber) {
        validateInputs(installationId, owner, repository, pullRequestNumber);

        GithubRepositoryResponse repositoryResponse = repositoryClient.getRepository(installationId, owner, repository);
        GithubPullRequestResponse pullRequestResponse = pullRequestClient.getPullRequest(installationId, owner, repository, pullRequestNumber);
        List<GithubPullRequestFileResponse> changedFiles = filesClient.getChangedFiles(installationId, owner, repository, pullRequestNumber);

        if (changedFiles == null) {
            changedFiles = Collections.emptyList();
        }

        return new PullRequestReviewContext(repositoryResponse, pullRequestResponse, changedFiles);
    }

    public ReviewInput getReviewInput(Long installationId,
                                      String owner,
                                      String repository,
                                      int pullRequestNumber) {
        return getReviewInput(installationId, owner, repository, (long) pullRequestNumber);
    }

    public ReviewInput getReviewInput(Long installationId,
                                      String owner,
                                      String repository,
                                      long pullRequestNumber) {
        PullRequestReviewContext context = getReviewContext(installationId, owner, repository, pullRequestNumber);
        return reviewInputMapper.toReviewInput(context);
    }

    private void validateInputs(Long installationId, String owner, String repository, long pullRequestNumber) {
        if (installationId == null || installationId <= 0) {
            throw new IllegalArgumentException("Installation ID must be a positive number");
        }
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Repository owner must not be blank");
        }
        if (repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("Repository name must not be blank");
        }
        if (pullRequestNumber <= 0) {
            throw new IllegalArgumentException("Pull request number must be a positive number");
        }
    }
}

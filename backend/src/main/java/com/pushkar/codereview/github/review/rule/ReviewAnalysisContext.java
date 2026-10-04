package com.pushkar.codereview.github.review.rule;

import com.pushkar.codereview.github.review.dto.ReviewFileInput;
import com.pushkar.codereview.github.review.dto.ReviewInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Context object holding all metadata and diff files for deterministic rule analysis.
 */
public class ReviewAnalysisContext {

    private final String repository;
    private final Long pullRequestNumber;
    private final String commitSha;
    private final List<AnalyzedFile> files;

    public ReviewAnalysisContext(String repository, Long pullRequestNumber, String commitSha, List<AnalyzedFile> files) {
        this.repository = repository != null ? repository : "";
        this.pullRequestNumber = pullRequestNumber;
        this.commitSha = commitSha;
        this.files = files != null ? Collections.unmodifiableList(files) : Collections.emptyList();
    }

    public ReviewAnalysisContext(String repository, long pullRequestNumber, String commitSha, List<AnalyzedFile> files) {
        this(repository, Long.valueOf(pullRequestNumber), commitSha, files);
    }

    /**
     * Constructs a ReviewAnalysisContext from the existing ReviewInput pipeline model.
     */
    public static ReviewAnalysisContext fromReviewInput(ReviewInput input, String commitSha) {
        if (input == null) {
            return new ReviewAnalysisContext("", null, commitSha, Collections.emptyList());
        }

        String repoName = input.getRepositoryFullName() != null && !input.getRepositoryFullName().isBlank()
                ? input.getRepositoryFullName()
                : input.getRepositoryName();

        List<AnalyzedFile> analyzedFiles = new ArrayList<>();
        if (input.getFiles() != null) {
            for (ReviewFileInput fileInput : input.getFiles()) {
                if (fileInput != null) {
                    analyzedFiles.add(AnalyzedFile.fromPatch(
                            fileInput.getFilename(),
                            fileInput.getStatus(),
                            fileInput.getPatch()
                    ));
                }
            }
        }

        return new ReviewAnalysisContext(repoName, input.getPullRequestNumber(), commitSha, analyzedFiles);
    }

    public String getRepository() {
        return repository;
    }

    public Long getPullRequestNumber() {
        return pullRequestNumber;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public List<AnalyzedFile> getFiles() {
        return files;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewAnalysisContext that = (ReviewAnalysisContext) o;
        return Objects.equals(repository, that.repository) &&
                Objects.equals(pullRequestNumber, that.pullRequestNumber) &&
                Objects.equals(commitSha, that.commitSha) &&
                Objects.equals(files, that.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repository, pullRequestNumber, commitSha, files);
    }

    @Override
    public String toString() {
        return "ReviewAnalysisContext{" +
                "repository='" + repository + '\'' +
                ", pullRequestNumber=" + pullRequestNumber +
                ", commitSha='" + commitSha + '\'' +
                ", fileCount=" + files.size() +
                '}';
    }
}

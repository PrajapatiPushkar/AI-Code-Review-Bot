package com.pushkar.codereview.github.review.persistence;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CodeReviewSpecification implements Specification<CodeReview> {

    private final Long userId;
    private final CodeReviewStatus status;
    private final String owner;
    private final String repository;
    private final Integer pullRequestNumber;

    public CodeReviewSpecification(Long userId, CodeReviewStatus status, String owner, String repository, Integer pullRequestNumber) {
        this.userId = userId;
        this.status = status;
        this.owner = owner;
        this.repository = repository;
        this.pullRequestNumber = pullRequestNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public CodeReviewStatus getStatus() {
        return status;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepository() {
        return repository;
    }

    public Integer getPullRequestNumber() {
        return pullRequestNumber;
    }

    public static Specification<CodeReview> withFilters(Long userId, CodeReviewStatus status, String owner, String repository, Integer pullRequestNumber) {
        return new CodeReviewSpecification(userId, status, owner, repository, pullRequestNumber);
    }

    @Override
    public Predicate toPredicate(Root<CodeReview> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (userId != null) {
            predicates.add(cb.equal(root.get("user").get("id"), userId));
        }
        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        if (owner != null && !owner.isBlank()) {
            predicates.add(cb.equal(cb.lower(root.get("owner")), owner.toLowerCase().trim()));
        }
        if (repository != null && !repository.isBlank()) {
            predicates.add(cb.equal(cb.lower(root.get("repository")), repository.toLowerCase().trim()));
        }
        if (pullRequestNumber != null) {
            predicates.add(cb.equal(root.get("pullRequestNumber"), pullRequestNumber));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}

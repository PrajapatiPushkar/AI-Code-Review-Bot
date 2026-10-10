package com.pushkar.codereview.github.webhook;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class WebhookDeliverySpecification {

    public static Specification<GithubWebhookDelivery> withFilters(
            Long userId,
            WebhookDeliveryStatus status,
            String repository,
            Instant fromDate,
            Instant toDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (repository != null && !repository.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("repository")), "%" + repository.toLowerCase().trim() + "%"));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("receivedAt"), fromDate));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("receivedAt"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

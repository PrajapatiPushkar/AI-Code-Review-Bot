package com.pushkar.codereview.github.webhook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GithubWebhookDeliveryRepository extends JpaRepository<GithubWebhookDelivery, Long>, JpaSpecificationExecutor<GithubWebhookDelivery> {

    Optional<GithubWebhookDelivery> findByDeliveryId(String deliveryId);

    Optional<GithubWebhookDelivery> findByDeliveryIdAndUserId(String deliveryId, Long userId);

    boolean existsByDeliveryId(String deliveryId);

    Page<GithubWebhookDelivery> findByUserId(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, WebhookDeliveryStatus status);

    long countByStatus(WebhookDeliveryStatus status);

    @Query("SELECT d FROM GithubWebhookDelivery d WHERE d.status = 'PROCESSING' AND (d.startedAt < :threshold OR (d.startedAt IS NULL AND d.receivedAt < :threshold))")
    List<GithubWebhookDelivery> findStaleProcessingDeliveries(@Param("threshold") Instant threshold);

    @Query("SELECT d FROM GithubWebhookDelivery d WHERE d.status = 'FAILED' AND d.retryable = true AND d.nextRetryAt <= :now AND d.attemptCount < d.maxAttempts")
    List<GithubWebhookDelivery> findEligibleScheduledRetries(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE GithubWebhookDelivery d SET d.status = 'PROCESSING', d.startedAt = :now, d.attemptCount = d.attemptCount + 1, d.nextRetryAt = null WHERE d.id = :id AND d.attemptCount < d.maxAttempts AND (d.status = 'FAILED' OR (d.status = 'PROCESSING' AND (d.startedAt < :staleThreshold OR (d.startedAt IS NULL AND d.receivedAt < :staleThreshold))))")
    int claimForRetry(@Param("id") Long id, @Param("now") Instant now, @Param("staleThreshold") Instant staleThreshold);
}

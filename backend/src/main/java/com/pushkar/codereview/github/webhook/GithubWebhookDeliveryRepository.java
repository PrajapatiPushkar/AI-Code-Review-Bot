package com.pushkar.codereview.github.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GithubWebhookDeliveryRepository extends JpaRepository<GithubWebhookDelivery, Long> {

    Optional<GithubWebhookDelivery> findByDeliveryId(String deliveryId);

    boolean existsByDeliveryId(String deliveryId);
}

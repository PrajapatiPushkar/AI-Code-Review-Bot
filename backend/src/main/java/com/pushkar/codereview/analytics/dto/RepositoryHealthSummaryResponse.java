package com.pushkar.codereview.analytics.dto;

import java.time.Instant;
import java.util.Objects;

public class RepositoryHealthSummaryResponse {

    private String repository;
    private String owner;
    private String name;
    private long totalReviews;
    private long completedReviews;
    private long failedReviews;
    private long inProgressReviews;
    private long totalFindings;
    private long criticalFindings;
    private long highFindings;
    private long mediumFindings;
    private long lowFindings;
    private long infoFindings;
    private Instant lastReviewAt;
    private String healthStatus;
    private String healthStatusDescription;

    public RepositoryHealthSummaryResponse() {
    }

    public RepositoryHealthSummaryResponse(String repository, String owner, String name,
                                           long totalReviews, long completedReviews, long failedReviews,
                                           long inProgressReviews, long totalFindings,
                                           long criticalFindings, long highFindings, long mediumFindings,
                                           long lowFindings, long infoFindings,
                                           Instant lastReviewAt,
                                           RepositoryHealthStatus healthStatus) {
        this.repository = repository;
        this.owner = owner;
        this.name = name;
        this.totalReviews = totalReviews;
        this.completedReviews = completedReviews;
        this.failedReviews = failedReviews;
        this.inProgressReviews = inProgressReviews;
        this.totalFindings = totalFindings;
        this.criticalFindings = criticalFindings;
        this.highFindings = highFindings;
        this.mediumFindings = mediumFindings;
        this.lowFindings = lowFindings;
        this.infoFindings = infoFindings;
        this.lastReviewAt = lastReviewAt;
        this.healthStatus = healthStatus != null ? healthStatus.name() : RepositoryHealthStatus.NO_REVIEWS.name();
        this.healthStatusDescription = healthStatus != null ? healthStatus.getDescription() : RepositoryHealthStatus.NO_REVIEWS.getDescription();
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public long getCompletedReviews() {
        return completedReviews;
    }

    public void setCompletedReviews(long completedReviews) {
        this.completedReviews = completedReviews;
    }

    public long getFailedReviews() {
        return failedReviews;
    }

    public void setFailedReviews(long failedReviews) {
        this.failedReviews = failedReviews;
    }

    public long getInProgressReviews() {
        return inProgressReviews;
    }

    public void setInProgressReviews(long inProgressReviews) {
        this.inProgressReviews = inProgressReviews;
    }

    public long getTotalFindings() {
        return totalFindings;
    }

    public void setTotalFindings(long totalFindings) {
        this.totalFindings = totalFindings;
    }

    public long getCriticalFindings() {
        return criticalFindings;
    }

    public void setCriticalFindings(long criticalFindings) {
        this.criticalFindings = criticalFindings;
    }

    public long getHighFindings() {
        return highFindings;
    }

    public void setHighFindings(long highFindings) {
        this.highFindings = highFindings;
    }

    public long getMediumFindings() {
        return mediumFindings;
    }

    public void setMediumFindings(long mediumFindings) {
        this.mediumFindings = mediumFindings;
    }

    public long getLowFindings() {
        return lowFindings;
    }

    public void setLowFindings(long lowFindings) {
        this.lowFindings = lowFindings;
    }

    public long getInfoFindings() {
        return infoFindings;
    }

    public void setInfoFindings(long infoFindings) {
        this.infoFindings = infoFindings;
    }

    public Instant getLastReviewAt() {
        return lastReviewAt;
    }

    public void setLastReviewAt(Instant lastReviewAt) {
        this.lastReviewAt = lastReviewAt;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getHealthStatusDescription() {
        return healthStatusDescription;
    }

    public void setHealthStatusDescription(String healthStatusDescription) {
        this.healthStatusDescription = healthStatusDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepositoryHealthSummaryResponse that = (RepositoryHealthSummaryResponse) o;
        return totalReviews == that.totalReviews &&
                completedReviews == that.completedReviews &&
                failedReviews == that.failedReviews &&
                inProgressReviews == that.inProgressReviews &&
                totalFindings == that.totalFindings &&
                criticalFindings == that.criticalFindings &&
                highFindings == that.highFindings &&
                mediumFindings == that.mediumFindings &&
                lowFindings == that.lowFindings &&
                infoFindings == that.infoFindings &&
                Objects.equals(repository, that.repository) &&
                Objects.equals(owner, that.owner) &&
                Objects.equals(name, that.name) &&
                Objects.equals(lastReviewAt, that.lastReviewAt) &&
                Objects.equals(healthStatus, that.healthStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repository, owner, name, totalReviews, completedReviews, failedReviews, inProgressReviews, totalFindings, criticalFindings, highFindings, mediumFindings, lowFindings, infoFindings, lastReviewAt, healthStatus);
    }
}

package com.pushkar.codereview.github;

import com.pushkar.codereview.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "github_installations")
public class GithubInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "github_installation_id", nullable = false, unique = true)
    private Long githubInstallationId;

    @Column(name = "github_account_login", nullable = false)
    private String githubAccountLogin;

    @Column(name = "github_account_type", nullable = false, length = 50)
    private String githubAccountType;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public GithubInstallation() {
    }

    public GithubInstallation(User user, Long githubInstallationId, String githubAccountLogin, String githubAccountType) {
        this.user = user;
        this.githubInstallationId = githubInstallationId;
        this.githubAccountLogin = githubAccountLogin;
        this.githubAccountType = githubAccountType;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getGithubInstallationId() {
        return githubInstallationId;
    }

    public void setGithubInstallationId(Long githubInstallationId) {
        this.githubInstallationId = githubInstallationId;
    }

    public String getGithubAccountLogin() {
        return githubAccountLogin;
    }

    public void setGithubAccountLogin(String githubAccountLogin) {
        this.githubAccountLogin = githubAccountLogin;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getGithubAccountType() {
        return githubAccountType;
    }

    public void setGithubAccountType(String githubAccountType) {
        this.githubAccountType = githubAccountType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GithubInstallation that = (GithubInstallation) o;
        return Objects.equals(id, that.id) || Objects.equals(githubInstallationId, that.githubInstallationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, githubInstallationId);
    }
}

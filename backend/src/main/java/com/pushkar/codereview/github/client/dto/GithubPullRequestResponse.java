package com.pushkar.codereview.github.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class GithubPullRequestResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("number")
    private Long number;

    @JsonProperty("title")
    private String title;

    @JsonProperty("body")
    private String body;

    @JsonProperty("state")
    private String state;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("user")
    private UserResponse user;

    @JsonProperty("head")
    private GitRefResponse head;

    @JsonProperty("base")
    private GitRefResponse base;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    public GithubPullRequestResponse() {
    }

    public GithubPullRequestResponse(Long id, Long number, String title, String body, String state,
                                    String htmlUrl, UserResponse user, GitRefResponse head,
                                    GitRefResponse base, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.number = number;
        this.title = title;
        this.body = body;
        this.state = state;
        this.htmlUrl = htmlUrl;
        this.user = user;
        this.head = head;
        this.base = base;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public GitRefResponse getHead() {
        return head;
    }

    public void setHead(GitRefResponse head) {
        this.head = head;
    }

    public GitRefResponse getBase() {
        return base;
    }

    public void setBase(GitRefResponse base) {
        this.base = base;
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

    public static class UserResponse {

        @JsonProperty("login")
        private String login;

        public UserResponse() {
        }

        public UserResponse(String login) {
            this.login = login;
        }

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }
    }

    public static class GitRefResponse {

        @JsonProperty("ref")
        private String ref;

        @JsonProperty("sha")
        private String sha;

        public GitRefResponse() {
        }

        public GitRefResponse(String ref) {
            this(ref, null);
        }

        public GitRefResponse(String ref, String sha) {
            this.ref = ref;
            this.sha = sha;
        }

        public String getRef() {
            return ref;
        }

        public void setRef(String ref) {
            this.ref = ref;
        }

        public String getSha() {
            return sha;
        }

        public void setSha(String sha) {
            this.sha = sha;
        }
    }
}

package com.pushkar.codereview.github.auth;

import org.springframework.stereotype.Component;

@Component
public class GithubAppAuthenticator {

    private final GithubJwtService githubJwtService;

    public GithubAppAuthenticator(GithubJwtService githubJwtService) {
        this.githubJwtService = githubJwtService;
    }

    public String getAppJwt() {
        return githubJwtService.generateAppJwt();
    }
}

package com.pushkar.codereview.github.mapper;

import com.pushkar.codereview.github.GithubInstallation;
import com.pushkar.codereview.github.dto.GithubInstallationCreateRequest;
import com.pushkar.codereview.github.dto.GithubInstallationResponse;
import com.pushkar.codereview.user.User;
import org.springframework.stereotype.Component;

@Component
public class GithubInstallationMapper {

    public GithubInstallation toEntity(GithubInstallationCreateRequest request, User user) {
        if (request == null) {
            return null;
        }

        return new GithubInstallation(
                user,
                request.getGithubInstallationId(),
                request.getGithubAccountLogin(),
                request.getGithubAccountType()
        );
    }

    public GithubInstallationResponse toResponse(GithubInstallation entity) {
        if (entity == null) {
            return null;
        }

        Long userId = (entity.getUser() != null) ? entity.getUser().getId() : null;

        return new GithubInstallationResponse(
                entity.getId(),
                userId,
                entity.getGithubInstallationId(),
                entity.getGithubAccountLogin(),
                entity.getGithubAccountType(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

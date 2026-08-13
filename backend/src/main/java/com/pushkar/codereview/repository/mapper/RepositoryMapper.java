package com.pushkar.codereview.repository.mapper;

import com.pushkar.codereview.repository.Repository;
import com.pushkar.codereview.repository.dto.RepositoryCreateRequest;
import com.pushkar.codereview.repository.dto.RepositoryResponse;
import com.pushkar.codereview.user.User;
import org.springframework.stereotype.Component;

@Component
public class RepositoryMapper {

    public Repository toEntity(RepositoryCreateRequest request, User user) {
        if (request == null) {
            return null;
        }

        Boolean isActive = request.getIsActive() != null ? request.getIsActive() : true;

        return new Repository(
                user,
                request.getGithubRepositoryId(),
                request.getName(),
                request.getFullName(),
                request.getDefaultBranch(),
                request.getHtmlUrl(),
                isActive
        );
    }

    public RepositoryResponse toResponse(Repository entity) {
        if (entity == null) {
            return null;
        }

        Long userId = (entity.getUser() != null) ? entity.getUser().getId() : null;

        return new RepositoryResponse(
                entity.getId(),
                userId,
                entity.getGithubRepositoryId(),
                entity.getName(),
                entity.getFullName(),
                entity.getDefaultBranch(),
                entity.getHtmlUrl(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

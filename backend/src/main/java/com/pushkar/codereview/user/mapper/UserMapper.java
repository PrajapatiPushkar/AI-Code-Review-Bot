package com.pushkar.codereview.user.mapper;

import com.pushkar.codereview.user.User;
import com.pushkar.codereview.user.dto.UserCreateRequest;
import com.pushkar.codereview.user.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserCreateRequest request) {
        if (request == null) {
            return null;
        }

        String role = (request.getRole() != null && !request.getRole().isBlank()) 
                ? request.getRole() 
                : "DEVELOPER";

        return new User(
                request.getGithubId(),
                request.getUsername(),
                request.getEmail(),
                request.getAvatarUrl(),
                role
        );
    }

    public UserResponse toResponse(User entity) {
        if (entity == null) {
            return null;
        }

        return new UserResponse(
                entity.getId(),
                entity.getGithubId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getAvatarUrl(),
                entity.getRole(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

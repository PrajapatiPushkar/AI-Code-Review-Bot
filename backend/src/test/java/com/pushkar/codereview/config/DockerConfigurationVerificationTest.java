package com.pushkar.codereview.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class DockerConfigurationVerificationTest {

    @Test
    void testDockerComposeConfiguration() throws Exception {
        Path dockerComposePath = findFilePath("docker-compose.yml");
        assertThat(dockerComposePath).isNotNull().exists();

        String content = Files.readString(dockerComposePath);

        // Verify services and architecture
        assertThat(content).contains("services:");
        assertThat(content).contains("postgres:");
        assertThat(content).contains("backend:");
        assertThat(content).contains("postgres-data:");
        assertThat(content).contains("app-network:");

        // Verify database connection host inside Docker network
        assertThat(content).contains("jdbc:postgresql://postgres:5432/");
        assertThat(content).doesNotContain("jdbc:postgresql://localhost:5432/");

        // Verify database healthcheck and dependency condition
        assertThat(content).contains("pg_isready");
        assertThat(content).contains("service_healthy");

        // Verify health check endpoint for backend
        assertThat(content).contains("/api/v1/actuator/health");
    }

    @Test
    void testDockerfileConfiguration() throws Exception {
        Path dockerfilePath = findFilePath("backend/Dockerfile");
        if (dockerfilePath == null || !Files.exists(dockerfilePath)) {
            dockerfilePath = findFilePath("Dockerfile");
        }
        assertThat(dockerfilePath).isNotNull().exists();

        String content = Files.readString(dockerfilePath);

        // Verify multi-stage build
        assertThat(content).contains("FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder");
        assertThat(content).contains("FROM eclipse-temurin:21-jre-alpine");

        // Verify non-root user execution
        assertThat(content).contains("USER appuser");

        // Verify exposed application port
        assertThat(content).contains("EXPOSE 8080");

        // Verify clean ENTRYPOINT execution
        assertThat(content).contains("ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]");
    }

    @Test
    void testDockerIgnoreConfiguration() throws Exception {
        Path dockerIgnorePath = findFilePath("backend/.dockerignore");
        if (dockerIgnorePath == null || !Files.exists(dockerIgnorePath)) {
            dockerIgnorePath = findFilePath(".dockerignore");
        }
        assertThat(dockerIgnorePath).isNotNull().exists();

        String content = Files.readString(dockerIgnorePath);

        // Verify exclusion of sensitive files and build target outputs
        assertThat(content).contains("target/");
        assertThat(content).contains(".git/");
        assertThat(content).contains(".env");
    }

    private Path findFilePath(String relativePath) {
        Path direct = Paths.get(relativePath);
        if (Files.exists(direct)) {
            return direct;
        }
        Path parent = Paths.get("..", relativePath);
        if (Files.exists(parent)) {
            return parent;
        }
        Path userDir = Paths.get(System.getProperty("user.dir"), relativePath);
        if (Files.exists(userDir)) {
            return userDir;
        }
        Path parentUserDir = Paths.get(System.getProperty("user.dir"), "..", relativePath);
        if (Files.exists(parentUserDir)) {
            return parentUserDir;
        }
        return null;
    }
}

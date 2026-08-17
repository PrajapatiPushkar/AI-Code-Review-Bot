package com.pushkar.codereview.config;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class DockerConfigurationVerificationTest {

    @Test
    void testDockerComposeHealthCheckConfiguration() throws Exception {
        File dockerComposeFile = new File("../docker-compose.yml");
        if (!dockerComposeFile.exists()) {
            dockerComposeFile = new File("docker-compose.yml");
        }

        assertThat(dockerComposeFile.exists()).isTrue();
        String content = Files.readString(dockerComposeFile.toPath());

        assertThat(content).contains("healthcheck:");
        assertThat(content).contains("/api/v1/actuator/health");
        assertThat(content).contains("pg_isready");
    }

    @Test
    void testDockerfileExistsAndExposesPort() throws Exception {
        File dockerfile = new File("Dockerfile");
        if (!dockerfile.exists()) {
            dockerfile = new File("backend/Dockerfile");
        }

        assertThat(dockerfile.exists()).isTrue();
        String content = Files.readString(dockerfile.toPath());

        assertThat(content).contains("EXPOSE 8080");
        assertThat(content).contains("app.jar");
    }
}

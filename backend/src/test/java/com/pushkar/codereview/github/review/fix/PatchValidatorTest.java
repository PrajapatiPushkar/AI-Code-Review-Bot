package com.pushkar.codereview.github.review.fix;

import com.pushkar.codereview.exception.PatchValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatchValidatorTest {

    private PatchValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PatchValidator();
    }

    @Test
    void testValidPatchAndPath_PassesValidation() {
        String expectedPath = "src/main/java/com/example/UserService.java";
        String candidatePath = "src/main/java/com/example/UserService.java";
        String validDiff = "--- a/src/main/java/com/example/UserService.java\n" +
                "+++ b/src/main/java/com/example/UserService.java\n" +
                "@@ -10,1 +10,1 @@\n" +
                "-if (user == null)\n" +
                "+if (user == null || user.getId() == null)";

        assertThatCode(() -> validator.validate(expectedPath, candidatePath, validDiff))
                .doesNotThrowAnyException();
    }

    @Test
    void testEmptyFilePath_ThrowsException() {
        assertThatThrownBy(() -> validator.validateFilePath("src/Test.java", ""))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("must not be empty");

        assertThatThrownBy(() -> validator.validateFilePath("src/Test.java", null))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void testPathTraversal_ThrowsException() {
        assertThatThrownBy(() -> validator.validateFilePath("src/Test.java", "../secret.txt"))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("Path traversal detected");

        assertThatThrownBy(() -> validator.validateFilePath("src/Test.java", "src/../../secret.txt"))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("Path traversal detected");

        assertThatThrownBy(() -> validator.validateFilePath("src/Test.java", "..\\windows\\system32"))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("Path traversal detected");
    }

    @Test
    void testAbsolutePath_UnixAndWindows_ThrowsException() {
        assertThatThrownBy(() -> validator.validateFilePath("src/Test.java", "/etc/passwd"))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("Absolute file path is not allowed");

        assertThatThrownBy(() -> validator.validateFilePath("src/Test.java", "C:\\Windows\\System32\\cmd.exe"))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("Absolute file path is not allowed");

        assertThatThrownBy(() -> validator.validateFilePath("src/Test.java", "\\\\remote-server\\share\\file.txt"))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("Absolute file path is not allowed");
    }

    @Test
    void testProhibitedSensitiveFiles_ThrowsException() {
        assertThatThrownBy(() -> validator.validateFilePath(".env", ".env"))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("prohibited sensitive configuration");

        assertThatThrownBy(() -> validator.validateFilePath("config/application.properties", "config/application.properties"))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("prohibited sensitive configuration");

        assertThatThrownBy(() -> validator.validateFilePath("secret.key", "secret.key"))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("prohibited sensitive configuration");

        assertThatThrownBy(() -> validator.validateFilePath("id_rsa", "id_rsa"))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("prohibited sensitive configuration");
    }

    @Test
    void testMismatchedFilePath_ThrowsException() {
        assertThatThrownBy(() -> validator.validateFilePath("src/main/A.java", "src/main/B.java"))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("does not match the target review finding file path");
    }

    @Test
    void testEmptyDiffContent_ThrowsException() {
        assertThatThrownBy(() -> validator.validateDiffContent(""))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("must not be empty");

        assertThatThrownBy(() -> validator.validateDiffContent("   "))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void testDangerousCommands_ThrowsException() {
        String dangerousDiff = "--- a/src/Main.java\n+++ b/src/Main.java\n@@ -1 +1 @@\n+ Runtime.getRuntime().exec(\"rm -rf /\");";
        assertThatThrownBy(() -> validator.validateDiffContent(dangerousDiff))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("malicious or destructive instruction");

        String gitPushDiff = "--- a/src/Main.java\n+++ b/src/Main.java\n@@ -1 +1 @@\n+ git push origin master";
        assertThatThrownBy(() -> validator.validateDiffContent(gitPushDiff))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("malicious or destructive instruction");
    }

    @Test
    void testDiffWithoutDiffMarkers_ThrowsException() {
        String proseOnly = "Here is the code you should change without any diff markers.";
        assertThatThrownBy(() -> validator.validateDiffContent(proseOnly))
                .isInstanceOf(PatchValidationException.class)
                .hasMessageContaining("valid unified diff syntax or markers");
    }
}

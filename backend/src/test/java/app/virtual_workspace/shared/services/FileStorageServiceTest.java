package app.virtual_workspace.shared.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;

public class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("Should store file successfully and return unique filename")
    void storeFile_shouldStoreFileSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "profilePicture",
                "avatar.png",
                "image/png",
                "fake-image-content".getBytes()
        );

        String storedFileName = fileStorageService.storeFile(file);

        assertThat(storedFileName).isNotNull();
        assertThat(storedFileName).contains("avatar.png");
        assertThat(Files.exists(tempDir.resolve(storedFileName))).isTrue();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when file is empty or null")
    void storeFile_shouldThrowException_whenFileIsEmptyOrNull() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "profilePicture",
                "empty.png",
                "image/png",
                new byte[0]
        );

        assertThatThrownBy(() -> fileStorageService.storeFile(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot store empty or null file");

        assertThatThrownBy(() -> fileStorageService.storeFile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot store empty or null file");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when filename contains path traversal")
    void storeFile_shouldThrowException_whenFilenameContainsPathTraversal() {
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "profilePicture",
                "../evil.png",
                "image/png",
                "malicious".getBytes()
        );

        assertThatThrownBy(() -> fileStorageService.storeFile(maliciousFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Filename contains invalid path sequence");
    }

    @Test
    @DisplayName("Should load file as Resource when file exists")
    void loadFileAsResource_shouldReturnResource_whenFileExists() throws IOException {
        Path filePath = tempDir.resolve("test_image.jpg");
        Files.write(filePath, "sample-image".getBytes());

        Resource resource = fileStorageService.loadFileAsResource("test_image.jpg");

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when file does not exist")
    void loadFileAsResource_shouldThrowException_whenFileDoesNotExist() {
        assertThatThrownBy(() -> fileStorageService.loadFileAsResource("nonexistent.png"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("File not found: nonexistent.png");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when path traversal attempt on load")
    void loadFileAsResource_shouldThrowException_whenPathTraversalAttempt() {
        assertThatThrownBy(() -> fileStorageService.loadFileAsResource("../outside.txt"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should delete file successfully when file exists")
    void deleteFile_shouldDeleteFileIfExists() throws IOException {
        Path filePath = tempDir.resolve("to_delete.png");
        Files.write(filePath, "content".getBytes());
        assertThat(Files.exists(filePath)).isTrue();

        fileStorageService.deleteFile("to_delete.png");

        assertThat(Files.exists(filePath)).isFalse();
    }

    @Test
    @DisplayName("Should handle deleteFile gracefully when file is null or blank")
    void deleteFile_shouldHandleNullOrBlank() {
        fileStorageService.deleteFile(null);
        fileStorageService.deleteFile("");
        fileStorageService.deleteFile("   ");
    }
}

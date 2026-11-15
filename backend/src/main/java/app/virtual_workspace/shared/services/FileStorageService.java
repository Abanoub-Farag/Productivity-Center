package app.virtual_workspace.shared.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:./upload}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty or null file");
        }

        String rawFileName = file.getOriginalFilename();
        String originalFileName = rawFileName != null ? StringUtils.cleanPath(rawFileName) : "file";

        if (originalFileName.contains("..")) {
            throw new IllegalArgumentException("Filename contains invalid path sequence: " + originalFileName);
        }

        String uniqueurl = UUID.randomUUID() + "_" + originalFileName;

        try (InputStream inputStream = file.getInputStream()) {
            Path targetLocation = this.fileStorageLocation.resolve(uniqueurl);
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return uniqueurl;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file: " + uniqueurl, ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path file = fileStorageLocation.resolve(fileName).normalize();
            if (!file.startsWith(this.fileStorageLocation)) {
                throw new ResourceNotFoundException("File not found: " + fileName);
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found: " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File path invalid: " + fileName);
        }
    }

    public void deleteFile(String fileName) {
        if (fileName != null && !fileName.isBlank()) {
            try {
                Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
                if (filePath.startsWith(this.fileStorageLocation)) {
                    Files.deleteIfExists(filePath);
                }
            } catch (IOException ignored) {
            }
        }
    }

}

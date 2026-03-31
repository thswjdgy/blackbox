package com.blackbox.domain.vault.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class FileStorageService {

    private final Path rootLocation;

    public FileStorageService(@Value("${file.upload-dir:/data/uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir);
    }

    /**
     * 파일을 /data/uploads/{projectId}/{hash}_{filename} 경로에 저장한다.
     * @return 저장된 상대 경로 문자열
     */
    public String store(MultipartFile file, Long projectId, String sha256Hash) throws IOException {
        String safeFileName = sanitize(file.getOriginalFilename());
        Path dir = rootLocation.resolve(projectId.toString());
        Files.createDirectories(dir);

        String storedName = sha256Hash + "_" + safeFileName;
        Path target = dir.resolve(storedName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        String relativePath = projectId + "/" + storedName;
        log.info("Stored file: {}", relativePath);
        return relativePath;
    }

    /**
     * 저장된 파일을 Resource로 반환 (다운로드용 스트리밍).
     */
    public Resource load(String relativePath) throws IOException {
        Path file = rootLocation.resolve(relativePath);
        if (!Files.exists(file)) {
            throw new IOException("File not found: " + relativePath);
        }
        return new InputStreamResource(Files.newInputStream(file));
    }

    /**
     * 저장된 파일의 InputStream을 반환 (해시 재검증용).
     */
    public InputStream openStream(String relativePath) throws IOException {
        Path file = rootLocation.resolve(relativePath);
        if (!Files.exists(file)) {
            throw new IOException("File not found: " + relativePath);
        }
        return Files.newInputStream(file);
    }

    private String sanitize(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) return "unknown";
        return originalFilename.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}

package com.example.forum.service.storage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.forum.dto.FileRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalStorageService implements FileStorageServiceImpl {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.access-url-prefix:http://localhost:8080/uploads}")
    private String accessUrlPrefix;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String generateStoredName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String datePath = LocalDate.now().format(DATE_FORMATTER);
        String savedFilename = UUID.randomUUID().toString() + extension;
        return datePath + "/" + savedFilename;
    }

    @Override
    public String getAccessUrl(String storedName) {
        String normalizedPath = storedName.replace('\\', '/');
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        return accessUrlPrefix + "/" + normalizedPath;
    }

    @Override
    public void savePhysicalFile(MultipartFile file, String storedName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        Path targetPath = Paths.get(uploadDir, storedName).toAbsolutePath().normalize();
        Path parentDir = targetPath.getParent();

        try {
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            file.transferTo(targetPath.toFile());
            log.info("물리 파일 저장 완료: {}", targetPath);
        } catch (IOException e) {
            // 동시성으로 인해 상위 폴더가 삭제되었을 경우 1회 재생성 후 재시도
            try {
                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                    file.transferTo(targetPath.toFile());
                    log.info("물리 파일 재시도 저장 완료: {}", targetPath);
                    return;
                }
            } catch (IOException retryEx) {
                log.error("물리 파일 재시도 저장 실패: {}", retryEx.getMessage());
            }

            log.error("물리 파일 저장 실패: {}", e.getMessage());
            throw new RuntimeException("파일 물리 저장 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public FileRequestDto storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        String storedName = generateStoredName(originalFilename);
        savePhysicalFile(file, storedName);
        String accessUrl = getAccessUrl(storedName);

        return FileRequestDto.builder()
                .originalName(originalFilename)
                .storedName(storedName)
                .accessUrl(accessUrl)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();
    }

    @Override
    public void deleteFile(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }

        // URL 형태로 넘어온 경우 상대 경로만 추출
        String relativePath = storedName;
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            if (relativePath.contains(accessUrlPrefix)) {
                relativePath = relativePath.substring(relativePath.indexOf(accessUrlPrefix) + accessUrlPrefix.length());
            } else {
                int slashIndex = relativePath.lastIndexOf("/uploads/");
                if (slashIndex != -1) {
                    relativePath = relativePath.substring(slashIndex + "/uploads/".length());
                }
            }
        }
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        Path filePath = Paths.get(uploadDir, relativePath).toAbsolutePath().normalize();
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("물리 파일 삭제 완료: {}", filePath);
                deleteEmptyParentDirectories(filePath);
            } else {
                log.warn("삭제할 물리 파일이 존재하지 않습니다: {}", filePath);
            }
        } catch (java.nio.file.NoSuchFileException e) {
            log.info("동시성: 이미 다른 스레드에 의해 삭제된 파일입니다: {}", filePath);
        } catch (IOException e) {
            log.error("물리 파일 삭제 실패: {} | 원인: {}", filePath, e.getMessage());
            throw new RuntimeException("물리 파일 삭제 실패: " + storedName, e);
        }
    }

    /**
     * 파일 삭제 후 남은 빈 상위 디렉터리(유령 폴더)들을 uploadDir 전까지 순차적으로 삭제
     * 동시성 고려: DirectoryNotEmptyException, NoSuchFileException 등 안전하게 격리
     */
    private void deleteEmptyParentDirectories(Path filePath) {
        try {
            Path rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path parent = filePath.getParent();

            while (parent != null && !parent.equals(rootDir) && parent.startsWith(rootDir)) {
                if (Files.exists(parent) && Files.isDirectory(parent)) {
                    try (var stream = Files.list(parent)) {
                        if (stream.findAny().isEmpty()) {
                            try {
                                Files.delete(parent);
                                log.info("빈 유령 폴더 삭제 완료: {}", parent);
                                parent = parent.getParent();
                            } catch (DirectoryNotEmptyException e) {
                                // 다른 스레드에서 동시에 파일을 저장함 -> 삭제 중단
                                log.debug("동시성: 폴더 내 파일이 생성되어 삭제를 중단합니다: {}", parent);
                                break;
                            } catch (NoSuchFileException e) {
                                // 이미 다른 스레드가 삭제함
                                break;
                            } catch (FileSystemException e) {
                                log.warn("파일 시스템 락으로 인해 폴더 삭제를 건너뜁니다: {}", parent);
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("빈 상위 폴더 정리 중 오류 발생: {}", e.getMessage());
        }
    }

    @Override
    public Resource loadFileAsResource(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            throw new IllegalArgumentException("파일명이 올바르지 않습니다.");
        }

        String relativePath = storedName.replace('\\', '/');
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        try {
            Path filePath = Paths.get(uploadDir, relativePath).toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("파일을 찾을 수 없거나 읽을 수 없습니다: " + storedName);
            }
        } catch (MalformedURLException e) {
            log.error("파일 리소스 로드 실패: {}", e.getMessage());
            throw new RuntimeException("파일 경로 오류: " + storedName, e);
        }
    }

    @Override
    public void cleanupEmptyDirectories() {
        try {
            Path rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(rootDir)) {
                return;
            }

            try (var pathStream = Files.walk(rootDir)) {
                pathStream
                        .filter(Files::isDirectory)
                        .filter(path -> !path.equals(rootDir))
                        .sorted((p1, p2) -> p2.compareTo(p1))
                        .forEach(dir -> {
                            try (var childStream = Files.list(dir)) {
                                if (childStream.findAny().isEmpty()) {
                                    try {
                                        Files.delete(dir);
                                        log.info("스케줄러: 빈 유령 폴더 정리 완료: {}", dir);
                                    } catch (java.nio.file.DirectoryNotEmptyException e) {
                                        log.debug("스케줄러 동시성: 폴더 내 파일 생성 감지로 삭제 건너뜀: {}", dir);
                                    } catch (java.nio.file.NoSuchFileException ignored) {
                                    }
                                }
                            } catch (IOException e) {
                                log.warn("빈 폴더 확인/삭제 실패: {}", dir, e);
                            }
                        });
            }
        } catch (Exception e) {
            log.warn("유령 폴더 일괄 정리 중 오류 발생: {}", e.getMessage());
        }
    }
}

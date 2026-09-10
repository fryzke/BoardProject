package com.example.forum.service.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.forum.dto.FileRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.net.MalformedURLException;

@Service
@RequiredArgsConstructor 
@Slf4j
public class LocalStorageService implements FileStorageServiceImpl {
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.access-url-prefix:http://localhost:8080/uploads}")
    private String accessUrlPrefix;

    @Override
    public String getAccessUrl(String storedName) {
        return accessUrlPrefix + "/" + storedName;
    }

    @Override
    public void savePhysicalFile(MultipartFile file, String storedName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        try {
            File folder = new File(uploadDir);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File dest = new File(folder.getAbsolutePath() + File.separator + storedName);
            file.transferTo(dest);
            log.info("물리 파일 저장 완료: {}", dest.getAbsolutePath());
        } catch (IOException e) {
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
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String savedFilename = UUID.randomUUID().toString() + extension;
        savePhysicalFile(file, savedFilename);
        String accessUrl = getAccessUrl(savedFilename);

        return FileRequestDto.builder()
                .originalName(originalFilename)
                .storedName(savedFilename)
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

        Path filePath = Paths.get(uploadDir, storedName).toAbsolutePath().normalize();
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("물리 파일 삭제 완료: {}", filePath);
            } else {
                log.warn("삭제할 물리 파일이 존재하지 않습니다: {}", filePath);
            }
        } catch (IOException e) {
            log.error("물리 파일 삭제 실패: {} | 원인: {}", filePath, e.getMessage());
            throw new RuntimeException("물리 파일 삭제 실패: " + storedName, e);
        }
    }

    @Override
    public Resource loadFileAsResource(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            throw new IllegalArgumentException("파일명이 올바르지 않습니다.");
        }

        try {
            Path filePath = Paths.get(uploadDir, storedName).toAbsolutePath().normalize();
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
}

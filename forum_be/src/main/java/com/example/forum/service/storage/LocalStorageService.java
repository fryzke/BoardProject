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

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LocalStorageService implements FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.access-url-prefix:http://localhost:8080/uploads}")
    private String accessUrlPrefix;

    @Override
    public FileRequestDto storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        try {
            File folder = new File(uploadDir);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String savedFilename = UUID.randomUUID().toString() + extension;
            File dest = new File(folder.getAbsolutePath() + File.separator + savedFilename);
            file.transferTo(dest);

            String accessUrl = accessUrlPrefix + "/" + savedFilename;

            return FileRequestDto.builder()
                    .originalName(originalFilename)
                    .storedName(savedFilename)
                    .accessUrl(accessUrl)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .build();
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", e.getMessage());
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
        }
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
}

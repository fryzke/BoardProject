package com.example.forum.validator;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.forum.domain.File;
import com.example.forum.repository.FileRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FileValidator {
    @Value("${file.max-count:10}")
    private int maxFiles;

    @Value("${file.max-single-size:20971520}")
    private Long maxSize;

    @Value("${file.max-total-size:104857600}")
    private Long totalMaxSize;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "doc", "txt", "xlsx", "pptx", "zip");

    private final FileRepository fileRepository;

    public void validateLogin(String loginId) {
        if (loginId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
    }

    public void validateAuthor(String loginId, File file) {
        if (!loginId.equals(file.getAuthor().getUserId())) {
            throw new IllegalArgumentException("본인이 업로드한 파일만 수정/삭제할 수 있습니다.");
        }
    }

    public void validateSingleFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("개별 파일 허용 용량을 초과하였습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("올바른 파일 확장자가 필요합니다.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 확장자입니다: " + extension);
        }
    }

    public void validateFilesCountAndSize(List<MultipartFile> files, Long postId) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        long newFilesCount = files.size();
        long newFilesTotalSize = files.stream().mapToLong(MultipartFile::getSize).sum();

        long existingCount = 0;
        long existingTotalSize = 0;

        if (postId != null) {
            existingCount = fileRepository.countByPostId(postId);
            Long sumSize = fileRepository.sumTotalSizeByPostId(postId);
            existingTotalSize = (sumSize != null) ? sumSize : 0L;
        }

        if (existingCount + newFilesCount > maxFiles) {
            throw new IllegalArgumentException("파일은 최대 " + maxFiles + "개까지만 업로드할 수 있습니다.");
        }

        if (existingTotalSize + newFilesTotalSize > totalMaxSize) {
            throw new IllegalArgumentException("전체 파일 총용량이 제한을 초과하였습니다.");
        }
    }
}

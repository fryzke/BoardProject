package com.example.forum.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.forum.domain.File;
import com.example.forum.domain.Post;
import com.example.forum.domain.User;
import com.example.forum.dto.FileRequestDto;
import com.example.forum.dto.FileResponseDto;
import com.example.forum.dto.PostDto;
import com.example.forum.event.FileDeleteEvent;
import com.example.forum.repository.FileRepository;
import com.example.forum.repository.PostRepository;
import com.example.forum.repository.UserRepository;
import com.example.forum.validator.FileValidator;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import com.example.forum.service.storage.FileStorageServiceImpl;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FileService {
    private final PostRepository postRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FileValidator fileValidator;
    private final FileStorageServiceImpl fileStorageService;

    public FileResponseDto uploadFile(MultipartFile file, Long postId, String loginUserId) {
        List<FileResponseDto> result = uploadFiles(List.of(file), postId, loginUserId);
        return result.get(0);
    }

    public List<FileResponseDto> uploadFiles(List<MultipartFile> files, Long postId, String loginUserId) {
        fileValidator.validateLogin(loginUserId);

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        for (MultipartFile file : files) {
            fileValidator.validateSingleFile(file);
        }

        Post post = null;
        if (postId != null) {
            post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        }
        User user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        List<FileResponseDto> responseList = new ArrayList<>();
        List<Runnable> physicalSaveTasks = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String storedName = UUID.randomUUID().toString() + extension;
            String accessUrl = fileStorageService.getAccessUrl(storedName);

            File fileEntity = File.builder()
                    .originalName(originalFilename)
                    .storedName(storedName)
                    .accessUrl(accessUrl)
                    .post(post)
                    .author(user)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .build();

            fileRepository.save(fileEntity);
            responseList.add(new FileResponseDto(fileEntity));

            physicalSaveTasks.add(() -> fileStorageService.savePhysicalFile(file, storedName));
        }

        fileValidator.validateFilesCountAndSize(postId, user);

        for (Runnable task : physicalSaveTasks) {
            task.run();
        }

        return responseList;
    }

    // 파일 단건 조회
    @Transactional(readOnly = true)
    public FileResponseDto getFile(Long id) {
        File file = fileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 파일입니다."));

        return new FileResponseDto(file);
    }

    // 게시글에 연관된 파일 목록 조회
    @Transactional(readOnly = true)
    public List<FileResponseDto> getFiles(Long postId) {
        List<File> files = fileRepository.findAllByPostId(postId);
        List<FileResponseDto> result = new ArrayList<>();

        for (File file : files) {
            result.add(new FileResponseDto(file));
        }

        return result;
    }

    // 파일 삭제 (Soft Delete + 물리 파일 삭제 이벤트 발행)
    public void deleteFile(Long id, String loginId) {
        fileValidator.validateLogin(loginId);

        File file = fileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 파일입니다."));

        fileValidator.validateAuthor(loginId, file);

        fileRepository.delete(file);
        eventPublisher.publishEvent(new FileDeleteEvent(file.getStoredName()));
    }

    // 파일 다중 삭제 (Soft Delete + 물리 파일 비동기 삭제 이벤트 발행)
    public void deleteFilesBatch(List<Long> fileIds, String loginId) {
        fileValidator.validateLogin(loginId);

        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        List<File> files = fileRepository.findAllById(fileIds);
        for (File file : files) {
            fileValidator.validateAuthor(loginId, file);
            fileRepository.delete(file);
            eventPublisher.publishEvent(new FileDeleteEvent(file.getStoredName()));
        }
    }

    // 스케줄러/시스템 전용 파일 삭제 (권한 검증 없이 삭제 + 이벤트 발행)
    public void deleteFileBySystem(File file) {
        fileRepository.delete(file);
        eventPublisher.publishEvent(new FileDeleteEvent(file.getStoredName()));
    }

    // postId 가 미연결된 파일 삭제
    public void deleteUnlinkedFile(Post post, PostDto dto) {
        List<File> unlinkedFiles = fileRepository.findAllByAuthorAndPostIsNull(post.getAuthor());
        for (File file : unlinkedFiles) {
            if (dto.getContent().contains(file.getAccessUrl())) {
                file.setPost(post);
            }
        }
    }

    // 파일 메타데이터 수정
    public FileResponseDto editFile(FileRequestDto dto, Long id, String loginId) {
        fileValidator.validateLogin(loginId);

        File file = fileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 파일입니다."));

        fileValidator.validateAuthor(loginId, file);
        file.update(dto);
        return new FileResponseDto(file);
    }

    // 파일 교체 (메타데이터 수정 -> 2차 검증 -> 물리 파일 저장 및 이전 파일 삭제)
    public FileResponseDto replaceFile(MultipartFile file, Long fileId, String loginId) {
        fileValidator.validateLogin(loginId);
        fileValidator.validateSingleFile(file);

        File fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 파일입니다."));

        fileValidator.validateAuthor(loginId, fileEntity);

        String oldStoredName = fileEntity.getStoredName();
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newStoredName = UUID.randomUUID().toString() + extension;
        String newAccessUrl = fileStorageService.getAccessUrl(newStoredName);

        // 1) 메타데이터 변경 및 저장
        FileRequestDto updateDto = FileRequestDto.builder()
                .originalName(originalFilename)
                .storedName(newStoredName)
                .accessUrl(newAccessUrl)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();
        fileEntity.update(updateDto);

        // 2) 백엔드 2차 검증
        fileValidator.validateFilesCountAndSize(fileEntity.getPost() != null ? fileEntity.getPost().getId() : null, fileEntity.getAuthor());

        // 3) 물리 파일 저장 및 이전 물리 파일 비동기 삭제 이벤트 발행
        fileStorageService.savePhysicalFile(file, newStoredName);
        eventPublisher.publishEvent(new FileDeleteEvent(oldStoredName));

        return new FileResponseDto(fileEntity);
    }

    public record FileDownloadDto(org.springframework.core.io.Resource resource, String originalName, String contentType) {}

    // 파일 다운로드 (ID 기준)
    @Transactional(readOnly = true)
    public FileDownloadDto downloadFile(Long id) {
        File file = fileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 파일입니다."));
        org.springframework.core.io.Resource resource = fileStorageService.loadFileAsResource(file.getStoredName());
        return new FileDownloadDto(resource, file.getOriginalName(), file.getContentType());
    }

    // 파일 다운로드 (저장된 파일명 기준)
    @Transactional(readOnly = true)
    public FileDownloadDto downloadFileByStoredName(String storedName) {
        File file = fileRepository.findByStoredName(storedName).orElse(null);
        org.springframework.core.io.Resource resource = fileStorageService.loadFileAsResource(storedName);
        String originalName = file != null ? file.getOriginalName() : storedName;
        String contentType = file != null ? file.getContentType() : "application/octet-stream";
        return new FileDownloadDto(resource, originalName, contentType);
    }
}

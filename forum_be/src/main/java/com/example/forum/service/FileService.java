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
import com.example.forum.event.FileDeleteEvent;
import com.example.forum.repository.FileRepository;
import com.example.forum.repository.PostRepository;
import com.example.forum.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FileService {
    private final PostRepository postRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 파일 메타데이터 저장
    public FileResponseDto createFile(FileRequestDto dto, Long postId, String loginUserId) {
        Post post = null;
        if (postId != null) {
            post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        }
        User user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        File file = File.builder()
                .originalName(dto.getOriginalName())
                .storedName(dto.getStoredName())
                .accessUrl(dto.getAccessUrl())
                .post(post)
                .author(user)
                .fileSize(dto.getFileSize())
                .contentType(dto.getContentType())
                .build();

        fileRepository.save(file);
        return new FileResponseDto(file);
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
        if (loginId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        File file = fileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 파일입니다."));

        if (!file.getAuthor().getUserId().equals(loginId)) {
            throw new IllegalArgumentException("본인이 업로드한 파일만 삭제할 수 있습니다.");
        }

        fileRepository.delete(file);
        eventPublisher.publishEvent(new FileDeleteEvent(file.getStoredName()));
    }

    // 파일 메타데이터 수정
    public FileResponseDto editFile(FileRequestDto dto, Long id, String loginId) {
        if (loginId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        File file = fileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 파일입니다."));

        if (!loginId.equals(file.getAuthor().getUserId())) {
            throw new IllegalArgumentException("본인이 업로드한 파일만 수정할 수 있습니다.");
        }
        file.update(dto);
        return new FileResponseDto(file);
    }
}

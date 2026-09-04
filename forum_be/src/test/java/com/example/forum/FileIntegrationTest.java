package com.example.forum;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import com.example.forum.domain.Category;
import com.example.forum.domain.Grade;
import com.example.forum.domain.Role;
import com.example.forum.domain.User;
import com.example.forum.dto.FileRequestDto;
import com.example.forum.dto.FileResponseDto;
import com.example.forum.dto.PostDto;
import com.example.forum.repository.FileRepository;
import com.example.forum.repository.UserRepository;
import com.example.forum.service.FileService;
import com.example.forum.service.PostService;
import com.example.forum.service.storage.FileStorageService;

@SpringBootTest
class FileIntegrationTest {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository;

    private User getOrCreateTestUser(String userId) {
        return userRepository.findByUserId(userId).orElseGet(() -> {
            User user = User.builder()
                    .userId(userId)
                    .userPassword("password123")
                    .userName("File Tester")
                    .role(Role.USER)
                    .grade(Grade.BRONZE)
                    .build();
            return userRepository.save(user);
        });
    }

    @Test
    @DisplayName("통합 DTO를 사용한 다양한 파일(이미지 및 PDF) 업로드 및 조회 검증")
    void testFileUploadAndGet() {
        String testUserId = "fileUser1";
        getOrCreateTestUser(testUserId);

        // 1. 이미지 파일 업로드 시뮬레이션
        MockMultipartFile imageMultipart = new MockMultipartFile(
                "file",
                "sample.png",
                "image/png",
                "fake image content".getBytes());
        FileRequestDto imageDto = fileStorageService.storeFile(imageMultipart);
        FileResponseDto savedImage = fileService.createFile(imageDto, null, testUserId);

        assertThat(savedImage.getId()).isNotNull();
        assertThat(savedImage.getOriginalName()).isEqualTo("sample.png");
        assertThat(savedImage.getContentType()).isEqualTo("image/png");

        // 2. 일반 문서(PDF) 파일 업로드 시뮬레이션
        MockMultipartFile pdfMultipart = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "fake pdf content".getBytes());
        FileRequestDto pdfDto = fileStorageService.storeFile(pdfMultipart);
        FileResponseDto savedPdf = fileService.createFile(pdfDto, null, testUserId);

        assertThat(savedPdf.getId()).isNotNull();
        assertThat(savedPdf.getOriginalName()).isEqualTo("document.pdf");
        assertThat(savedPdf.getContentType()).isEqualTo("application/pdf");

        // 3. 단건 조회 검증
        FileResponseDto fetchedPdf = fileService.getFile(savedPdf.getId());
        assertThat(fetchedPdf.getOriginalName()).isEqualTo("document.pdf");

        // 뒷정리
        fileStorageService.deleteFile(imageDto.getStoredName());
        fileStorageService.deleteFile(pdfDto.getStoredName());
    }

    @Test
    @DisplayName("게시글 삭제 시 Soft Delete 및 연계된 물리 파일 비동기 삭제 검증")
    void testPostDeleteAndPhysicalFileDeletion() throws Exception {
        String testUserId = "fileUser2";
        getOrCreateTestUser(testUserId);

        // 1. 물리 파일 2개 생성 (이미지 1개, ZIP 1개)
        MockMultipartFile file1 = new MockMultipartFile("file", "image1.jpg", "image/jpeg", "image byte".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "archive.zip", "application/zip", "zip byte".getBytes());

        FileRequestDto dto1 = fileStorageService.storeFile(file1);
        FileRequestDto dto2 = fileStorageService.storeFile(file2);

        // 물리 파일이 실제로 디스크에 존재하는지 확인
        Path path1 = Path.of("uploads", dto1.getStoredName());
        Path path2 = Path.of("uploads", dto2.getStoredName());
        assertThat(Files.exists(path1)).isTrue();
        assertThat(Files.exists(path2)).isTrue();

        // 2. 미연결 파일 DB 저장
        fileService.createFile(dto1, null, testUserId);
        fileService.createFile(dto2, null, testUserId);

        // 3. 게시글 작성 (본문에 파일 URL 포함시켜 자동 연결)
        PostDto postDto = new PostDto();
        postDto.setTitle("첨부파일 테스트 게시글");
        postDto.setContent("내용입니다. " + dto1.getAccessUrl() + " 그리고 " + dto2.getAccessUrl());
        postDto.setCategory(Category.TALK);

        var createdPost = postService.createPost(postDto, testUserId);
        Long postId = createdPost.getId();

        // 게시글에 파일 2개가 연결되었는지 확인
        List<FileResponseDto> linkedFiles = fileService.getFiles(postId);
        assertThat(linkedFiles).hasSize(2);

        // 4. 게시글 삭제 수행
        postService.deletePost(postId, testUserId);

        // 5. DB에서 Soft Delete 확인 (is_deleted = true 이므로 @SQLRestriction에 의해 findAllByPostId는 빈 목록)
        List<FileResponseDto> afterDeleteFiles = fileService.getFiles(postId);
        assertThat(afterDeleteFiles).isEmpty();

        // 6. 비동기 이벤트(@Async) 처리를 잠시 대기 (최대 1초)
        Thread.sleep(1000);

        // 7. 스토리지의 물리 파일이 삭제되었는지 검증
        assertThat(Files.exists(path1)).isFalse();
        assertThat(Files.exists(path2)).isFalse();
    }
}

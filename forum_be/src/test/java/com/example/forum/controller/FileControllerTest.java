package com.example.forum.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.forum.annotation.WithCustomMockUser;
import com.example.forum.config.SecurityConfig;
import com.example.forum.domain.File;
import com.example.forum.dto.FileDownloadDto;
import com.example.forum.dto.FileResponseDto;
import com.example.forum.security.JwtAuthenticationFilter;
import com.example.forum.security.JwtProvider;
import com.example.forum.service.FileService;
import com.example.forum.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithCustomMockUser
@Import(SecurityConfig.class)
public class FileControllerTest {
        @Autowired
        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        private FileService fileService;
        @MockitoBean
        private RateLimitService rateLimitService;
        @MockitoBean
        JwtAuthenticationFilter jwtAuthenticationFilter;
        @MockitoBean
        private JwtProvider jwtProvider;
        @MockitoBean
        private StringRedisTemplate stringRedisTemplate;

        /* 파일/이미지 통합 업로드 API /api/files/upload */
        @DisplayName("파일/이미지 업로드 성공 : 단일 파일")
        @Test
        void uploadFileSuccess() throws Exception {
                when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

                File file = File.builder()
                                .originalName("image1.jpg")
                                .storedName("580200db-5e55-49ab-a222-f64accbc39ea.jpg")
                                .accessUrl("http://localhost:8080/uploads/580200db-5e55-49ab-a222-f64accbc39ea.jpg")
                                .fileSize(10L)
                                .contentType("image/jpeg")
                                .build();
                FileResponseDto response = new FileResponseDto(file);

                when(fileService.uploadFile(any(), anyLong(), anyString())).thenReturn(response);

                MockMultipartFile mockFile = new MockMultipartFile(
                                "file",
                                "image1.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "test image content".getBytes());

                mockMvc.perform(multipart("/api/files/upload")
                                .file(mockFile)
                                .param("postId", "1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.url")
                                                .value("http://localhost:8080/uploads/580200db-5e55-49ab-a222-f64accbc39ea.jpg"))
                                .andExpect(jsonPath("$.data.originalName").value("image1.jpg"))
                                .andExpect(jsonPath("$.data.accessUrl")
                                                .value("http://localhost:8080/uploads/580200db-5e55-49ab-a222-f64accbc39ea.jpg"))
                                .andExpect(jsonPath("$.message").value("파일이 성공적으로 업로드되었습니다."));
        }

        @DisplayName("파일/이미지 업로드 성공 : 여러 파일")
        @Test
        void uploadFilesSuccess() throws Exception {
                when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

                File file1 = File.builder()
                                .originalName("image1.jpg")
                                .storedName("580200db-5e55-49ab-a222-f64accbc39ea.jpg")
                                .accessUrl("http://localhost:8080/uploads/580200db-5e55-49ab-a222-f64accbc39ea.jpg")
                                .fileSize(10L)
                                .contentType("image/jpeg")
                                .build();
                File file2 = File.builder()
                                .originalName("image2.jpg")
                                .storedName("580200db-5e55-49ab-a222-f64accbc39ea.jpg")
                                .accessUrl("http://localhost:8080/uploads/580200db-5e55-49ab-a222-f64accbc39ea.jpg")
                                .fileSize(10L)
                                .contentType("image/jpeg")
                                .build();
                List<FileResponseDto> response = List.of(
                                new FileResponseDto(file1),
                                new FileResponseDto(file2));

                when(fileService.uploadFiles(any(), anyLong(), anyString())).thenReturn(response);

                MockMultipartFile mockFile1 = new MockMultipartFile(
                                "files",
                                "image1.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "test image content".getBytes());
                MockMultipartFile mockFile2 = new MockMultipartFile(
                                "files",
                                "image2.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "test image content".getBytes());

                mockMvc.perform(multipart("/api/files/upload")
                                .file(mockFile1)
                                .file(mockFile2)
                                .param("postId", "1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(2))
                                .andExpect(jsonPath("$.data.[0].originalName").value("image1.jpg"))
                                .andExpect(jsonPath("$.data.[0].accessUrl")
                                                .value("http://localhost:8080/uploads/580200db-5e55-49ab-a222-f64accbc39ea.jpg"))
                                .andExpect(jsonPath("$.message").value("파일들이 성공적으로 업로드되었습니다."));
        }

        /* 게시글에 첨부된 파일 목록 조회 API /api/files/{postId} */
        @DisplayName("게시글에 첨부된 파일 목록 조회 성공")
        @Test
        void getFilesSuccess() throws Exception {
                when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

                File file1 = File.builder()
                                .originalName("image1.jpg")
                                .storedName("580200db-5e55-49ab-a222-f64accbc39ea.jpg")
                                .accessUrl("http://localhost:8080/uploads/580200db-5e55-49ab-a222-f64accbc39ea.jpg")
                                .fileSize(10L)
                                .contentType("image/jpeg")
                                .build();
                File file2 = File.builder()
                                .originalName("image2.jpg")
                                .storedName("580200db-5e55-49ab-a222-f64accbc39ea.jpg")
                                .accessUrl("http://localhost:8080/uploads/580200db-5e55-49ab-a222-f64accbc39ea.jpg")
                                .fileSize(10L)
                                .contentType("image/jpeg")
                                .build();
                List<FileResponseDto> response = List.of(
                                new FileResponseDto(file1),
                                new FileResponseDto(file2));

                when(fileService.getFiles(anyLong())).thenReturn(response);

                mockMvc.perform(get("/api/files/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(2))
                                .andExpect(jsonPath("$.data.[0].originalName").value("image1.jpg"))
                                .andExpect(jsonPath("$.data.[0].accessUrl")
                                                .value("http://localhost:8080/uploads/580200db-5e55-49ab-a222-f64accbc39ea.jpg"))
                                .andExpect(jsonPath("$.message").value("파일 목록을 성공적으로 조회하였습니다."));
        }

        /* 파일 수정 API /api/files/{fileId} */
        @DisplayName("파일 수정 성공")
        @Test
        void updateFileSuccess() throws Exception {
                when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);
                MockMultipartFile mockFile = new MockMultipartFile(
                                "file",
                                "image1.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "test image content".getBytes());
                File file = File.builder()
                                .originalName("image1.jpg")
                                .storedName("580200db-5e55-49ab-a222-f64accbc39ea.jpg")
                                .accessUrl("http://localhost:8080/uploads/580200db-5e55-49ab-a222-f64accbc39ea.jpg")
                                .fileSize(10L)
                                .contentType("image/jpeg")
                                .build();
                FileResponseDto response = new FileResponseDto(file);

                when(fileService.replaceFile(any(), anyLong(), anyString())).thenReturn(response);

                mockMvc.perform(multipart(HttpMethod.PUT, "/api/files/1")
                                .file(mockFile))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.originalName").value("image1.jpg"))
                                .andExpect(jsonPath("$.data.accessUrl")
                                                .value("http://localhost:8080/uploads/580200db-5e55-49ab-a222-f64accbc39ea.jpg"))
                                .andExpect(jsonPath("$.message").value("파일을 성공적으로 수정하였습니다."));
        }

        /* 파일 삭제 API /api/files/{fileId} */
        @DisplayName("파일 삭제 성공")
        @Test
        void deleteFileSuccess() throws Exception {
                when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

                mockMvc.perform(delete("/api/files/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("파일을 성공적으로 삭제하였습니다."));
        }

        /* 파일 다중 삭제 API /api/files/delete-batch */
        @DisplayName("파일 다중 삭제 성공")
        @Test
        void deleteBatchFilesSuccess() throws Exception {
                when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

                List<Long> fileIdList = new ArrayList<>(List.of(1L, 2L, 3L));

                mockMvc.perform(post("/api/files/delete-batch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(fileIdList)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("선택한 파일들이 성공적으로 삭제되었습니다."));
        }

        /* 파일 다운로드 API /api/files/download/{fileId} */
        @DisplayName("파일 다운로드 성공")
        @Test
        void downloadFileSuccess() throws Exception {
                when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

                Resource resource = new UrlResource("https://example.com");
                FileDownloadDto response = new FileDownloadDto(resource, "image1.jpg", "image/jpeg");

                when(fileService.downloadFile(anyLong())).thenReturn(response);

                mockMvc.perform(get("/api/files/download/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType("image/jpeg"))
                                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"image1.jpg\"; filename*=UTF-8''image1.jpg"));

        }

        /* 파일 다운로드 API /api/files/download */
        @DisplayName("파일 다운로드 성공 : storedName 기준")
        @Test
        void downloadFileByStoredNameSuccess() throws Exception {
                when(rateLimitService.isAllowed(any(), anyLong(), anyLong(), anyLong())).thenReturn(true);

                Resource resource = new UrlResource("https://example.com");
                FileDownloadDto response = new FileDownloadDto(resource, "image1.jpg", "image/jpeg");

                when(fileService.downloadFileByStoredName(anyString())).thenReturn(response);

                mockMvc.perform(get("/api/files/download")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("storedName", "580200db-5e55-49ab-a222-f64accbc39ea.jpg"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType("image/jpeg"))
                                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"image1.jpg\"; filename*=UTF-8''image1.jpg"));

        }
}

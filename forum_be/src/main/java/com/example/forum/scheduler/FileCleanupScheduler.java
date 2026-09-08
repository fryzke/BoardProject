package com.example.forum.scheduler;

import com.example.forum.domain.File;
import com.example.forum.repository.FileRepository;
import com.example.forum.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileCleanupScheduler {

    private final FileRepository fileRepository;
    private final FileService fileService;

    // 매일 새벽 3시에 실행 (Cron: 초 분 시 일 월 요일)
    @Scheduled(cron = "${schedule.cron}")
    @Transactional
    public void cleanupOrphanFiles() {
        // 24시간 이상 지난 미연결(postId == null) 파일 대상
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<File> orphanFiles = fileRepository.findAllByPostIsNullAndCreatedAtBefore(threshold);

        if (orphanFiles.isEmpty()) {
            return;
        }

        log.info("=== 미연결 유령 파일 정리 시작: 총 {}개 ===", orphanFiles.size());

        for (File file : orphanFiles) {
            try {
               fileService.deleteFileBySystem(file);
            } catch (Exception e) {
                log.error("유령 파일 삭제 중 오류 발생 [fileId={}]: {}", file.getId(), e.getMessage());
            }
        }

        log.info("=== 미연결 유령 파일 정리 완료 ===");
    }
}
package com.example.forum.event;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.forum.service.storage.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileDeleteEventListener {

    private final FileStorageService fileStorageService;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
        retryFor = { Exception.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void handleFileDelete(FileDeleteEvent event) {
        log.info("▶ [이벤트 수신] 물리 파일 비동기 삭제 시도: {}", event.storedName());
        fileStorageService.deleteFile(event.storedName());
        log.info("✔ [삭제 완료] 물리 파일 삭제 성공: {}", event.storedName());
    }

    @Recover
    public void recover(Exception e, FileDeleteEvent event) {
        log.error("❌ [최종 실패] 파일 삭제 3회 재시도 실패: {} | 원인: {}", event.storedName(), e.getMessage());
    }
}

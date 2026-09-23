package com.example.forum.scheduler;

import com.example.forum.domain.User;
import com.example.forum.repository.UserRepository;

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
public class UserCleanupScheduler {

    private final UserRepository userRepository;

    // 매일 새벽 3시에 실행 (Cron: 초 분 시 일 월 요일)
    @Scheduled(cron = "${schedule.cron}")
    @Transactional
    public void cleanupOrphanFiles() {
        // 24시간 이상 지난 탈퇴 회원 대상
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<User> withdrawUsers = userRepository.findAllByIsDeletedIsTrueAndDeletedAtBefore(threshold);

        if (!withdrawUsers.isEmpty()) {
            log.info("=== 탈퇴 회원 정보 정리 시작: 총 {}명 ===", withdrawUsers.size());

            for (User user : withdrawUsers) {
                try {
                    userRepository.hardDelete(user.getUserId());
                } catch (Exception e) {
                    log.error("유령 파일 삭제 중 오류 발생 [userId={}]: {}", user.getId(), e.getMessage());
                }
            }

            log.info("=== 탈퇴 회원 정보 정리 완료 ===");
        }
    }
}
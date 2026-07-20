package com.wut.practicum.task;

import com.wut.practicum.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceScheduler {

    private final AttendanceService attendanceService;

    /**
     * 每天早上 9:00 自动发布签到记录
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void publishDailyAttendance() {
        try {
            int count = attendanceService.publishDailyAttendance();
            log.info("Successfully published daily attendance records. Created count: {}", count);
        } catch (Exception e) {
            log.error("Failed to publish daily attendance records", e);
        }
    }
}

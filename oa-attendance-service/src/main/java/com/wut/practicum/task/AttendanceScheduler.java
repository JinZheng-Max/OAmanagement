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
     * 每天凌晨 00:05 自动触发各部门独立考勤任务生成
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void publishDailyAttendance() {
        try {
            int count = attendanceService.autoPublishAllActiveDepartmentTasks();
            log.info("【定时任务】成功按各部门规则发布今日考勤任务，创建任务总数: {}", count);
        } catch (Exception e) {
            log.error("【定时任务】按部门规则发布考勤任务失败", e);
        }
    }
}

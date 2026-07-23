package com.wut.practicum.task;

import com.wut.practicum.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class AttendanceScheduledTask {

    private final AttendanceService attendanceService;

    /**
     * 每天凌晨 00:05 依据各部门绑定的规则与启用的考勤场次，自动为各部门在职员工生成当天的考勤快照任务
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void autoPublishDailyDepartmentAttendance() {
        log.info("【定时任务】开始各部门自动考勤任务发布...");
        try {
            int count = attendanceService.autoPublishAllActiveDepartmentTasks();
            log.info("【定时任务】各部门自动考勤任务发布成功，新建任务总数: {}", count);
        } catch (Exception e) {
            log.error("【定时任务】各部门自动考勤任务发布失败", e);
        }
    }
}

package com.wut.practicum.delegate;

import com.wut.practicum.entity.OaAttendance;
import com.wut.practicum.mapper.AttendanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Flowable 自动回调：考勤补签审批【驳回】Delegate
 */
@Slf4j
@Component("attendanceRejectDelegate")
@RequiredArgsConstructor
public class AttendanceRejectDelegate implements JavaDelegate {

    private final AttendanceMapper attendanceMapper;
    private static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");

    @Override
    public void execute(DelegateExecution execution) {
        Long attendanceId = (Long) execution.getVariable("attendanceId");
        Long approverUserId = (Long) execution.getVariable("approverUserId");
        String approveComment = (String) execution.getVariable("approveComment");

        log.info("Flowable [AttendanceRejectDelegate] 执行驳回回调: attendanceId={}, approverUserId={}",
                attendanceId, approverUserId);

        if (attendanceId == null) return;

        OaAttendance att = attendanceMapper.selectById(attendanceId);
        if (att != null) {
            LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);
            att.setReplenishStatus("REJECTED");
            if (approverUserId != null) {
                att.setApproverId(approverUserId);
            }
            att.setApproveTime(now);
            att.setApproveComment(approveComment != null ? approveComment.trim() : "Flowable 工作流驳回");
            att.setUpdateTime(now);

            attendanceMapper.update(att);
            log.info("Flowable 考勤状态更新完成: attendanceId={}, replenishStatus=REJECTED", attendanceId);
        }
    }
}

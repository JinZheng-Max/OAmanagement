package com.wut.practicum.service;

import com.wut.practicum.dto.AttendancePageResult;
import com.wut.practicum.dto.AttendanceResponse;
import com.wut.practicum.dto.PageQuery;
import com.wut.practicum.entity.OaAttendance;
import com.wut.practicum.entity.OaAttendanceRule;
import java.util.List;

public interface AttendanceService {
    AttendanceResponse checkIn(Long employeeId, Long attendanceId, String clientIp);
    
    AttendanceResponse checkOut(Long employeeId, Long attendanceId, String clientIp);
    
    AttendancePageResult queryPersonalRecords(Long employeeId, String startDate, String endDate, PageQuery query, Long requestEmployeeId);
    
    AttendancePageResult queryAdminRecords(Long employeeId, Long departmentId, String startDate, String endDate, String status, PageQuery query);

    List<OaAttendance> queryTodayPersonalTasks(Long employeeId);

    // ================= 部门考勤规则配置 =================
    List<OaAttendanceRule> getDepartmentRules(Long departmentId, Long operatorDeptId, String operatorRole);

    OaAttendanceRule saveDepartmentRule(OaAttendanceRule rule, Long operatorDeptId, String operatorRole);

    void deleteDepartmentRule(Long ruleId, Long operatorDeptId, String operatorRole);

    // ================= 定时与手动发布 =================
    int publishDailyAttendance();

    int autoPublishAllActiveDepartmentTasks();

    int publishDepartmentAttendanceTask(Long departmentId, String targetDate, String sessionName, Long operatorDeptId, String operatorRole);

    // ================= 考勤补签申请与审批 =================
    AttendanceResponse applyReplenishment(Long attendanceId, Long employeeId, String reason);

    AttendanceResponse approveReplenishment(Long attendanceId, Long approverUserId, Long approverEmployeeId, String approverRole, boolean approved, String comment);

    AttendancePageResult queryReplenishRecords(Long departmentId, Long employeeId, String replenishStatus, PageQuery query, Long operatorDeptId, String operatorRole);

    // 旧管理员保存更新兼容方法
    AttendanceResponse saveOrUpdateAdminRecord(OaAttendance attendance);

    AttendanceResponse updateAdminRecord(Long id, OaAttendance attendance);
}


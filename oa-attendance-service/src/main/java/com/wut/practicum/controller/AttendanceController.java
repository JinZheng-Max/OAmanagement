package com.wut.practicum.controller;

import com.wut.practicum.client.EmployeeClient;
import com.wut.practicum.common.ApiResult;
import com.wut.practicum.common.BusinessException;
import com.wut.practicum.dto.AttendancePageResult;
import com.wut.practicum.dto.AttendanceResponse;
import com.wut.practicum.dto.PageQuery;
import com.wut.practicum.entity.OaAttendance;
import com.wut.practicum.entity.OaAttendanceRule;
import com.wut.practicum.security.CurrentUser;
import com.wut.practicum.service.AttendanceService;
import com.wut.practicum.util.IpUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "考勤管理模块", description = "包含多时段考勤打卡、部门独立考勤规则维护、补签申请与审批流等接口")
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeClient employeeClient;

    private Long getOperatorDeptId(CurrentUser user) {
        if (user == null || user.employeeId() == null) return null;
        try {
            var empRes = employeeClient.getById(user.employeeId());
            if (empRes != null && empRes.data() != null) {
                return empRes.data().departmentId();
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Operation(summary = "查询员工今日所有考勤任务", description = "用于工作台及打卡卡片展示当天发布的所有场次（如上午场、下午场）打卡记录与状态")
    @GetMapping("/today-tasks")
    public ApiResult<List<OaAttendance>> queryTodayPersonalTasks(@AuthenticationPrincipal CurrentUser user) {
        if (user == null || user.employeeId() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "当前账号未绑定员工");
        }
        return ApiResult.success(attendanceService.queryTodayPersonalTasks(user.employeeId()));
    }

    @Operation(summary = "员工签到", description = "打卡签到，系统将依据当前时段规则判定是否正常签到或迟到")
    @PostMapping("/check-in")
    public ApiResult<AttendanceResponse> checkIn(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam(required = false) Long attendanceId,
            HttpServletRequest request) {
        if (user == null || user.employeeId() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "当前账号未绑定员工，无法签到");
        }
        String clientIp = IpUtils.getClientIp(request);
        AttendanceResponse response = attendanceService.checkIn(user.employeeId(), attendanceId, clientIp);
        return ApiResult.success(response, "签到成功");
    }

    @Operation(summary = "员工签退", description = "打卡签退，系统将依据当前时段规则判定正常签退或早退")
    @PostMapping("/check-out")
    public ApiResult<AttendanceResponse> checkOut(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam(required = false) Long attendanceId,
            HttpServletRequest request) {
        if (user == null || user.employeeId() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "当前账号未绑定员工，无法签退");
        }
        String clientIp = IpUtils.getClientIp(request);
        AttendanceResponse response = attendanceService.checkOut(user.employeeId(), attendanceId, clientIp);
        return ApiResult.success(response, "签退成功");
    }

    // ================= 部门独立考勤规则维护 =================

    @Operation(summary = "查询部门考勤规则", description = "超级管理员可查全部部门规则，部门管理员只查本部门")
    @GetMapping("/rules")
    public ApiResult<List<OaAttendanceRule>> getDepartmentRules(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam(required = false) Long departmentId) {
        Long operatorDeptId = getOperatorDeptId(user);
        String role = user != null ? user.role() : null;
        List<OaAttendanceRule> rules = attendanceService.getDepartmentRules(departmentId, operatorDeptId, role);
        return ApiResult.success(rules);
    }

    @Operation(summary = "保存/修改部门考勤规则", description = "部门管理员只可配置本部门规则，超级管理员可配置全部")
    @PostMapping("/rules")
    public ApiResult<OaAttendanceRule> saveDepartmentRule(
            @AuthenticationPrincipal CurrentUser user,
            @RequestBody OaAttendanceRule rule) {
        Long operatorDeptId = getOperatorDeptId(user);
        String role = user != null ? user.role() : null;
        OaAttendanceRule saved = attendanceService.saveDepartmentRule(rule, operatorDeptId, role);
        return ApiResult.success(saved, "考勤规则保存成功");
    }

    @Operation(summary = "删除部门考勤规则")
    @DeleteMapping("/rules/{id}")
    public ApiResult<String> deleteDepartmentRule(
            @AuthenticationPrincipal CurrentUser user,
            @PathVariable Long id) {
        Long operatorDeptId = getOperatorDeptId(user);
        String role = user != null ? user.role() : null;
        attendanceService.deleteDepartmentRule(id, operatorDeptId, role);
        return ApiResult.success("删除成功");
    }

    @Operation(summary = "【管理员】手动按部门发布考勤", description = "发布指定部门指定日期/场次的考勤任务")
    @PostMapping("/publish-department")
    public ApiResult<String> publishDepartmentAttendance(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam Long departmentId,
            @RequestParam(required = false) String targetDate,
            @RequestParam(required = false) String sessionName) {
        Long operatorDeptId = getOperatorDeptId(user);
        String role = user != null ? user.role() : null;
        int count = attendanceService.publishDepartmentAttendanceTask(departmentId, targetDate, sessionName, operatorDeptId, role);
        return ApiResult.success("考勤任务发布完成，成功生成 " + count + " 条考勤卡片记录！");
    }

    // ================= 考勤补签申请与审批 =================

    public record ReplenishApplyDTO(Long attendanceId, String reason) {}
    public record ReplenishApproveDTO(Long attendanceId, boolean approved, String comment) {}

    @Operation(summary = "员工提交考勤补签申请")
    @PostMapping("/replenish/apply")
    public ApiResult<AttendanceResponse> applyReplenishment(
            @AuthenticationPrincipal CurrentUser user,
            @RequestBody ReplenishApplyDTO request) {
        if (user == null || user.employeeId() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "当前账号未绑定员工");
        }
        AttendanceResponse response = attendanceService.applyReplenishment(request.attendanceId(), user.employeeId(), request.reason());
        return ApiResult.success(response, "补签申请提交成功，请等待审批");
    }

    @Operation(summary = "【管理员】审批补签申请", description = "部门经理审批部门员工补签；部门经理自己的补签只能超级管理员审批")
    @PostMapping("/replenish/approve")
    public ApiResult<AttendanceResponse> approveReplenishment(
            @AuthenticationPrincipal CurrentUser user,
            @RequestBody ReplenishApproveDTO request) {
        if (user == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "未登录");
        }
        AttendanceResponse response = attendanceService.approveReplenishment(
                request.attendanceId(),
                user.userId(),
                user.employeeId(),
                user.role(),
                request.approved(),
                request.comment()
        );
        return ApiResult.success(response, request.approved() ? "补签审批已同意" : "补签申请已驳回");
    }

    @Operation(summary = "【管理员/员工】查询补签列表", description = "用于审批看板与补签历史展示")
    @GetMapping("/replenish/records")
    public ApiResult<AttendancePageResult> queryReplenishRecords(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String replenishStatus) {
        Long operatorDeptId = getOperatorDeptId(user);
        String role = user != null ? user.role() : null;
        PageQuery query = new PageQuery(page, size);
        AttendancePageResult result = attendanceService.queryReplenishRecords(departmentId, employeeId, replenishStatus, query, operatorDeptId, role);
        return ApiResult.success(result);
    }

    // ================= 历史考勤查询 =================

    @Operation(summary = "查询个人考勤记录")
    @GetMapping("/records")
    public ApiResult<AttendancePageResult> queryPersonalRecords(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long employeeId) {
        if (user == null || user.employeeId() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "未绑定员工账号");
        }
        PageQuery pageQuery = new PageQuery(page, size);
        AttendancePageResult result = attendanceService.queryPersonalRecords(user.employeeId(), startDate, endDate, pageQuery, employeeId);
        return ApiResult.success(result);
    }

    @Operation(summary = "【管理员】查询全员/部门考勤看板")
    @GetMapping("/admin/records")
    public ApiResult<AttendancePageResult> queryAdminRecords(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status) {
        Long targetDeptId = departmentId;
        if (user != null && user.role() != null && "DEPT_MANAGER".equalsIgnoreCase(user.role())) {
            targetDeptId = getOperatorDeptId(user);
        }
        PageQuery pageQuery = new PageQuery(page, size);
        AttendancePageResult result = attendanceService.queryAdminRecords(employeeId, targetDeptId, startDate, endDate, status, pageQuery);
        return ApiResult.success(result);
    }
}

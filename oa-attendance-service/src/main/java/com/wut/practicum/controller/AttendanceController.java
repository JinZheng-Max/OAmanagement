package com.wut.practicum.controller;

import com.wut.practicum.common.ApiResult;
import com.wut.practicum.common.BusinessException;
import com.wut.practicum.dto.AttendancePageResult;
import com.wut.practicum.dto.AttendanceResponse;
import com.wut.practicum.dto.PageQuery;
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

@Tag(name = "考勤管理模块", description = "包含员工签到、签退、个人考勤查询、管理员考勤看板、考勤补录与初始化等接口")
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final com.wut.practicum.client.EmployeeClient employeeClient;

    @Operation(summary = "员工签到", description = "用于员工打卡签到，系统会自动校验时间窗口(8:00-9:30)以及内网IP范围")
    @PostMapping("/check-in")
    public ApiResult<AttendanceResponse> checkIn(@AuthenticationPrincipal CurrentUser user, HttpServletRequest request) {
        if (user == null || user.employeeId() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "当前账号未绑定员工，无法签到");
        }
        String clientIp = IpUtils.getClientIp(request);
        AttendanceResponse response = attendanceService.checkIn(user.employeeId(), clientIp);
        return ApiResult.success(response, "签到成功");
    }

    @Operation(summary = "员工签退", description = "用于员工打卡签退，系统会自动校验签退时间(18:00-19:00)，早于18:00签退标记为早退")
    @PostMapping("/check-out")
    public ApiResult<AttendanceResponse> checkOut(@AuthenticationPrincipal CurrentUser user, HttpServletRequest request) {
        if (user == null || user.employeeId() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "当前账号未绑定员工，无法签退");
        }
        String clientIp = IpUtils.getClientIp(request);
        AttendanceResponse response = attendanceService.checkOut(user.employeeId(), clientIp);
        return ApiResult.success(response, "签退成功");
    }

    @Operation(summary = "查询个人考勤记录", description = "查询当前登录员工的历史考勤打卡记录，支持按日期范围筛选")
    @GetMapping("/records")
    public ApiResult<AttendancePageResult> queryPersonalRecords(
            @AuthenticationPrincipal CurrentUser user,
            @Parameter(description = "页码，默认 1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，默认 10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "开始日期 (yyyy-MM-dd)") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 (yyyy-MM-dd)") @RequestParam(required = false) String endDate,
            @Parameter(description = "特定员工ID (可选)") @RequestParam(required = false) Long employeeId) {
        if (user == null || user.employeeId() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "当前账号未绑定员工，无法查询考勤");
        }
        PageQuery pageQuery = new PageQuery(page, size);
        AttendancePageResult result = attendanceService.queryPersonalRecords(user.employeeId(), startDate, endDate, pageQuery, employeeId);
        return ApiResult.success(result);
    }

    @Operation(summary = "【管理员】查询全员/部门考勤看板", description = "超级管理员可看全公司考勤，部门管理员自动强管控只看本部门")
    @GetMapping("/admin/records")
    public ApiResult<AttendancePageResult> queryAdminRecords(
            @AuthenticationPrincipal CurrentUser user,
            @Parameter(description = "页码，默认 1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，默认 10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "员工ID (可选)") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "部门ID (可选)") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "开始日期 (yyyy-MM-dd)") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 (yyyy-MM-dd)") @RequestParam(required = false) String endDate,
            @Parameter(description = "考勤状态：CHECKED_IN / CHECKED_OUT / UNCHECKED / LEAVE_EARLY") @RequestParam(required = false) String status) {
        
        Long targetDeptId = departmentId;
        if (user != null && user.role() != null && "DEPT_MANAGER".equalsIgnoreCase(user.role())) {
            if (user.employeeId() != null) {
                try {
                    var empRes = employeeClient.getById(user.employeeId());
                    if (empRes != null && empRes.data() != null && empRes.data().departmentId() != null) {
                        targetDeptId = empRes.data().departmentId();
                    }
                } catch (Exception ignored) {}
            }
        }

        PageQuery pageQuery = new PageQuery(page, size);
        AttendancePageResult result = attendanceService.queryAdminRecords(employeeId, targetDeptId, startDate, endDate, status, pageQuery);
        return ApiResult.success(result);
    }

    @Operation(summary = "【管理员】新增/补录考勤记录", description = "管理员手动为员工新增或补录考勤打卡信息")
    @PostMapping("/admin/records")
    public ApiResult<AttendanceResponse> saveOrUpdateAdminRecord(@RequestBody com.wut.practicum.entity.OaAttendance record) {
        AttendanceResponse response = attendanceService.saveOrUpdateAdminRecord(record);
        return ApiResult.success(response, "考勤补录成功");
    }

    @Operation(summary = "【管理员】修改考勤记录", description = "管理员按ID修改已有的考勤状态、打卡时间或打卡IP")
    @PutMapping("/admin/records/{id}")
    public ApiResult<AttendanceResponse> updateAdminRecord(
            @Parameter(description = "考勤记录ID") @PathVariable Long id,
            @RequestBody com.wut.practicum.entity.OaAttendance record) {
        AttendanceResponse response = attendanceService.updateAdminRecord(id, record);
        return ApiResult.success(response, "考勤修改成功");
    }

    @Operation(summary = "【管理员】初始化今日考勤", description = "管理员手动触发，批量为今天尚未有考勤记录的在职员工生成初始UNCHECKED考勤记录")
    @PostMapping("/admin/publish")
    public ApiResult<String> publishDailyAttendance() {
        int count = attendanceService.publishDailyAttendance();
        if (count > 0) {
            return ApiResult.success("今日初始考勤发布成功，已为 " + count + " 名尚未有记录的在职员工初始化了今日考勤！");
        } else {
            return ApiResult.success("当前所有在职员工的今日考勤记录均已存在，无需重复发布。");
        }
    }
}

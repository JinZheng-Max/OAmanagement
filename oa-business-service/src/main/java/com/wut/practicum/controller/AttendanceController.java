package com.wut.practicum.controller;

import com.wut.practicum.common.ApiResult;
import com.wut.practicum.common.BusinessException;
import com.wut.practicum.dto.AttendancePageResult;
import com.wut.practicum.dto.AttendanceResponse;
import com.wut.practicum.dto.PageQuery;
import com.wut.practicum.security.CurrentUser;
import com.wut.practicum.service.AttendanceService;
import com.wut.practicum.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    public ApiResult<AttendanceResponse> checkIn(@AuthenticationPrincipal CurrentUser user, HttpServletRequest request) {
        if (user == null || user.employeeId() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "当前账号未绑定员工，无法签到");
        }
        String clientIp = IpUtils.getClientIp(request);
        AttendanceResponse response = attendanceService.checkIn(user.employeeId(), clientIp);
        return ApiResult.success(response, "签到成功");
    }

    @PostMapping("/check-out")
    public ApiResult<AttendanceResponse> checkOut(@AuthenticationPrincipal CurrentUser user, HttpServletRequest request) {
        if (user == null || user.employeeId() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "当前账号未绑定员工，无法签退");
        }
        String clientIp = IpUtils.getClientIp(request);
        AttendanceResponse response = attendanceService.checkOut(user.employeeId(), clientIp);
        return ApiResult.success(response, "签退成功");
    }

    @GetMapping("/records")
    public ApiResult<AttendancePageResult> queryPersonalRecords(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long employeeId) {
        if (user == null || user.employeeId() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "当前账号未绑定员工，无法查询考勤");
        }
        PageQuery pageQuery = new PageQuery(page, size);
        AttendancePageResult result = attendanceService.queryPersonalRecords(user.employeeId(), startDate, endDate, pageQuery, employeeId);
        return ApiResult.success(result);
    }

    @GetMapping("/admin/records")
    public ApiResult<AttendancePageResult> queryAdminRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status) {
        PageQuery pageQuery = new PageQuery(page, size);
        AttendancePageResult result = attendanceService.queryAdminRecords(employeeId, departmentId, startDate, endDate, status, pageQuery);
        return ApiResult.success(result);
    }
}

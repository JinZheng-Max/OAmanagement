package com.wut.practicum.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wut.practicum.client.EmployeeClient;
import com.wut.practicum.common.ApiResult;
import com.wut.practicum.common.BusinessException;
import com.wut.practicum.dto.AttendancePageResult;
import com.wut.practicum.dto.AttendanceResponse;
import com.wut.practicum.dto.EmployeeResponse;
import com.wut.practicum.dto.PageQuery;
import com.wut.practicum.entity.OaAttendance;
import com.wut.practicum.mapper.AttendanceMapper;
import com.wut.practicum.service.AttendanceService;
import com.wut.practicum.util.IpValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceMapper attendanceMapper;
    private final EmployeeClient employeeClient;
    private final IpValidator ipValidator;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    @Value("${oa.attendance.check-in-start:09:00}")
    private String checkInStartStr;

    @Value("${oa.attendance.check-in-end:09:30}")
    private String checkInEndStr;

    @Value("${oa.attendance.check-out-start:18:00}")
    private String checkOutStartStr;

    @Value("${oa.attendance.check-out-end:19:00}")
    private String checkOutEndStr;

    private static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");

    @Override
    @Transactional
    public AttendanceResponse checkIn(Long employeeId, String clientIp) {
        // 1. Verify if employee exists and is active
        ApiResult<EmployeeResponse> empResult = employeeClient.getById(employeeId);
        if (empResult == null || empResult.code() != 200 || empResult.data() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "员工不存在");
        }
        EmployeeResponse employee = empResult.data();
        if (employee.status() != 1) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "员工已离职，无法考勤");
        }

        // 2. Verify IP network constraint
        if (!ipValidator.isValid(clientIp)) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "当前网络非公司内网，禁止签到");
        }
        // 3. Time setup and restriction
        LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);
        java.time.LocalTime nowTime = now.toLocalTime();
        java.time.LocalTime startTime = java.time.LocalTime.parse(checkInStartStr != null ? checkInStartStr : "09:00");
        java.time.LocalTime endTime = java.time.LocalTime.parse(checkInEndStr != null ? checkInEndStr : "09:30");
        if (nowTime.isBefore(startTime) || nowTime.isAfter(endTime)) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "非签到时间段（打卡时间为" + (checkInStartStr != null ? checkInStartStr : "09:00") + " - " + (checkInEndStr != null ? checkInEndStr : "09:30") + "），请联系管理员补录");
        }
        String workDate = now.toLocalDate().toString();

        // 4. Redis lock/prevent concurrent double check-ins
        String lockKey = "oa:attendance:lock:" + employeeId + ":" + workDate;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        if (locked == null || !locked) {
            throw new BusinessException(40910, HttpStatus.CONFLICT, "签到处理中，请勿重复提交");
        }

        try {
            // Check if record exists (pre-created by scheduler)
            OaAttendance existing = attendanceMapper.selectByEmployeeAndDate(employeeId, workDate);
            if (existing == null) {
                throw new BusinessException(400, HttpStatus.BAD_REQUEST, "签到尚未发布，请联系管理员补录");
            }

            // Validate status transitions
            if ("CHECKED_IN".equals(existing.getStatus()) || "CHECKED_OUT".equals(existing.getStatus())) {
                throw new BusinessException(40910, HttpStatus.CONFLICT, "重复签到", convertToResponse(existing));
            }

            // Update record
            existing.setCheckIn(now);
            existing.setCheckInIp(clientIp);
            existing.setStatus("CHECKED_IN");
            existing.setUpdateTime(now);

            attendanceMapper.update(existing);

            // Clean caches
            clearPersonalCache(employeeId);
            clearAdminCache();

            return convertToResponse(existing);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
    @Override
    @Transactional
    public AttendanceResponse checkOut(Long employeeId, String clientIp) {
        // 1. Verify IP network constraint
        if (!ipValidator.isValid(clientIp)) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "当前网络非公司内网，禁止签退");
        }

        // Time setup
        LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);
        String workDate = now.toLocalDate().toString();

        // Check if record exists
        OaAttendance existing = attendanceMapper.selectByEmployeeAndDate(employeeId, workDate);
        if (existing == null) {
            throw new BusinessException(40911, HttpStatus.CONFLICT, "未签到即签退");
        }

        // Validate state transitions
        if ("CHECKED_OUT".equals(existing.getStatus()) || "LEAVE_EARLY".equals(existing.getStatus())) {
            throw new BusinessException(40911, HttpStatus.CONFLICT, "重复签退");
        }
        if (!"CHECKED_IN".equals(existing.getStatus())) {
            throw new BusinessException(40911, HttpStatus.CONFLICT, "未签到即签退");
        }

        // Validate checkout window: 18:00 - 19:00
        java.time.LocalTime nowTime = now.toLocalTime();
        java.time.LocalTime startTime = java.time.LocalTime.parse(checkOutStartStr != null ? checkOutStartStr : "18:00");
        java.time.LocalTime endTime = java.time.LocalTime.parse(checkOutEndStr != null ? checkOutEndStr : "19:00");

        if (nowTime.isAfter(endTime)) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "非签退时间段（签退时间为" + (checkOutStartStr != null ? checkOutStartStr : "18:00") + " - " + (checkOutEndStr != null ? checkOutEndStr : "19:00") + "），请联系管理员补录");
        }

        boolean isLeaveEarly = nowTime.isBefore(startTime);

        // Update record
        existing.setCheckOut(now);
        existing.setCheckOutIp(clientIp);
        existing.setStatus(isLeaveEarly ? "LEAVE_EARLY" : "CHECKED_OUT");
        existing.setUpdateTime(now);

        attendanceMapper.update(existing);

        // Clean caches
        clearPersonalCache(employeeId);
        clearAdminCache();

        return convertToResponse(existing);
    }
    @Override
    public AttendancePageResult queryPersonalRecords(Long employeeId, String startDate, String endDate, PageQuery query, Long requestEmployeeId) {
        // Date handling
        LocalDate today = LocalDate.now(ZONE_SHANGHAI);
        if (startDate == null || startDate.trim().isEmpty()) {
            startDate = today.minusDays(30).toString();
        }
        if (endDate == null || endDate.trim().isEmpty()) {
            endDate = today.toString();
        }

        // Range constraint verification
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            if (ChronoUnit.DAYS.between(start, end) > 31) {
                throw new BusinessException(400, HttpStatus.BAD_REQUEST, "日期范围不能超过31天");
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "日期格式错误，正确格式为 yyyy-MM-dd");
        }

        boolean isOverridden = false;
        Long targetEmployeeId = employeeId;
        if (requestEmployeeId != null && !requestEmployeeId.equals(employeeId)) {
            targetEmployeeId = employeeId;
            isOverridden = true;
        }

        String cacheKey = "oa:attendance:personal:" + targetEmployeeId + ":" + startDate + ":" + endDate + ":" + query.page() + ":" + query.size();
        AttendancePageResult cached = getFromCache(cacheKey, AttendancePageResult.class);

        if (cached != null) {
            if (isOverridden) {
                throw new BusinessException(40302, HttpStatus.FORBIDDEN, "无权查询他人数据，已强制返回本人记录", cached);
            }
            return cached;
        }

        // Query database
        List<OaAttendance> list = attendanceMapper.selectPageList(targetEmployeeId, null, startDate, endDate, null, query.offset(), query.size());
        long total = attendanceMapper.selectCount(targetEmployeeId, null, startDate, endDate, null);

        List<AttendanceResponse> responseList = list.stream().map(this::convertToResponse).collect(Collectors.toList());
        Map<String, Long> statistics = getStatistics(targetEmployeeId, null, startDate, endDate, null);

        AttendancePageResult result = AttendancePageResult.of(responseList, total, query, statistics);
        saveToCache(cacheKey, result, Duration.ofMinutes(10));

        if (isOverridden) {
            throw new BusinessException(40302, HttpStatus.FORBIDDEN, "无权查询他人数据，已强制返回本人记录", result);
        }

        return result;
    }

    @Override
    public AttendancePageResult queryAdminRecords(Long employeeId, Long departmentId, String startDate, String endDate, String status, PageQuery query) {
        LocalDate today = LocalDate.now(ZONE_SHANGHAI);
        if (startDate == null || startDate.trim().isEmpty()) {
            startDate = today.minusDays(30).toString();
        }
        if (endDate == null || endDate.trim().isEmpty()) {
            endDate = today.toString();
        }

        String cacheKey = "oa:attendance:admin:" + (employeeId == null ? "all" : employeeId) + ":" + (departmentId == null ? "all" : departmentId)
                + ":" + startDate + ":" + endDate + ":" + (status == null ? "all" : status) + ":" + query.page() + ":" + query.size();

        AttendancePageResult cached = getFromCache(cacheKey, AttendancePageResult.class);
        if (cached != null) {
            return cached;
        }

        // Query database
        List<OaAttendance> list = attendanceMapper.selectPageList(employeeId, departmentId, startDate, endDate, status, query.offset(), query.size());
        long total = attendanceMapper.selectCount(employeeId, departmentId, startDate, endDate, status);

        List<AttendanceResponse> responseList = list.stream().map(this::convertToResponse).collect(Collectors.toList());
        Map<String, Long> statistics = getStatistics(employeeId, departmentId, startDate, endDate, status);

        AttendancePageResult result = AttendancePageResult.of(responseList, total, query, statistics);
        saveToCache(cacheKey, result, Duration.ofMinutes(10));

        return result;
    }

    private AttendanceResponse convertToResponse(OaAttendance attendance) {
        if (attendance == null) return null;
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getEmployeeId(),
                attendance.getEmployeeName(),
                attendance.getWorkDate(),
                attendance.getCheckIn(),
                attendance.getCheckOut(),
                attendance.getCheckInIp(),
                attendance.getCheckOutIp(),
                attendance.getStatus(),
                attendance.getCreateTime(),
                attendance.getUpdateTime()
        );
    }

    private Map<String, Long> getStatistics(Long employeeId, Long departmentId, String startDate, String endDate, String status) {
        List<Map<String, Object>> statsList = attendanceMapper.countStatusByFilter(employeeId, departmentId, startDate, endDate, status);
        Map<String, Long> stats = new HashMap<>();
        stats.put("checkedIn", 0L);
        stats.put("checkedOut", 0L);
        stats.put("unchecked", 0L);
        if (statsList != null) {
            for (Map<String, Object> map : statsList) {
                String state = (String) map.get("status");
                Number countNum = (Number) map.get("count");
                long count = countNum != null ? countNum.longValue() : 0L;
                if ("CHECKED_IN".equals(state)) {
                    stats.put("checkedIn", count);
                } else if ("CHECKED_OUT".equals(state) || "LEAVE_EARLY".equals(state)) {
                    stats.put("checkedOut", stats.getOrDefault("checkedOut", 0L) + count);
                } else if ("UNCHECKED".equals(state)) {
                    stats.put("unchecked", count);
                }
            }
        }
        return stats;
    }

    private void saveToCache(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            log.warn("Failed to save to cache: key={}", key, e);
        }
    }

    private <T> T getFromCache(String key, Class<T> clazz) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("Failed to get from cache: key={}", key, e);
            return null;
        }
    }

    private void clearPersonalCache(Long employeeId) {
        try {
            Set<String> keys = redisTemplate.keys("oa:attendance:personal:" + employeeId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Failed to clear personal cache for employeeId={}", employeeId, e);
        }
    }

    private void clearAdminCache() {
        try {
            Set<String> keys = redisTemplate.keys("oa:attendance:admin:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Failed to clear admin cache", e);
        }
    }
    @Override
    @Transactional
    public int publishDailyAttendance() {
        String workDate = LocalDate.now(ZONE_SHANGHAI).toString();
        LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);
        List<Long> activeEmployeeIds = attendanceMapper.selectActiveEmployeeIds();
        log.info("Publishing daily attendance for workDate={}, activeEmployeesCount={}", workDate, activeEmployeeIds.size());
        
        int createdCount = 0;
        for (Long empId : activeEmployeeIds) {
            OaAttendance existing = attendanceMapper.selectByEmployeeAndDate(empId, workDate);
            if (existing == null) {
                OaAttendance attendance = new OaAttendance();
                attendance.setEmployeeId(empId);
                attendance.setWorkDate(workDate);
                attendance.setStatus("UNCHECKED");
                attendance.setCreateTime(now);
                attendance.setUpdateTime(now);
                try {
                    attendanceMapper.insert(attendance);
                    createdCount++;
                } catch (DuplicateKeyException e) {
                    log.debug("Attendance record already exists for employeeId={}, workDate={}", empId, workDate);
                }
            }
        }
        clearAdminCache();
        return createdCount;
    }

    @Override
    @Transactional
    public AttendanceResponse saveOrUpdateAdminRecord(OaAttendance record) {
        if (record.getEmployeeId() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "员工ID不能为空");
        }
        if (record.getWorkDate() == null || record.getWorkDate().trim().isEmpty()) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "工作日期不能为空");
        }
        
        ApiResult<EmployeeResponse> empResult = employeeClient.getById(record.getEmployeeId());
        if (empResult == null || empResult.code() != 200 || empResult.data() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "员工不存在");
        }
        
        LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);
        OaAttendance existing = attendanceMapper.selectByEmployeeAndDate(record.getEmployeeId(), record.getWorkDate());
        if (existing != null) {
            existing.setCheckIn(record.getCheckIn());
            existing.setCheckOut(record.getCheckOut());
            existing.setCheckInIp(record.getCheckInIp());
            existing.setCheckOutIp(record.getCheckOutIp());
            existing.setStatus(record.getStatus());
            existing.setUpdateTime(now);
            attendanceMapper.update(existing);
            
            clearPersonalCache(record.getEmployeeId());
            clearAdminCache();
            return convertToResponse(attendanceMapper.selectById(existing.getId()));
        } else {
            record.setCreateTime(now);
            record.setUpdateTime(now);
            if (record.getStatus() == null) {
                record.setStatus("UNCHECKED");
            }
            attendanceMapper.insert(record);
            
            clearPersonalCache(record.getEmployeeId());
            clearAdminCache();
            return convertToResponse(attendanceMapper.selectById(record.getId()));
        }
    }

    @Override
    @Transactional
    public AttendanceResponse updateAdminRecord(Long id, OaAttendance record) {
        OaAttendance existing = attendanceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "考勤记录不存在");
        }
        
        LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);
        existing.setCheckIn(record.getCheckIn());
        existing.setCheckOut(record.getCheckOut());
        existing.setCheckInIp(record.getCheckInIp());
        existing.setCheckOutIp(record.getCheckOutIp());
        existing.setStatus(record.getStatus());
        existing.setUpdateTime(now);
        
        attendanceMapper.update(existing);
        
        clearPersonalCache(existing.getEmployeeId());
        clearAdminCache();
        return convertToResponse(attendanceMapper.selectById(id));
    }
}

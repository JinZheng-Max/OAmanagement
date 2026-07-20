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

        // 3. Time setup
        LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);
        String workDate = now.toLocalDate().toString();

        // 4. Redis lock/prevent concurrent double check-ins
        String lockKey = "oa:attendance:lock:" + employeeId + ":" + workDate;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        if (locked == null || !locked) {
            throw new BusinessException(40910, HttpStatus.CONFLICT, "签到处理中，请勿重复提交");
        }

        try {
            // Check if record already exists
            OaAttendance existing = attendanceMapper.selectByEmployeeAndDate(employeeId, workDate);
            if (existing != null) {
                throw new BusinessException(40910, HttpStatus.CONFLICT, "重复签到", convertToResponse(existing));
            }

            // Create new check-in record
            OaAttendance attendance = new OaAttendance();
            attendance.setEmployeeId(employeeId);
            attendance.setWorkDate(workDate);
            attendance.setCheckIn(now);
            attendance.setCheckInIp(clientIp);
            attendance.setStatus("CHECKED_IN");
            attendance.setCreateTime(now);
            attendance.setUpdateTime(now);
            attendance.setEmployeeName(employee.name());

            try {
                attendanceMapper.insert(attendance);
            } catch (DuplicateKeyException e) {
                // Catch concurrent DB constraint violations
                OaAttendance dbExisting = attendanceMapper.selectByEmployeeAndDate(employeeId, workDate);
                throw new BusinessException(40910, HttpStatus.CONFLICT, "重复签到", convertToResponse(dbExisting));
            }

            // Clean caches
            clearPersonalCache(employeeId);
            clearAdminCache();

            return convertToResponse(attendance);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    @Transactional
    public AttendanceResponse checkOut(Long employeeId, String clientIp) {
        // Time setup
        LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);
        String workDate = now.toLocalDate().toString();

        // Check if record exists
        OaAttendance existing = attendanceMapper.selectByEmployeeAndDate(employeeId, workDate);
        if (existing == null) {
            throw new BusinessException(40911, HttpStatus.CONFLICT, "未签到即签退");
        }

        // Validate state transitions
        if ("CHECKED_OUT".equals(existing.getStatus())) {
            throw new BusinessException(40911, HttpStatus.CONFLICT, "重复签退");
        }
        if (!"CHECKED_IN".equals(existing.getStatus())) {
            throw new BusinessException(40911, HttpStatus.CONFLICT, "未签到即签退或重复签退");
        }

        // Update record
        existing.setCheckOut(now);
        existing.setCheckOutIp(clientIp);
        existing.setStatus("CHECKED_OUT");
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
                } else if ("CHECKED_OUT".equals(state)) {
                    stats.put("checkedOut", count);
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
}

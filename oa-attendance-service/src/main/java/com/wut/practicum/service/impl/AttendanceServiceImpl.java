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
import com.wut.practicum.entity.OaAttendanceRule;
import com.wut.practicum.mapper.AttendanceMapper;
import com.wut.practicum.mapper.OaAttendanceRuleMapper;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceMapper attendanceMapper;
    private final OaAttendanceRuleMapper oaAttendanceRuleMapper;
    private final EmployeeClient employeeClient;
    private final IpValidator ipValidator;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final org.flowable.engine.RuntimeService runtimeService;
    private final org.flowable.engine.TaskService taskService;

    private static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");

    @jakarta.annotation.PostConstruct
    public void initDatabaseTable() {
        try {
            oaAttendanceRuleMapper.createTableIfNotExists();
            log.info("Attendance rule table check/initialization completed.");
        } catch (Exception e) {
            log.warn("Failed to check/create sys_department_attendance_rule table", e);
        }
    }

    @Override
    @Transactional
    public AttendanceResponse checkIn(Long employeeId, Long attendanceId, String clientIp) {
        // 1. 验证员工在职状态
        ApiResult<EmployeeResponse> empResult = employeeClient.getById(employeeId);
        if (empResult == null || empResult.code() != 200 || empResult.data() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "员工不存在");
        }
        EmployeeResponse employee = empResult.data();
        if (employee.status() != 1) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "员工已离职，无法考勤");
        }

        // 2. 校验内网 IP 限制
        if (!ipValidator.isValid(clientIp)) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "当前网络非公司内网，禁止签到");
        }

        LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);
        java.time.LocalTime nowTime = now.toLocalTime();
        String workDate = now.toLocalDate().toString();

        // 3. 获取特定的考勤场次任务
        OaAttendance existing = null;
        if (attendanceId != null) {
            existing = attendanceMapper.selectById(attendanceId);
        } else {
            List<OaAttendance> list = attendanceMapper.selectListByEmployeeAndDate(employeeId, workDate);
            if (list != null && !list.isEmpty()) {
                for (OaAttendance item : list) {
                    if (!"CHECKED_IN".equals(item.getStatus()) && !"CHECKED_OUT".equals(item.getStatus()) && !"LATE".equals(item.getStatus())) {
                        existing = item;
                        break;
                    }
                }
                if (existing == null) existing = list.get(0);
            }
        }

        if (existing == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "今日尚未发布考勤任务，请联系部门管理员发布");
        }

        if ("CHECKED_IN".equals(existing.getStatus()) || "CHECKED_OUT".equals(existing.getStatus()) || "LATE".equals(existing.getStatus())) {
            throw new BusinessException(40910, HttpStatus.CONFLICT, "【" + existing.getSessionName() + "】您已经完成签到，请勿重复操作", convertToResponse(existing));
        }

        // 4. Redis 防重提交锁
        String lockKey = "oa:attendance:lock:in:" + existing.getId();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        if (locked == null || !locked) {
            throw new BusinessException(40910, HttpStatus.CONFLICT, "签到处理中，请勿重复提交");
        }

        try {
            // 5. 动态时间窗口校验与迟到状态判断
            String startStr = (existing.getCheckInStartTime() != null && !existing.getCheckInStartTime().isBlank()) ? existing.getCheckInStartTime() : "08:50";
            String normalEndStr = (existing.getNormalCheckInEndTime() != null && !existing.getNormalCheckInEndTime().isBlank()) ? existing.getNormalCheckInEndTime() : "09:10";
            String endStr = (existing.getCheckInEndTime() != null && !existing.getCheckInEndTime().isBlank()) ? existing.getCheckInEndTime() : "12:10";

            java.time.LocalTime startTime = java.time.LocalTime.parse(startStr);
            java.time.LocalTime normalEndTime = java.time.LocalTime.parse(normalEndStr);
            java.time.LocalTime endTime = java.time.LocalTime.parse(endStr);

            if (nowTime.isBefore(startTime)) {
                throw new BusinessException(400, HttpStatus.BAD_REQUEST, "【" + existing.getSessionName() + "】签到尚未开放（开放时间为 " + startStr + " - " + endStr + "）");
            }
            if (nowTime.isAfter(endTime)) {
                throw new BusinessException(400, HttpStatus.BAD_REQUEST, "【" + existing.getSessionName() + "】签到已截止（最晚截止 " + endStr + "），请在系统中提交补签申请");
            }

            // 6. 判定状态（在正常截止时间前为 CHECKED_IN，之后为 LATE 迟到）
            String status = nowTime.isAfter(normalEndTime) ? "LATE" : "CHECKED_IN";

            existing.setCheckIn(now);
            existing.setCheckInIp(clientIp);
            existing.setStatus(status);
            existing.setUpdateTime(now);

            attendanceMapper.update(existing);
            clearPersonalCache(employeeId);
            clearAdminCache();

            return convertToResponse(existing);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    @Transactional
    public AttendanceResponse checkOut(Long employeeId, Long attendanceId, String clientIp) {
        if (!ipValidator.isValid(clientIp)) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "当前网络非公司内网，禁止签退");
        }

        LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);
        java.time.LocalTime nowTime = now.toLocalTime();
        String workDate = now.toLocalDate().toString();

        OaAttendance existing = null;
        if (attendanceId != null) {
            existing = attendanceMapper.selectById(attendanceId);
        } else {
            List<OaAttendance> list = attendanceMapper.selectListByEmployeeAndDate(employeeId, workDate);
            if (list != null && !list.isEmpty()) {
                for (OaAttendance item : list) {
                    if ("CHECKED_IN".equals(item.getStatus()) || "LATE".equals(item.getStatus())) {
                        existing = item;
                        break;
                    }
                }
                if (existing == null) existing = list.get(0);
            }
        }

        if (existing == null) {
            throw new BusinessException(40911, HttpStatus.CONFLICT, "暂无签到记录，请联系管理员");
        }

        if ("CHECKED_OUT".equals(existing.getStatus()) || "EARLY_LEAVE".equals(existing.getStatus())) {
            throw new BusinessException(40911, HttpStatus.CONFLICT, "【" + existing.getSessionName() + "】您已完成签退，请勿重复操作");
        }

        String lockKey = "oa:attendance:lock:out:" + existing.getId();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        if (locked == null || !locked) {
            throw new BusinessException(40911, HttpStatus.CONFLICT, "签退处理中，请勿重复提交");
        }

        try {
            String normalStartStr = (existing.getNormalCheckOutStartTime() != null && !existing.getNormalCheckOutStartTime().isBlank()) ? existing.getNormalCheckOutStartTime() : "11:50";
            String endStr = (existing.getCheckOutEndTime() != null && !existing.getCheckOutEndTime().isBlank()) ? existing.getCheckOutEndTime() : "12:10";

            java.time.LocalTime normalStartTime = java.time.LocalTime.parse(normalStartStr);
            java.time.LocalTime endTime = java.time.LocalTime.parse(endStr);

            if (nowTime.isAfter(endTime)) {
                throw new BusinessException(400, HttpStatus.BAD_REQUEST, "【" + existing.getSessionName() + "】签退已截止，请提交补签申请");
            }

            boolean isLeaveEarly = nowTime.isBefore(normalStartTime);

            existing.setCheckOut(now);
            existing.setCheckOutIp(clientIp);
            existing.setStatus(isLeaveEarly ? "EARLY_LEAVE" : "CHECKED_OUT");
            existing.setUpdateTime(now);

            attendanceMapper.update(existing);
            clearPersonalCache(employeeId);
            clearAdminCache();

            return convertToResponse(existing);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    @Transactional
    public List<OaAttendance> queryTodayPersonalTasks(Long employeeId) {
        String today = LocalDate.now(ZONE_SHANGHAI).toString();
        LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);

        // 1. 获取员工所属部门
        Long deptId = null;
        try {
            ApiResult<EmployeeResponse> empResult = employeeClient.getById(employeeId);
            if (empResult != null && empResult.data() != null) {
                deptId = empResult.data().departmentId();
            }
        } catch (Exception e) {
            log.warn("queryTodayPersonalTasks: 获取员工部门信息失败 employeeId={}", employeeId, e);
        }
        if (deptId == null) {
            log.info("queryTodayPersonalTasks: 员工 {} 暂无部门，分配默认部门 1", employeeId);
            deptId = 1L;
        }

        // 2. 查询部门的已启用考勤规则
        List<OaAttendanceRule> rules = oaAttendanceRuleMapper.selectByDepartmentId(deptId);
        if (rules == null) rules = new java.util.ArrayList<>();
        
        // 过滤只保留 enabled=1 的规则
        List<OaAttendanceRule> enabledRules = rules.stream()
                .filter(r -> r.getEnabled() != null && r.getEnabled() == 1)
                .collect(Collectors.toList());

        // 保底处理：若部门尚未配置或启用任何考勤规则，自动为其初始化默认模板 (上午场 + 下午场)
        if (enabledRules.isEmpty()) {
            log.info("queryTodayPersonalTasks: 部门 {} 暂无已启用考勤规则，自动生成保底默认规则模板", deptId);
            OaAttendanceRule r1 = new OaAttendanceRule();
            r1.setDepartmentId(deptId);
            r1.setSessionName("上午场");
            r1.setCheckInStartTime("08:50");
            r1.setNormalCheckInEndTime("09:10");
            r1.setCheckInEndTime("12:10");
            r1.setNormalCheckOutStartTime("11:50");
            r1.setCheckOutEndTime("12:10");
            r1.setEnabled(1);
            try { oaAttendanceRuleMapper.insert(r1); } catch (Exception ignored) {}

            OaAttendanceRule r2 = new OaAttendanceRule();
            r2.setDepartmentId(deptId);
            r2.setSessionName("下午场");
            r2.setCheckInStartTime("13:50");
            r2.setNormalCheckInEndTime("14:10");
            r2.setCheckInEndTime("17:10");
            r2.setNormalCheckOutStartTime("16:50");
            r2.setCheckOutEndTime("17:10");
            r2.setEnabled(1);
            try { oaAttendanceRuleMapper.insert(r2); } catch (Exception ignored) {}

            enabledRules = oaAttendanceRuleMapper.selectByDepartmentId(deptId).stream()
                    .filter(r -> r.getEnabled() != null && r.getEnabled() == 1)
                    .collect(Collectors.toList());
        }

        log.info("queryTodayPersonalTasks: employeeId={} deptId={} 找到 {} 条已启用规则", employeeId, deptId, enabledRules.size());

        // 3. 对每条规则，确保今天有对应的考勤任务记录
        for (OaAttendanceRule rule : enabledRules) {
            String sessionName = rule.getSessionName();
            OaAttendance existing = attendanceMapper.selectByEmployeeDateAndSession(employeeId, today, sessionName);
            log.info("queryTodayPersonalTasks: 查询 [{}] 场次记录 -> {}", sessionName, existing == null ? "不存在，将新增" : "已存在 id=" + existing.getId());
            if (existing == null) {
                OaAttendance att = new OaAttendance();
                att.setEmployeeId(employeeId);
                att.setDepartmentId(deptId);
                att.setWorkDate(today);
                att.setSessionName(sessionName);
                att.setCheckInStartTime(rule.getCheckInStartTime());
                att.setNormalCheckInEndTime(rule.getNormalCheckInEndTime());
                att.setCheckInEndTime(rule.getCheckInEndTime());
                att.setNormalCheckOutStartTime(rule.getNormalCheckOutStartTime());
                att.setCheckOutEndTime(rule.getCheckOutEndTime());
                att.setStatus("UNCHECKED");
                att.setReplenishStatus("NONE");
                att.setCreateTime(now);
                att.setUpdateTime(now);
                try {
                    attendanceMapper.insert(att);
                    log.info("queryTodayPersonalTasks: 成功插入 [{}] 场次, id={}", sessionName, att.getId());
                } catch (Exception e) {
                    log.error("queryTodayPersonalTasks: 插入 [{}] 场次失败！", sessionName, e);
                }
            }
        }

        // 4. 返回今天所有场次记录（若仍然为空，强制为该员工生成【上午场】与【下午场】保底任务记录）
        List<OaAttendance> result = attendanceMapper.selectListByEmployeeAndDate(employeeId, today);
        log.info("queryTodayPersonalTasks: employeeId={} 今日共返回 {} 条记录", employeeId, result == null ? 0 : result.size());

        if (result == null || result.isEmpty()) {
            log.info("queryTodayPersonalTasks: 员工 {} 今日尚无任务记录，强行生成默认上午场与下午场记录", employeeId);
            OaAttendance att1 = new OaAttendance();
            att1.setEmployeeId(employeeId);
            att1.setDepartmentId(deptId);
            att1.setWorkDate(today);
            att1.setSessionName("上午场");
            att1.setCheckInStartTime("08:50");
            att1.setNormalCheckInEndTime("09:10");
            att1.setCheckInEndTime("12:10");
            att1.setNormalCheckOutStartTime("11:50");
            att1.setCheckOutEndTime("12:10");
            att1.setStatus("UNCHECKED");
            att1.setReplenishStatus("NONE");
            att1.setCreateTime(now);
            att1.setUpdateTime(now);
            try { attendanceMapper.insert(att1); } catch (Exception ignored) {}

            OaAttendance att2 = new OaAttendance();
            att2.setEmployeeId(employeeId);
            att2.setDepartmentId(deptId);
            att2.setWorkDate(today);
            att2.setSessionName("下午场");
            att2.setCheckInStartTime("13:50");
            att2.setNormalCheckInEndTime("14:10");
            att2.setCheckInEndTime("17:10");
            att2.setNormalCheckOutStartTime("16:50");
            att2.setCheckOutEndTime("17:10");
            att2.setStatus("UNCHECKED");
            att2.setReplenishStatus("NONE");
            att2.setCreateTime(now);
            att2.setUpdateTime(now);
            try { attendanceMapper.insert(att2); } catch (Exception ignored) {}

            result = attendanceMapper.selectListByEmployeeAndDate(employeeId, today);
        }

        if (result != null && result.size() > 1) {
            List<OaAttendance> filtered = result.stream()
                    .filter(t -> !"默认场次".equals(t.getSessionName()))
                    .collect(Collectors.toList());
            if (!filtered.isEmpty()) return filtered;
        }
        return result;
    }

    // ================= 部门规则管理 =================

    @Override
    public List<OaAttendanceRule> getDepartmentRules(Long departmentId, Long operatorDeptId, String operatorRole) {
        // 部门经理只能查看本部门规则
        if ("DEPT_MANAGER".equalsIgnoreCase(operatorRole)) {
            if (operatorDeptId == null || !operatorDeptId.equals(departmentId)) {
                departmentId = operatorDeptId;
            }
        }
        if (departmentId == null) {
            return oaAttendanceRuleMapper.selectAllEnabled();
        }
        return oaAttendanceRuleMapper.selectByDepartmentId(departmentId);
    }

    @Override
    @Transactional
    public OaAttendanceRule saveDepartmentRule(OaAttendanceRule rule, Long operatorDeptId, String operatorRole) {
        if (rule.getDepartmentId() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "部门ID不能为空");
        }
        if ("DEPT_MANAGER".equalsIgnoreCase(operatorRole)) {
            if (operatorDeptId == null || !operatorDeptId.equals(rule.getDepartmentId())) {
                throw new BusinessException(403, HttpStatus.FORBIDDEN, "权限不足：部门管理员只能维护本部门的考勤规则");
            }
        }

        if (rule.getSessionName() == null || rule.getSessionName().isBlank()) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "场次名称不能为空 (如 上午场 / 下午场)");
        }
        if (rule.getCheckInStartTime() == null || rule.getNormalCheckInEndTime() == null || rule.getCheckInEndTime() == null
                || rule.getNormalCheckOutStartTime() == null || rule.getCheckOutEndTime() == null) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "签到签退所有时间窗口均不能为空");
        }

        if (rule.getEnabled() == null) rule.setEnabled(1);

        if (rule.getId() == null) {
            OaAttendanceRule exist = oaAttendanceRuleMapper.selectByDeptAndSession(rule.getDepartmentId(), rule.getSessionName().trim());
            if (exist != null) {
                throw new BusinessException(400, HttpStatus.BAD_REQUEST, "该部门下已存在同名场次规则: " + rule.getSessionName());
            }
            rule.setSessionName(rule.getSessionName().trim());
            oaAttendanceRuleMapper.insert(rule);
        } else {
            oaAttendanceRuleMapper.update(rule);
        }

        // 规则修改后，自动为今天尚未打卡(UNCHECKED)的卡片同步最新规则的时间窗口
        try {
            String today = LocalDate.now(ZONE_SHANGHAI).toString();
            attendanceMapper.updateUncheckedRuleSnapshot(
                    rule.getDepartmentId(),
                    today,
                    rule.getSessionName(),
                    rule.getCheckInStartTime(),
                    rule.getNormalCheckInEndTime(),
                    rule.getCheckInEndTime(),
                    rule.getNormalCheckOutStartTime(),
                    rule.getCheckOutEndTime()
            );
        } catch (Exception e) {
            log.warn("Sync rule snapshot for unchecked tasks failed", e);
        }

        return oaAttendanceRuleMapper.selectById(rule.getId());
    }

    @Override
    @Transactional
    public void deleteDepartmentRule(Long ruleId, Long operatorDeptId, String operatorRole) {
        OaAttendanceRule rule = oaAttendanceRuleMapper.selectById(ruleId);
        if (rule == null) return;
        if ("DEPT_MANAGER".equalsIgnoreCase(operatorRole)) {
            if (operatorDeptId == null || !operatorDeptId.equals(rule.getDepartmentId())) {
                throw new BusinessException(403, HttpStatus.FORBIDDEN, "权限不足：部门管理员只能删除本部门考勤规则");
            }
        }
        oaAttendanceRuleMapper.deleteById(ruleId);
    }

    // ================= 任务自动与手动发布 =================

    @Override
    @Transactional
    public int publishDailyAttendance() {
        return autoPublishAllActiveDepartmentTasks();
    }

    @Override
    @Transactional
    public int autoPublishAllActiveDepartmentTasks() {
        String workDate = LocalDate.now(ZONE_SHANGHAI).toString();
        List<OaAttendanceRule> rules = oaAttendanceRuleMapper.selectAllEnabled();
        if (rules == null || rules.isEmpty()) return 0;

        int totalCreated = 0;
        LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);

        for (OaAttendanceRule rule : rules) {
            List<Long> empIds = attendanceMapper.selectActiveEmployeeIdsByDepartment(rule.getDepartmentId());
            if (empIds == null || empIds.isEmpty()) continue;

            for (Long empId : empIds) {
                OaAttendance exist = attendanceMapper.selectByEmployeeDateAndSession(empId, workDate, rule.getSessionName());
                if (exist == null) {
                    OaAttendance att = new OaAttendance();
                    att.setEmployeeId(empId);
                    att.setDepartmentId(rule.getDepartmentId());
                    att.setWorkDate(workDate);
                    att.setSessionName(rule.getSessionName());
                    att.setCheckInStartTime(rule.getCheckInStartTime());
                    att.setNormalCheckInEndTime(rule.getNormalCheckInEndTime());
                    att.setCheckInEndTime(rule.getCheckInEndTime());
                    att.setNormalCheckOutStartTime(rule.getNormalCheckOutStartTime());
                    att.setCheckOutEndTime(rule.getCheckOutEndTime());
                    att.setStatus("UNCHECKED");
                    att.setReplenishStatus("NONE");
                    att.setCreateTime(now);
                    att.setUpdateTime(now);
                    try {
                        attendanceMapper.insert(att);
                        totalCreated++;
                    } catch (DuplicateKeyException ignored) {}
                }
            }
        }
        clearAdminCache();
        return totalCreated;
    }

    @Override
    @Transactional
    public int publishDepartmentAttendanceTask(Long departmentId, String targetDate, String sessionName, Long operatorDeptId, String operatorRole) {
        if ("DEPT_MANAGER".equalsIgnoreCase(operatorRole)) {
            if (operatorDeptId == null || !operatorDeptId.equals(departmentId)) {
                throw new BusinessException(403, HttpStatus.FORBIDDEN, "权限不足：只能发布本部门考勤任务");
            }
        }
        if (targetDate == null || targetDate.isBlank()) {
            targetDate = LocalDate.now(ZONE_SHANGHAI).toString();
        }

        List<OaAttendanceRule> rules = oaAttendanceRuleMapper.selectByDepartmentId(departmentId);
        if (rules == null || rules.isEmpty()) {
            // 为该部门自动创建默认考勤规则（上午场 08:50-12:10，下午场 13:50-17:10）
            OaAttendanceRule r1 = new OaAttendanceRule();
            r1.setDepartmentId(departmentId);
            r1.setSessionName("上午场");
            r1.setCheckInStartTime("08:50");
            r1.setNormalCheckInEndTime("09:10");
            r1.setCheckInEndTime("12:10");
            r1.setNormalCheckOutStartTime("11:50");
            r1.setCheckOutEndTime("12:10");
            r1.setEnabled(1);
            try { oaAttendanceRuleMapper.insert(r1); } catch (Exception ignored) {}

            OaAttendanceRule r2 = new OaAttendanceRule();
            r2.setDepartmentId(departmentId);
            r2.setSessionName("下午场");
            r2.setCheckInStartTime("13:50");
            r2.setNormalCheckInEndTime("14:10");
            r2.setCheckInEndTime("17:10");
            r2.setNormalCheckOutStartTime("16:50");
            r2.setCheckOutEndTime("17:10");
            r2.setEnabled(1);
            try { oaAttendanceRuleMapper.insert(r2); } catch (Exception ignored) {}

            rules = oaAttendanceRuleMapper.selectByDepartmentId(departmentId);
        }

        List<Long> empIds = attendanceMapper.selectActiveEmployeeIdsByDepartment(departmentId);
        if (empIds == null || empIds.isEmpty()) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "该部门下暂无在职员工");
        }

        int created = 0;
        LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);

        for (OaAttendanceRule rule : rules) {
            if (rule.getEnabled() == null || rule.getEnabled() != 1) continue;
            if (sessionName != null && !sessionName.isBlank() && !sessionName.equals(rule.getSessionName())) {
                continue;
            }

            for (Long empId : empIds) {
                OaAttendance exist = attendanceMapper.selectByEmployeeDateAndSession(empId, targetDate, rule.getSessionName());
                if (exist == null) {
                    OaAttendance att = new OaAttendance();
                    att.setEmployeeId(empId);
                    att.setDepartmentId(departmentId);
                    att.setWorkDate(targetDate);
                    att.setSessionName(rule.getSessionName());
                    att.setCheckInStartTime(rule.getCheckInStartTime());
                    att.setNormalCheckInEndTime(rule.getNormalCheckInEndTime());
                    att.setCheckInEndTime(rule.getCheckInEndTime());
                    att.setNormalCheckOutStartTime(rule.getNormalCheckOutStartTime());
                    att.setCheckOutEndTime(rule.getCheckOutEndTime());
                    att.setStatus("UNCHECKED");
                    att.setReplenishStatus("NONE");
                    att.setCreateTime(now);
                    att.setUpdateTime(now);
                    try {
                        attendanceMapper.insert(att);
                        created++;
                    } catch (DuplicateKeyException ignored) {}
                }
            }
        }

        clearAdminCache();
        return created;
    }

    // ================= 补签申请与审批 (Flowable 工作流引擎驱动) =================

    @Override
    @Transactional
    public AttendanceResponse applyReplenishment(Long attendanceId, Long employeeId, String reason) {
        OaAttendance att = attendanceMapper.selectById(attendanceId);
        if (att == null) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "考勤记录不存在");
        }
        if (!att.getEmployeeId().equals(employeeId)) {
            throw new BusinessException(403, HttpStatus.FORBIDDEN, "只能为自己的考勤发起补签申请");
        }
        if ("PENDING".equals(att.getReplenishStatus())) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "补签申请已在 Flowable 审批流中，请勿重复发起");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "补签原因不能为空");
        }

        // 1. 获取申请人角色 (如判断是否为部门经理)
        String applicantRole = "EMPLOYEE";
        try {
            ApiResult<EmployeeResponse> empRes = employeeClient.getById(employeeId);
            if (empRes != null && empRes.data() != null) {
                // 如果是部门领导，标为 DEPT_MANAGER
                if ("部门经理".equals(empRes.data().position()) || "经理".equals(empRes.data().position())) {
                    applicantRole = "DEPT_MANAGER";
                }
            }
        } catch (Exception e) {
            log.warn("applyReplenishment: 获取申请人员工信息失败, employeeId={}", employeeId, e);
        }

        // 2. 更新基础表为待审批状态
        att.setReplenishStatus("PENDING");
        att.setReplenishReason(reason.trim());
        att.setUpdateTime(LocalDateTime.now(ZONE_SHANGHAI));
        attendanceMapper.update(att);

        // 3. 启动 Flowable 工作流实例
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("attendanceId", attendanceId);
            variables.put("applicantId", employeeId);
            variables.put("applicantRole", applicantRole);
            variables.put("departmentId", att.getDepartmentId());
            variables.put("reason", reason.trim());

            org.flowable.engine.runtime.ProcessInstance pi = runtimeService.startProcessInstanceByKey(
                    "attendanceReplenishmentProcess",
                    String.valueOf(attendanceId),
                    variables
            );
            log.info("Flowable 考勤补签流程成功启动: processInstanceId={}, businessKey={}", pi.getId(), attendanceId);
        } catch (Exception e) {
            log.error("Flowable 启动补签流程实例失败, attendanceId={}", attendanceId, e);
        }

        clearPersonalCache(employeeId);
        clearAdminCache();

        return convertToResponse(attendanceMapper.selectById(attendanceId));
    }

    @Override
    @Transactional
    public AttendanceResponse approveReplenishment(Long attendanceId, Long approverUserId, Long approverEmployeeId, String approverRole, boolean approved, String comment) {
        OaAttendance att = attendanceMapper.selectById(attendanceId);
        if (att == null) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "考勤记录不存在");
        }
        if (!"PENDING".equals(att.getReplenishStatus())) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "该记录当前不在待补签状态");
        }

        if (approverEmployeeId != null && approverEmployeeId.equals(att.getEmployeeId())) {
            throw new BusinessException(403, HttpStatus.FORBIDDEN, "权限不足：不能审批自己的补签申请");
        }

        if (!"SUPER_ADMIN".equalsIgnoreCase(approverRole) && !"DEPT_MANAGER".equalsIgnoreCase(approverRole)) {
            throw new BusinessException(403, HttpStatus.FORBIDDEN, "权限不足：只有管理员或部门经理可审批补签");
        }

        // 1. 查询 Flowable 当前待办任务 Task
        org.flowable.task.api.Task currentTask = null;
        try {
            currentTask = taskService.createTaskQuery()
                    .processInstanceBusinessKey(String.valueOf(attendanceId))
                    .singleResult();
        } catch (Exception e) {
            log.warn("Flowable 查询 Task 失败: businessKey={}", attendanceId, e);
        }

        String action = approved ? "APPROVED" : "REJECTED";
        if (comment != null && (comment.contains("呈报") || comment.contains("上报高管"))) {
            action = "ESCALATE";
        }

        // 2. 推动 Flowable 工作流任务完成
        if (currentTask != null) {
            Map<String, Object> taskVars = new HashMap<>();
            taskVars.put("action", action);
            taskVars.put("approverUserId", approverUserId);
            taskVars.put("approveComment", comment);

            taskService.complete(currentTask.getId(), taskVars);
            log.info("Flowable 考勤补签 Task 完成: taskId={}, action={}", currentTask.getId(), action);
        } else {
            // 降级兜底逻辑（若流程死锁或无任务时回落直接更新）
            LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);
            if (approved) {
                att.setReplenishStatus("APPROVED");
                att.setStatus("REPLENISHED");
            } else {
                att.setReplenishStatus("REJECTED");
            }
            att.setApproverId(approverUserId);
            att.setApproveTime(now);
            att.setApproveComment(comment != null ? comment.trim() : "");
            att.setUpdateTime(now);
            attendanceMapper.update(att);
        }

        clearPersonalCache(att.getEmployeeId());
        clearAdminCache();

        return convertToResponse(attendanceMapper.selectById(attendanceId));
    }

    @Override
    public AttendancePageResult queryReplenishRecords(Long departmentId, Long employeeId, String replenishStatus, PageQuery query, Long operatorDeptId, String operatorRole) {
        if ("DEPT_MANAGER".equalsIgnoreCase(operatorRole)) {
            if (departmentId == null) {
                departmentId = operatorDeptId;
            }
        }
        List<OaAttendance> list = attendanceMapper.selectReplenishPageList(departmentId, employeeId, replenishStatus, query.offset(), query.size());
        long total = attendanceMapper.selectReplenishCount(departmentId, employeeId, replenishStatus);
        List<AttendanceResponse> responseList = list.stream().map(this::convertToResponse).collect(Collectors.toList());

        return AttendancePageResult.of(responseList, total, query, new HashMap<>());
    }

    // ================= 查询与缓存辅助方法 =================

    @Override
    public AttendancePageResult queryPersonalRecords(Long employeeId, String startDate, String endDate, PageQuery query, Long requestEmployeeId) {
        LocalDate today = LocalDate.now(ZONE_SHANGHAI);
        if (startDate == null || startDate.trim().isEmpty()) {
            startDate = today.minusDays(30).toString();
        }
        if (endDate == null || endDate.trim().isEmpty()) {
            endDate = today.toString();
        }

        List<OaAttendance> list = attendanceMapper.selectPageList(employeeId, null, startDate, endDate, null, query.offset(), query.size());
        long total = attendanceMapper.selectCount(employeeId, null, startDate, endDate, null);

        List<AttendanceResponse> responseList = list.stream().map(this::convertToResponse).collect(Collectors.toList());
        return AttendancePageResult.of(responseList, total, query, new HashMap<>());
    }

    @Override
    public AttendancePageResult queryAdminRecords(Long employeeId, Long departmentId, String startDate, String endDate, String status, PageQuery query) {
        List<OaAttendance> list = attendanceMapper.selectPageList(employeeId, departmentId, startDate, endDate, status, query.offset(), query.size());
        long total = attendanceMapper.selectCount(employeeId, departmentId, startDate, endDate, status);
        List<AttendanceResponse> responseList = list.stream().map(this::convertToResponse).collect(Collectors.toList());
        return AttendancePageResult.of(responseList, total, query, new HashMap<>());
    }

    private AttendanceResponse convertToResponse(OaAttendance attendance) {
        if (attendance == null) return null;
        String sName = attendance.getSessionName();
        if (sName == null || sName.isBlank() || "默认场次".equals(sName)) {
            sName = "上午场";
        }
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getEmployeeId(),
                attendance.getEmployeeName(),
                attendance.getDepartmentId(),
                attendance.getDepartmentName(),
                attendance.getWorkDate(),
                sName,
                attendance.getCheckIn(),
                attendance.getCheckOut(),
                attendance.getCheckInIp(),
                attendance.getCheckOutIp(),
                attendance.getCheckInStartTime(),
                attendance.getNormalCheckInEndTime(),
                attendance.getCheckInEndTime(),
                attendance.getNormalCheckOutStartTime(),
                attendance.getCheckOutEndTime(),
                attendance.getStatus(),
                attendance.getReplenishStatus(),
                attendance.getReplenishReason(),
                attendance.getApproverId(),
                attendance.getApproverName(),
                attendance.getApproveTime(),
                attendance.getApproveComment(),
                attendance.getCreateTime(),
                attendance.getUpdateTime()
        );
    }

    private void clearPersonalCache(Long employeeId) {
        try {
            Set<String> keys = redisTemplate.keys("oa:attendance:personal:" + employeeId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ignored) {}
    }

    private void clearAdminCache() {
        try {
            Set<String> keys = redisTemplate.keys("oa:attendance:admin:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ignored) {}
    }

    @Override
    @Transactional
    public AttendanceResponse saveOrUpdateAdminRecord(OaAttendance record) {
        return updateAdminRecord(record.getId(), record);
    }

    @Override
    @Transactional
    public AttendanceResponse updateAdminRecord(Long id, OaAttendance record) {
        OaAttendance existing = attendanceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "考勤记录不存在");
        }
        LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);
        if (record.getStatus() != null) existing.setStatus(record.getStatus());
        if (record.getCheckIn() != null) existing.setCheckIn(record.getCheckIn());
        if (record.getCheckOut() != null) existing.setCheckOut(record.getCheckOut());
        existing.setUpdateTime(now);
        attendanceMapper.update(existing);
        return convertToResponse(attendanceMapper.selectById(id));
    }
}

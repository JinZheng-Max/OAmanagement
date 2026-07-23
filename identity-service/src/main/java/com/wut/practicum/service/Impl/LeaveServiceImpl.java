package com.wut.practicum.service.impl;
import com.wut.practicum.common.BusinessException;
import com.wut.practicum.dto.*;
import com.wut.practicum.entity.OaEmployee;
import com.wut.practicum.entity.OaLeave;
import com.wut.practicum.entity.OaLeaveAudit;
import com.wut.practicum.entity.SysDepartment;
import com.wut.practicum.mapper.DepartmentMapper;
import com.wut.practicum.mapper.EmployeeMapper;
import com.wut.practicum.mapper.LeaveMapper;
import com.wut.practicum.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveMapper leaveMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;

    @Override
    public PageResult<LeaveResponse> page(PageQuery query, Long currentUserId, String role, String status, String type) {
        Long applicantId = null;
        Long deptId = null;

        if ("SUPER_ADMIN".equals(role)) {
            // 超级管理员：看全部
        } else if ("DEPT_MANAGER".equals(role)) {
            // 部门管理员：看本部门员工的 + 自己的
            OaEmployee emp = employeeMapper.selectById(currentUserId);
            if (emp != null && emp.getDepartmentId() != null) {
                // 查找该部门负责人对应的部门
                SysDepartment dept = departmentMapper.selectByLeaderId(currentUserId);
                if (dept != null) {
                    deptId = dept.getId();
                } else {
                    // 如果没有找到对应的部门，只显示自己的
                    applicantId = currentUserId;
                }
            } else {
                applicantId = currentUserId;
            }
        } else {
            // 普通员工：只看自己的
            applicantId = currentUserId;
        }

        long total = leaveMapper.countPage(applicantId, status, type, deptId);
        if (total == 0) return PageResult.of(List.of(), 0, query);
        List<LeaveResponse> list = leaveMapper.selectPage(applicantId, status, type, query.offset(), query.size(), deptId).stream()
                .map(l -> LeaveResponse.from(l, leaveMapper.selectAuditsByLeaveId(l.getId())))
                .collect(Collectors.toList());
        return PageResult.of(list, total, query);
    }

    @Override
    public LeaveResponse getById(Long id) {
        OaLeave leave = leaveMapper.selectById(id);
        if (leave == null) throw new BusinessException(5001, HttpStatus.NOT_FOUND, "请假申请不存在");
        return LeaveResponse.from(leave, leaveMapper.selectAuditsByLeaveId(id));
    }

    @Override
    @Transactional
    public LeaveResponse create(Long applicantId, LeaveCreateRequest request) {
        OaLeave leave = new OaLeave();
        leave.setApplicantId(applicantId);
        leave.setType(request.type());
        leave.setStartTime(LocalDateTime.parse(request.startTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        leave.setEndTime(LocalDateTime.parse(request.endTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        leave.setReason(request.reason());
        leave.setStatus("PENDING");
        leaveMapper.insert(leave);
        log.info("请假申请提交: id={}, applicantId={}, type={}", leave.getId(), applicantId, request.type());
        return LeaveResponse.from(leave, List.of());
    }

    @Override
    @Transactional
    public void withdraw(Long id, Long applicantId) {
        OaLeave leave = leaveMapper.selectById(id);
        if (leave == null) throw new BusinessException(5001, HttpStatus.NOT_FOUND, "请假申请不存在");
        if (!leave.getApplicantId().equals(applicantId))
            throw new BusinessException(5002, HttpStatus.FORBIDDEN, "只能撤回自己的申请");
        if (!"PENDING".equals(leave.getStatus()))
            throw new BusinessException(5003, HttpStatus.BAD_REQUEST, "只能撤回待审批的申请");
        leaveMapper.updateStatus(id, "WITHDRAWN");
        log.info("请假申请撤回: id={}", id);
    }

    @Override
    @Transactional
    public void audit(Long id, Long auditorId, LeaveAuditRequest request) {
        OaLeave leave = leaveMapper.selectById(id);
        if (leave == null) throw new BusinessException(5001, HttpStatus.NOT_FOUND, "请假申请不存在");
        if (!"PENDING".equals(leave.getStatus()))
            throw new BusinessException(5004, HttpStatus.BAD_REQUEST, "该申请已被处理");

        String newStatus = leave.getStatus();
        if ("APPROVED".equals(request.action())) {
            newStatus = "APPROVED";
        } else if ("REJECTED".equals(request.action())) {
            newStatus = "REJECTED";
        } else if ("ESCALATE".equals(request.action()) || "TRANSFER".equals(request.action())) {
            newStatus = "PENDING";
        }
        leaveMapper.updateStatus(id, newStatus);

        OaLeaveAudit audit = new OaLeaveAudit();
        audit.setLeaveId(id);
        audit.setAuditorId(auditorId);
        audit.setAction(request.action());
        audit.setComment(request.comment());
        leaveMapper.insertAudit(audit);
        log.info("请假审批: id={}, action={}, auditorId={}", id, request.action(), auditorId);
    }
}

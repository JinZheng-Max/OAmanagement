package com.wut.practicum.service.impl;

import com.wut.practicum.common.BusinessException;
import com.wut.practicum.dto.*;
import com.wut.practicum.entity.OaEmployee;
import com.wut.practicum.entity.SysDepartment;
import com.wut.practicum.entity.SysUser;
import com.wut.practicum.mapper.DepartmentMapper;
import com.wut.practicum.mapper.EmployeeMapper;
import com.wut.practicum.mapper.UserMapper;
import com.wut.practicum.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;
    private final UserMapper userMapper;

    @Override
    public MyDepartmentResponse getMyDepartment(Long employeeId) {
        // 1. 查询当前员工信息（获取 departmentId）
        OaEmployee emp = employeeMapper.selectById(employeeId);
        if (emp == null || emp.getDepartmentId() == null) {
            throw new BusinessException(4001, HttpStatus.NOT_FOUND, "您尚未分配部门");
        }

        // 2. 查询部门信息
        SysDepartment dept = departmentMapper.selectById(emp.getDepartmentId());
        if (dept == null) {
            throw new BusinessException(3001, HttpStatus.NOT_FOUND, "部门不存在");
        }

        // 3. 查询该部门所有员工
        List<OaEmployee> emps = employeeMapper.selectByDepartmentId(emp.getDepartmentId());
        List<EmployeeResponse> empResponses = emps.stream()
                .map(EmployeeResponse::from).collect(Collectors.toList());

        // 4. 统计各岗位人数
        Map<String, Integer> positionCounts = new LinkedHashMap<>();
        for (OaEmployee e : emps) {
            String pos = (e.getPosition() != null && !e.getPosition().isBlank()) ? e.getPosition() : "未设职位";
            positionCounts.merge(pos, 1, Integer::sum);
        }

        // 5. 查找部门负责人信息
        EmployeeResponse leader = null;
        if (dept.getLeaderId() != null) {
            OaEmployee leaderEmp = employeeMapper.selectById(dept.getLeaderId());
            if (leaderEmp != null) {
                leader = EmployeeResponse.from(leaderEmp);
            }
        }

        return new MyDepartmentResponse(
                DepartmentResponse.from(dept),
                empResponses,
                emps.size(),
                positionCounts,
                leader);
    }

    @Override
    public PageResult<DepartmentResponse> page(PageQuery query, String keyword) {
        long total = departmentMapper.countPage(keyword);
        if (total == 0) return PageResult.of(List.of(), 0, query);
        List<DepartmentResponse> list = departmentMapper.selectPage(keyword, query.offset(), query.size())
                .stream().map(DepartmentResponse::from).collect(Collectors.toList());
        return PageResult.of(list, total, query);
    }

    @Override
    public DepartmentResponse getById(Long id) {
        SysDepartment dept = departmentMapper.selectById(id);
        if (dept == null) throw new BusinessException(3001, HttpStatus.NOT_FOUND, "部门不存在");
        return DepartmentResponse.from(dept);
    }

    @Override
    @Transactional
    public DepartmentResponse create(DepartmentCreateRequest request) {
        if (departmentMapper.selectByCode(request.code()) != null)
            throw new BusinessException(3002, HttpStatus.BAD_REQUEST, "部门编码已存在");

        SysDepartment dept = new SysDepartment();
        dept.setCode(request.code());
        dept.setName(request.name());
        dept.setLeaderId(request.leaderId());
        dept.setSort(request.sort() != null ? request.sort() : 0);
        dept.setStatus(1);
        departmentMapper.insert(dept);

        if (request.leaderId() != null) {
            updateUserRoleToDeptManager(request.leaderId());
        }

        log.info("新增部门: id={}, name={}", dept.getId(), dept.getName());
        return DepartmentResponse.from(dept);
    }

    @Override
    @Transactional
    public DepartmentResponse update(Long id, DepartmentUpdateRequest request) {
        SysDepartment dept = departmentMapper.selectById(id);
        if (dept == null) throw new BusinessException(3001, HttpStatus.NOT_FOUND, "部门不存在");
        
        Long oldLeaderId = dept.getLeaderId();
        Long newLeaderId = request.leaderId();

        if (request.code() != null) dept.setCode(request.code());
        if (request.name() != null) dept.setName(request.name());
        if (request.leaderId() != null) dept.setLeaderId(request.leaderId());
        if (request.sort() != null) dept.setSort(request.sort());
        departmentMapper.update(dept);

        if (newLeaderId != null && !newLeaderId.equals(oldLeaderId)) {
            // 1. 新负责人升级为 DEPT_MANAGER
            updateUserRoleToDeptManager(newLeaderId);

            // 2. 旧负责人若不再担任任何部门负责人，角色重置为 EMPLOYEE
            if (oldLeaderId != null) {
                checkAndResetOldLeaderRole(oldLeaderId);
            }
        }

        log.info("更新部门: id={}, leaderId={}", id, newLeaderId);
        return DepartmentResponse.from(departmentMapper.selectById(id));
    }

    private void updateUserRoleToDeptManager(Long employeeId) {
        if (employeeId == null) return;
        SysUser user = userMapper.selectByEmployeeId(employeeId);
        if (user != null) {
            if (!"SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {
                userMapper.updateRole(user.getId(), "DEPT_MANAGER");
                log.info("设置部门负责人成功，已自动升级关联用户(userId={}, employeeId={})的角色为 DEPT_MANAGER", user.getId(), employeeId);
            }
        }
    }

    private void checkAndResetOldLeaderRole(Long oldLeaderId) {
        if (oldLeaderId == null) return;
        SysDepartment deptAsLeader = departmentMapper.selectByLeaderId(oldLeaderId);
        if (deptAsLeader == null) {
            SysUser user = userMapper.selectByEmployeeId(oldLeaderId);
            if (user != null && "DEPT_MANAGER".equalsIgnoreCase(user.getRole())) {
                userMapper.updateRole(user.getId(), "EMPLOYEE");
                log.info("旧负责人(employeeId={})不再担任任何部门负责人，角色重置为 EMPLOYEE", oldLeaderId);
            }
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        SysDepartment dept = departmentMapper.selectById(id);
        if (dept == null) throw new BusinessException(3001, HttpStatus.NOT_FOUND, "部门不存在");
        departmentMapper.updateStatus(id, status);
        log.info("部门状态变更: id={}, status={}", id, status);
    }

    @Override
    public List<DepartmentResponse> listAllActive() {
        return departmentMapper.selectAllActive().stream()
                .map(DepartmentResponse::from).collect(Collectors.toList());
    }
}

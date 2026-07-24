package com.wut.practicum.service.impl;

import com.wut.practicum.dto.AdminDashboardVO;
import com.wut.practicum.entity.SysDepartment;
import com.wut.practicum.mapper.DepartmentMapper;
import com.wut.practicum.mapper.EmployeeMapper;
import com.wut.practicum.mapper.LeaveMapper;
import com.wut.practicum.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final LeaveMapper leaveMapper;

    @Override
    public AdminDashboardVO getAdminDashboardStats() {
        // 1. 在册员工总数与部门总数
        long employeeCount = employeeMapper.countPage(null, null, null, null);
        long departmentCount = departmentMapper.countPage(null);

        // 2. 全局待处理计数
        long pendingLeaveCount = leaveMapper.countPendingLeaves();
        long pendingRecordCount = leaveMapper.countPendingReplenishes();

        // 3. 各部门的分组聚合结果
        List<Map<String, Object>> rawDeptLeaveList = leaveMapper.selectDeptPendingLeaveCounts();
        List<Map<String, Object>> rawDeptReplenishList = leaveMapper.selectDeptPendingReplenishCounts();

        Map<Long, Long> deptLeaveMap = new HashMap<>();
        if (rawDeptLeaveList != null) {
            for (Map<String, Object> map : rawDeptLeaveList) {
                Long deptId = map.get("deptId") != null ? ((Number) map.get("deptId")).longValue() : null;
                Long count = map.get("count") != null ? ((Number) map.get("count")).longValue() : 0L;
                if (deptId != null) deptLeaveMap.put(deptId, count);
            }
        }

        Map<Long, Long> deptReplenishMap = new HashMap<>();
        if (rawDeptReplenishList != null) {
            for (Map<String, Object> map : rawDeptReplenishList) {
                Long deptId = map.get("deptId") != null ? ((Number) map.get("deptId")).longValue() : null;
                Long count = map.get("count") != null ? ((Number) map.get("count")).longValue() : 0L;
                if (deptId != null) deptReplenishMap.put(deptId, count);
            }
        }

        // 4. 查询全量部门拼装列表
        List<SysDepartment> depts = departmentMapper.selectPage(null, 0, 500);
        List<AdminDashboardVO.DeptStatItem> deptLeaves = new ArrayList<>();
        List<AdminDashboardVO.DeptStatItem> deptRecords = new ArrayList<>();

        if (depts != null) {
            for (SysDepartment dept : depts) {
                long leaveCnt = deptLeaveMap.getOrDefault(dept.getId(), 0L);
                long recordCnt = deptReplenishMap.getOrDefault(dept.getId(), 0L);

                deptLeaves.add(new AdminDashboardVO.DeptStatItem(
                        dept.getId(), dept.getName(), leaveCnt, leaveCnt > 0 ? "刚刚" : "正常"
                ));
                deptRecords.add(new AdminDashboardVO.DeptStatItem(
                        dept.getId(), dept.getName(), recordCnt, recordCnt > 0 ? "刚刚" : "正常"
                ));
            }
        }

        return new AdminDashboardVO(
                employeeCount, departmentCount, pendingLeaveCount, pendingRecordCount,
                deptLeaves, deptRecords
        );
    }
}

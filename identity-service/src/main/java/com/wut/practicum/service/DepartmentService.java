package com.wut.practicum.service;

import com.wut.practicum.dto.*;
import java.util.List;

public interface DepartmentService {
    /** 获取当前员工所属部门的详细信息 */
    MyDepartmentResponse getMyDepartment(Long employeeId);
    PageResult<DepartmentResponse> page(PageQuery query, String keyword);
    DepartmentResponse getById(Long id);
    DepartmentResponse create(DepartmentCreateRequest request);
    DepartmentResponse update(Long id, DepartmentUpdateRequest request);
    void updateStatus(Long id, Integer status);
    List<DepartmentResponse> listAllActive();
}

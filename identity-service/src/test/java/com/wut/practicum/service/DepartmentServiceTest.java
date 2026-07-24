package com.wut.practicum.service;

import com.wut.practicum.common.BusinessException;
import com.wut.practicum.dto.*;
import com.wut.practicum.entity.OaEmployee;
import com.wut.practicum.entity.SysDepartment;
import com.wut.practicum.entity.SysUser;
import com.wut.practicum.mapper.DepartmentMapper;
import com.wut.practicum.mapper.EmployeeMapper;
import com.wut.practicum.mapper.UserMapper;
import com.wut.practicum.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 部门管理单元测试 (UT-02)
 * 依据 《智办AI OA 单元测试模块内容说明书 V2.0》编写
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UT-02: 部门与员工模块单元测试")
public class DepartmentServiceTest {

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private SysDepartment sampleDept;

    @BeforeEach
    void setUp() {
        sampleDept = new SysDepartment();
        sampleDept.setId(1L);
        sampleDept.setCode("DEPT_IT");
        sampleDept.setName("技术部");
        sampleDept.setLeaderId(10L);
        sampleDept.setStatus(1);
        sampleDept.setSort(1);
    }

    @Nested
    @DisplayName("部门创建与编码约束校验")
    class DepartmentCreateTests {

        @Test
        @DisplayName("UT-02-01: 成功新增部门，且自动升级负责人角色为 DEPT_MANAGER")
        void shouldCreateDepartmentSuccessfully() {
            DepartmentCreateRequest req = new DepartmentCreateRequest("DEPT_DEV", "研发部", 10L, 2);
            when(departmentMapper.selectByCode("DEPT_DEV")).thenReturn(null);
            when(departmentMapper.insert(any(SysDepartment.class))).thenReturn(1);
            
            SysUser user = new SysUser();
            user.setId(100L);
            user.setRole("EMPLOYEE");
            when(userMapper.selectByEmployeeId(10L)).thenReturn(user);

            DepartmentResponse response = departmentService.create(req);

            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo("DEPT_DEV");
            assertThat(response.name()).isEqualTo("研发部");

            verify(departmentMapper).insert(any(SysDepartment.class));
            verify(userMapper).updateRole(100L, "DEPT_MANAGER");
        }

        @Test
        @DisplayName("UT-02-02: 当部门编码重复时抛出 400 业务异常")
        void shouldThrowExceptionWhenCodeExists() {
            DepartmentCreateRequest req = new DepartmentCreateRequest("DEPT_IT", "重复部门", null, 1);
            when(departmentMapper.selectByCode("DEPT_IT")).thenReturn(sampleDept);

            assertThatThrownBy(() -> departmentService.create(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("部门编码已存在");

            verify(departmentMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("部门安全删除拦截与级联关系保护")
    class DepartmentDeleteTests {

        @Test
        @DisplayName("UT-02-03: 部门仍有关联员工时阻止删除并提示友好错误")
        void shouldBlockDeleteWhenHasEmployees() {
            when(departmentMapper.selectById(1L)).thenReturn(sampleDept);
            
            OaEmployee emp = new OaEmployee();
            emp.setId(101L);
            emp.setName("张三");
            when(employeeMapper.selectByDepartmentId(1L)).thenReturn(List.of(emp));

            assertThatThrownBy(() -> departmentService.delete(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("删除失败：该部门下仍有 1 名关联员工");

            verify(departmentMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("UT-02-04: 无关联员工时成功删除部门并重置旧负责人角色")
        void shouldDeleteDepartmentSuccessfullyWhenNoEmployees() {
            when(departmentMapper.selectById(1L)).thenReturn(sampleDept);
            when(employeeMapper.selectByDepartmentId(1L)).thenReturn(Collections.emptyList());
            when(departmentMapper.selectByLeaderId(10L)).thenReturn(null);

            SysUser user = new SysUser();
            user.setId(200L);
            user.setRole("DEPT_MANAGER");
            when(userMapper.selectByEmployeeId(10L)).thenReturn(user);

            departmentService.delete(1L);

            verify(departmentMapper).deleteById(1L);
            verify(userMapper).updateRole(200L, "EMPLOYEE");
        }
    }
}

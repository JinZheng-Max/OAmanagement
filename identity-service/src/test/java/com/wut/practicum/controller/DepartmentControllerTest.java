package com.wut.practicum.controller;

import com.wut.practicum.common.GlobalExceptionHandler;
import com.wut.practicum.config.SecurityConfig;
import com.wut.practicum.dto.DepartmentResponse;
import com.wut.practicum.dto.PageQuery;
import com.wut.practicum.dto.PageResult;
import com.wut.practicum.filter.TraceIdFilter;
import com.wut.practicum.mapper.DepartmentMapper;
import com.wut.practicum.mapper.EmployeeMapper;
import com.wut.practicum.mapper.UserMapper;
import com.wut.practicum.security.CurrentUser;
import com.wut.practicum.security.JwtAuthenticationFilter;
import com.wut.practicum.security.JwtService;
import com.wut.practicum.security.SessionStore;
import com.wut.practicum.service.DepartmentService;
import com.wut.practicum.service.EmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DepartmentController.class, properties = {
        "spring.config.import=", "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false"})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, TraceIdFilter.class, GlobalExceptionHandler.class, DepartmentControllerTest.MybatisMockConfig.class})
@DisplayName("UT-02: 部门 Controller WebMvc 交互与 MockitoBean 单元测试")
class DepartmentControllerTest {
    @org.springframework.boot.test.context.TestConfiguration
    static class MybatisMockConfig {
        @org.springframework.context.annotation.Bean
        public org.apache.ibatis.session.SqlSessionFactory sqlSessionFactory() {
            org.apache.ibatis.session.SqlSessionFactory factory = mock(org.apache.ibatis.session.SqlSessionFactory.class);
            org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
            org.apache.ibatis.mapping.Environment env = new org.apache.ibatis.mapping.Environment(
                    "development",
                    new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(),
                    mock(javax.sql.DataSource.class)
            );
            config.setEnvironment(env);
            when(factory.getConfiguration()).thenReturn(config);
            return factory;
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DepartmentService departmentService;

    @MockitoBean
    private SessionStore sessions;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private EmployeeMapper employeeMapper;

    @MockitoBean
    private DepartmentMapper departmentMapper;

    @MockitoBean
    private EmployeeService employeeService;

    private void authenticateAsAdmin() {
        when(jwtService.parse(anyString())).thenReturn(new CurrentUser(1L, "admin", "SUPER_ADMIN", null, "session-1"));
        when(sessions.isActive("session-1")).thenReturn(true);
    }

    @Test
    @DisplayName("UT-02-C01: 超级管理员可以成功调用 DELETE /api/departments/{id} 接口")
    void shouldAllowAdminToDeleteDepartment() throws Exception {
        authenticateAsAdmin();

        mvc.perform(delete("/api/departments/1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("部门删除成功"));
    }

    @Test
    @DisplayName("UT-02-C02: 分页查询部门列表正常返回数据格式")
    void shouldReturnDepartmentPage() throws Exception {
        authenticateAsAdmin();
        DepartmentResponse dept = new DepartmentResponse(1L, "DEPT_IT", "技术部", 10L, "张三", 5, 1, 1, null, null);
        PageQuery query = new PageQuery(1, 10);
        PageResult<DepartmentResponse> pageResult = PageResult.of(List.of(dept), 1L, query);
        
        when(departmentService.page(any(), any())).thenReturn(pageResult);

        mvc.perform(get("/api/departments")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].code").value("DEPT_IT"));
    }
}

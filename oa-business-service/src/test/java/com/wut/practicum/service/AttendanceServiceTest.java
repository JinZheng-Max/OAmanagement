package com.wut.practicum.service;

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
import com.wut.practicum.service.impl.AttendanceServiceImpl;
import com.wut.practicum.util.IpValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AttendanceServiceTest {

    @Mock
    private AttendanceMapper attendanceMapper;

    @Mock
    private EmployeeClient employeeClient;

    @Mock
    private IpValidator ipValidator;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    private AttendanceServiceImpl attendanceService;
    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(ipValidator.isValid(anyString())).thenReturn(true);
        attendanceService = new AttendanceServiceImpl(attendanceMapper, employeeClient, ipValidator, redisTemplate, objectMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(attendanceService, "checkInStartStr", "00:00");
        org.springframework.test.util.ReflectionTestUtils.setField(attendanceService, "checkInEndStr", "23:59");
        org.springframework.test.util.ReflectionTestUtils.setField(attendanceService, "checkOutStartStr", "00:00");
        org.springframework.test.util.ReflectionTestUtils.setField(attendanceService, "checkOutEndStr", "23:59");
    }

    @Test
    void testCheckIn_Success() {
        EmployeeResponse employee = new EmployeeResponse(1L, "EMP001", "张三", 10L, "技术部", "工程师", "13800000000", 1, LocalDate.now(), null, null);
        OaAttendance existing = new OaAttendance();
        existing.setId(100L);
        existing.setEmployeeId(1L);
        existing.setStatus("UNCHECKED");
        existing.setWorkDate(LocalDate.now().toString());
        existing.setEmployeeName("张三");

        when(employeeClient.getById(1L)).thenReturn(ApiResult.success(employee));
        when(ipValidator.isValid("192.168.1.100")).thenReturn(true);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(attendanceMapper.selectByEmployeeAndDate(eq(1L), anyString())).thenReturn(existing);

        AttendanceResponse response = attendanceService.checkIn(1L, "192.168.1.100");

        assertNotNull(response);
        assertEquals("CHECKED_IN", response.status());
        assertEquals("192.168.1.100", response.checkInIp());
        assertEquals("张三", response.employeeName());
        verify(attendanceMapper, times(1)).update(any());
    }

    @Test
    void testCheckIn_InvalidIp() {
        EmployeeResponse employee = new EmployeeResponse(1L, "EMP001", "张三", 10L, "技术部", "工程师", "13800000000", 1, LocalDate.now(), null, null);
        when(employeeClient.getById(1L)).thenReturn(ApiResult.success(employee));
        when(ipValidator.isValid("8.8.8.8")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            attendanceService.checkIn(1L, "8.8.8.8");
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("非公司内网"));
    }

    @Test
    void testCheckIn_Duplicate() {
        EmployeeResponse employee = new EmployeeResponse(1L, "EMP001", "张三", 10L, "技术部", "工程师", "13800000000", 1, LocalDate.now(), null, null);
        OaAttendance existing = new OaAttendance();
        existing.setId(100L);
        existing.setEmployeeId(1L);
        existing.setStatus("CHECKED_IN");
        existing.setWorkDate(LocalDate.now().toString());

        when(employeeClient.getById(1L)).thenReturn(ApiResult.success(employee));
        when(ipValidator.isValid("192.168.1.100")).thenReturn(true);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(attendanceMapper.selectByEmployeeAndDate(eq(1L), anyString())).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            attendanceService.checkIn(1L, "192.168.1.100");
        });

        assertEquals(40910, exception.getCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertNotNull(exception.getData());
        AttendanceResponse respData = (AttendanceResponse) exception.getData();
        assertEquals("CHECKED_IN", respData.status());
    }

    @Test
    void testCheckOut_Success() {
        OaAttendance existing = new OaAttendance();
        existing.setId(100L);
        existing.setEmployeeId(1L);
        existing.setStatus("CHECKED_IN");
        existing.setWorkDate(LocalDate.now().toString());

        when(attendanceMapper.selectByEmployeeAndDate(eq(1L), anyString())).thenReturn(existing);

        AttendanceResponse response = attendanceService.checkOut(1L, "192.168.1.100");

        assertNotNull(response);
        assertEquals("CHECKED_OUT", response.status());
        assertEquals("192.168.1.100", response.checkOutIp());
        verify(attendanceMapper, times(1)).update(any());
    }

    @Test
    void testCheckOut_WithoutCheckIn() {
        when(attendanceMapper.selectByEmployeeAndDate(eq(1L), anyString())).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            attendanceService.checkOut(1L, "192.168.1.100");
        });

        assertEquals(40911, exception.getCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("未签到即签退", exception.getMessage());
    }

    @Test
    void testCheckOut_Duplicate() {
        OaAttendance existing = new OaAttendance();
        existing.setId(100L);
        existing.setEmployeeId(1L);
        existing.setStatus("CHECKED_OUT");
        existing.setWorkDate(LocalDate.now().toString());

        when(attendanceMapper.selectByEmployeeAndDate(eq(1L), anyString())).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            attendanceService.checkOut(1L, "192.168.1.100");
        });

        assertEquals(40911, exception.getCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("重复签退", exception.getMessage());
    }

    @Test
    void testQueryPersonalRecords_OverrideEmployeeId() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(attendanceMapper.selectPageList(anyLong(), any(), anyString(), anyString(), any(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(attendanceMapper.selectCount(anyLong(), any(), anyString(), anyString(), any()))
                .thenReturn(0L);
        when(attendanceMapper.countStatusByFilter(anyLong(), any(), anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            attendanceService.queryPersonalRecords(1L, "2026-07-01", "2026-07-15", new PageQuery(1, 10), 2L);
        });

        assertEquals(40302, exception.getCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertNotNull(exception.getData());
        AttendancePageResult result = (AttendancePageResult) exception.getData();
        assertEquals(0, result.total());
    }
}

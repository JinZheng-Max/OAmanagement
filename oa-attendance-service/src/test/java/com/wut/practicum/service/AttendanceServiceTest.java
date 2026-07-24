package com.wut.practicum.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wut.practicum.client.EmployeeClient;
import com.wut.practicum.common.BusinessException;
import com.wut.practicum.dto.AttendanceResponse;
import com.wut.practicum.dto.EmployeeResponse;
import com.wut.practicum.entity.OaAttendance;
import com.wut.practicum.mapper.AttendanceMapper;
import com.wut.practicum.mapper.OaAttendanceRuleMapper;
import com.wut.practicum.service.impl.AttendanceServiceImpl;
import com.wut.practicum.util.IpValidator;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 考勤打卡与结算单元测试 (UT-04 / UT-05)
 * 依据 《智办AI OA 单元测试模块内容说明书 V2.0》编写
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UT-05: 考勤打卡与结算模块单元测试")
public class AttendanceServiceTest {

    @Mock
    private AttendanceMapper attendanceMapper;

    @Mock
    private OaAttendanceRuleMapper oaAttendanceRuleMapper;

    @Mock
    private EmployeeClient employeeClient;

    @Mock
    private IpValidator ipValidator;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private OaAttendance sampleRecord;

    @BeforeEach
    void setUp() {
        sampleRecord = new OaAttendance();
        sampleRecord.setId(219L);
        sampleRecord.setEmployeeId(69L);
        sampleRecord.setWorkDate(LocalDate.now().toString());
        sampleRecord.setSessionName("上午场");
        sampleRecord.setCheckInStartTime("00:00");
        sampleRecord.setNormalCheckInEndTime("23:59");
        sampleRecord.setCheckInEndTime("23:59");
        sampleRecord.setStatus("UNCHECKED");
        sampleRecord.setReplenishStatus("NONE");

        lenient().when(ipValidator.isValid(anyString())).thenReturn(true);
        EmployeeResponse emp = new EmployeeResponse(69L, "EMP001", "孙杰", 1L, "行政部", "专员", "13800138000", 1, LocalDate.now(), null, null);
        EmployeeResponse empOther = new EmployeeResponse(999L, "EMP999", "路人", 1L, "行政部", "专员", "13800138000", 1, LocalDate.now(), null, null);
        
        lenient().when(employeeClient.getById(anyLong())).thenAnswer(invocation -> {
            Long arg = invocation.getArgument(0);
            if (Long.valueOf(999L).equals(arg)) {
                return com.wut.practicum.common.ApiResult.success(empOther);
            }
            return com.wut.practicum.common.ApiResult.success(emp);
        });

        org.springframework.data.redis.core.ValueOperations valueOps = mock(org.springframework.data.redis.core.ValueOperations.class);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class))).thenReturn(true);
    }

    @Nested
    @DisplayName("签到打卡业务测试")
    class CheckInTests {

        @Test
        @DisplayName("UT-05-01: 正常时间段内完成签到打卡，状态更新为 CHECKED_IN")
        void shouldCheckInSuccessfully() {
            when(attendanceMapper.selectById(219L)).thenReturn(sampleRecord);

            AttendanceResponse response = attendanceService.checkIn(69L, 219L, "127.0.0.1");

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(219L);
            verify(attendanceMapper).update(any(OaAttendance.class));
        }

        @Test
        @DisplayName("UT-05-02: 重复点击已签到卡片时拒绝操作，抛出防重拦截异常")
        void shouldPreventDuplicateCheckIn() {
            sampleRecord.setStatus("CHECKED_IN");
            sampleRecord.setCheckIn(LocalDateTime.now());
            when(attendanceMapper.selectById(219L)).thenReturn(sampleRecord);

            assertThatThrownBy(() -> attendanceService.checkIn(69L, 219L, "127.0.0.1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("您已经完成签到，请勿重复操作");

            verify(attendanceMapper, never()).update(any());
        }

        @Test
        @DisplayName("UT-05-03: 跨员工非法打卡拦截，抛出 403 权限越界异常")
        void shouldBlockCheckInForOtherEmployee() {
            when(attendanceMapper.selectById(219L)).thenReturn(sampleRecord);

            assertThatThrownBy(() -> attendanceService.checkIn(999L, 219L, "127.0.0.1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权操作他人考勤记录");

            verify(attendanceMapper, never()).update(any());
        }
    }
}

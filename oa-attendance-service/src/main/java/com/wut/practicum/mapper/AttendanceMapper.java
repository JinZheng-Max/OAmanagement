package com.wut.practicum.mapper;

import com.wut.practicum.entity.OaAttendance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AttendanceMapper {
    int insert(OaAttendance attendance);
    
    int update(OaAttendance attendance);
    
    OaAttendance selectByEmployeeAndDate(@Param("employeeId") Long employeeId, @Param("workDate") String workDate);
    
    OaAttendance selectById(@Param("id") Long id);
    
    List<OaAttendance> selectPageList(@Param("employeeId") Long employeeId,
                                      @Param("departmentId") Long departmentId,
                                      @Param("startDate") String startDate,
                                      @Param("endDate") String endDate,
                                      @Param("status") String status,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);
                                      
    long selectCount(@Param("employeeId") Long employeeId,
                     @Param("departmentId") Long departmentId,
                     @Param("startDate") String startDate,
                     @Param("endDate") String endDate,
                     @Param("status") String status);
                     
    List<Map<String, Object>> countStatusByFilter(@Param("employeeId") Long employeeId,
                                                  @Param("departmentId") Long departmentId,
                                                  @Param("startDate") String startDate,
                                                  @Param("endDate") String endDate,
                                                  @Param("status") String status);

    List<Long> selectActiveEmployeeIds();
}

package com.wut.practicum.mapper;

import com.wut.practicum.entity.OaAttendanceRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OaAttendanceRuleMapper {
    List<OaAttendanceRule> selectByDepartmentId(@Param("departmentId") Long departmentId);
    List<OaAttendanceRule> selectAllEnabled();
    OaAttendanceRule selectById(@Param("id") Long id);
    OaAttendanceRule selectByDeptAndSession(@Param("departmentId") Long departmentId, @Param("sessionName") String sessionName);
    int insert(OaAttendanceRule rule);
    int update(OaAttendanceRule rule);
    int deleteById(@Param("id") Long id);
    void createTableIfNotExists();
}

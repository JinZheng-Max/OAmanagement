package com.wut.practicum.mapper;

import com.wut.practicum.entity.SysDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DepartmentMapper {
    List<SysDepartment> selectPage(@Param("keyword") String keyword,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);
    long countPage(@Param("keyword") String keyword);
    SysDepartment selectById(@Param("id") Long id);
    SysDepartment selectByCode(@Param("code") String code);
    int insert(SysDepartment dept);
    int update(SysDepartment dept);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    List<SysDepartment> selectAllActive();
}

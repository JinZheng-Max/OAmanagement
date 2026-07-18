package com.wut.practicum.mapper;

import com.wut.practicum.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户信息
     */
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 根据用户ID查询用户及关联员工档案信息
     */
    SysUser selectUserWithEmployeeById(@Param("id") Long id);

    /**
     * 更新用户密码
     */
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    /**
     * 根据员工ID查询用户（用于判断员工是否已有账号）
     */
    SysUser selectByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * 新增用户
     */
    int insertUser(SysUser sysUser);
}

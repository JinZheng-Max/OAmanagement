package com.wut.practicum.mapper;

import com.wut.practicum.entity.OaEmployee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 员工档案 数据访问层
 *
 * 这里定义了对 oa_employee 表的操作方法，
 * MyBatis 会根据 EmployeeMapper.xml 中的 SQL 映射来执行实际查询。
 */
@Mapper
public interface EmployeeMapper {

    /**
     * 分页查询员工列表（支持关键词搜索和部门筛选）
     *
     * @param keyword      搜索关键词（按姓名/工号/手机号模糊匹配）
     * @param departmentId 部门ID筛选（null 表示不筛选）
     * @param offset       分页偏移量（从第几条开始）
     * @param limit        每页大小（取多少条）
     * @return 员工列表
     */
    List<OaEmployee> selectPage(@Param("keyword") String keyword,
                                @Param("departmentId") Long departmentId,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    /**
     * 查询符合条件的员工总数（用于分页计算总页数）
     */
    long countPage(@Param("keyword") String keyword,
                   @Param("departmentId") Long departmentId);

    /**
     * 根据ID查询员工（包含部门名称）
     */
    OaEmployee selectById(@Param("id") Long id);

    /**
     * 根据工号查询员工（用于校验工号是否唯一）
     */
    OaEmployee selectByEmployeeNo(@Param("employeeNo") String employeeNo);

    /**
     * 新增员工
     *
     * @param employee 员工对象
     * @return 受影响行数（成功为1）
     */
    int insert(OaEmployee employee);

    /**
     * 更新员工信息
     *
     * @param employee 要更新的员工数据
     * @return 受影响行数
     */
    int update(OaEmployee employee);

    /**
     * 查询指定部门的员工列表（用于下拉选择等场景）
     */
    List<OaEmployee> selectByDepartmentId(@Param("departmentId") Long departmentId);
}

package com.wut.practicum.service;

import com.wut.practicum.dto.*;
import java.util.List;

/**
 * 员工管理 业务接口
 *
 * 定义员工管理的业务方法，实现类负责写具体逻辑。
 * 使用接口的目的是方便替换实现和单元测试。
 */
public interface EmployeeService {

    /**
     * 分页查询员工列表
     *
     * @param name       按姓名模糊搜索（可选）
     * @param employeeNo 按工号模糊搜索（可选）
     * @param phone      按手机号模糊搜索（可选）
     */
    PageResult<EmployeeResponse> page(PageQuery query, Long departmentId,
                                      String name, String employeeNo, String phone);

    /**
     * 查询员工详情
     */
    EmployeeResponse getById(Long id);

    /**
     * 新增员工
     */
    EmployeeResponse create(EmployeeCreateRequest request);

    /**
     * 更新员工信息
     */
    EmployeeResponse update(Long id, EmployeeUpdateRequest request);

    /**
     * 为员工开通系统账号
     *
     * @param employeeId 员工ID
     * @param request    账号信息（用户名可选）
     * @return 返回生成的临时密码
     */
    String createAccount(Long employeeId, CreateAccountRequest request);

    /**
     * 重置用户密码（管理员操作）
     *
     * @param userId 系统用户ID
     * @return 新的临时密码
     */
    String resetPassword(Long userId);

    /**
     * 按部门ID查询员工（用于部门管理展示部门人员）
     */
    List<EmployeeResponse> listByDepartment(Long departmentId);

    /**
     * 员工自助修改个人信息
     * 仅允许修改姓名和手机号
     *
     * @param employeeId 员工ID
     * @param request    要修改的信息
     * @return 更新后的员工信息
     */
    EmployeeResponse updateProfile(Long employeeId, EmployeeProfileUpdateRequest request);

    /**
     * 批量导入员工 (支持 Excel 和 CSV)
     */
    EmployeeImportResultVO importEmployees(org.springframework.web.multipart.MultipartFile file);
}

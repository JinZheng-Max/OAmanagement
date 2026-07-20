package com.wut.practicum.service.Impl;

import com.wut.practicum.common.BusinessException;
import com.wut.practicum.dto.*;
import com.wut.practicum.entity.OaEmployee;
import com.wut.practicum.entity.SysUser;
import com.wut.practicum.mapper.EmployeeMapper;
import com.wut.practicum.mapper.UserMapper;
import com.wut.practicum.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 员工管理 业务实现
 *
 * @RequiredArgsConstructor 是 Lombok 注解，为所有 final 字段生成构造器
 * Spring 会自动通过这个构造器注入依赖（称为构造器注入）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 安全随机数生成器（用于生成临时密码）
     * SecureRandom 比 Random 更安全，适合密码场景
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public PageResult<EmployeeResponse> page(PageQuery query, Long departmentId,
                                             String name, String employeeNo, String phone) {
        // 1. 查询总数
        long total = employeeMapper.countPage(name, employeeNo, phone, departmentId);

        // 2. 如果总数=0，直接返回空列表，不用查数据库
        if (total == 0) {
            return PageResult.of(List.of(), 0, query);
        }

        // 3. 查询当前页数据
        List<OaEmployee> list = employeeMapper.selectPage(name, employeeNo, phone, departmentId, query.offset(), query.size());

        // 4. 将实体列表转换为 DTO 列表
        // stream() 是 Java 8 的流式操作，map() 做转换，collect() 收集为 List
        List<EmployeeResponse> records = list.stream()
                .map(EmployeeResponse::from)
                .collect(Collectors.toList());

        // 5. 返回分页结果
        return PageResult.of(records, total, query);
    }

    @Override
    public EmployeeResponse getById(Long id) {
        OaEmployee emp = employeeMapper.selectById(id);
        if (emp == null) {
            throw new BusinessException(2001, HttpStatus.NOT_FOUND, "员工不存在");
        }
        return EmployeeResponse.from(emp);
    }

    @Override
    @Transactional // 事务：这个方法里的数据库操作要么全成功，要么全回滚
    public EmployeeResponse create(EmployeeCreateRequest request) {
        // 1. 校验工号唯一性
        OaEmployee exist = employeeMapper.selectByEmployeeNo(request.employeeNo());
        if (exist != null) {
            throw new BusinessException(2002, HttpStatus.BAD_REQUEST, "工号已存在");
        }

        // 2. 创建实体对象，把请求数据复制进去
        OaEmployee emp = new OaEmployee();
        emp.setEmployeeNo(request.employeeNo());
        emp.setName(request.name());
        emp.setDepartmentId(request.departmentId());
        emp.setPosition(request.position());
        emp.setPhone(request.phone());

        // 入职日期：如果没填则默认为当天
        if (request.hireDate() != null && !request.hireDate().isBlank()) {
            emp.setHireDate(LocalDate.parse(request.hireDate()));
        } else {
            emp.setHireDate(LocalDate.now());
        }

        emp.setStatus(1); // 默认为在职

        // 3. 插入数据库
        employeeMapper.insert(emp);

        log.info("新增员工: id={}, name={}, employeeNo={}", emp.getId(), emp.getName(), emp.getEmployeeNo());

        // 4. 返回创建后的员工信息（包含自增的ID）
        return EmployeeResponse.from(emp);
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long id, EmployeeUpdateRequest request) {
        // 1. 检查员工是否存在
        OaEmployee emp = employeeMapper.selectById(id);
        if (emp == null) {
            throw new BusinessException(2001, HttpStatus.NOT_FOUND, "员工不存在");
        }

        // 2. 只更新有值的字段（request 中非 null 的字段）
        if (request.name() != null) emp.setName(request.name());
        if (request.departmentId() != null) emp.setDepartmentId(request.departmentId());
        if (request.position() != null) emp.setPosition(request.position());
        if (request.phone() != null) emp.setPhone(request.phone());
        if (request.status() != null) emp.setStatus(request.status());
        if (request.hireDate() != null && !request.hireDate().isBlank()) {
            emp.setHireDate(LocalDate.parse(request.hireDate()));
        }

        // 3. 更新数据库
        employeeMapper.update(emp);

        log.info("更新员工: id={}", id);

        // 4. 重新查询返回最新数据（包含数据库自动更新的 update_time）
        return EmployeeResponse.from(employeeMapper.selectById(id));
    }

    @Override
    @Transactional
    public String createAccount(Long employeeId, CreateAccountRequest request) {
        // 1. 检查员工是否存在
        OaEmployee emp = employeeMapper.selectById(employeeId);
        if (emp == null) {
            throw new BusinessException(2001, HttpStatus.NOT_FOUND, "员工不存在");
        }

        // 2. 检查员工是否已有账号
        SysUser existUser = userMapper.selectByEmployeeId(employeeId);
        if (existUser != null) {
            throw new BusinessException(2007, HttpStatus.BAD_REQUEST, "该员工已有系统账号");
        }

        // 3. 确定用户名：优先使用请求中的，否则默认用员工工号
        String username = (request.username() != null && !request.username().isBlank())
                ? request.username()
                : emp.getEmployeeNo();

        // 4. 检查用户名是否被占用
        SysUser userByUsername = userMapper.selectByUsername(username);
        if (userByUsername != null) {
            throw new BusinessException(2008, HttpStatus.BAD_REQUEST, "用户名已被使用");
        }

        // 5. 生成随机临时密码（8位：字母+数字）
        String tempPassword = generateRandomPassword();

        // 6. 创建系统用户
        SysUser newUser = new SysUser();
        newUser.setUsername(username);
        newUser.setPasswordHash(passwordEncoder.encode(tempPassword)); // BCrypt加密
        newUser.setRole("EMPLOYEE");
        newUser.setStatus(1); // 启用
        newUser.setEmployeeId(employeeId);

        userMapper.insertUser(newUser);

        log.info("开通员工账号: employeeId={}, username={}, userId={}", employeeId, username, newUser.getId());

        return tempPassword;
    }

    @Override
    @Transactional
    public String resetPassword(Long userId) {
        // 1. 检查用户是否存在
        // 这里直接用 selectUserWithEmployeeById 检查用户是否存在
        SysUser user = userMapper.selectUserWithEmployeeById(userId);
        if (user == null) {
            throw new BusinessException(1004, HttpStatus.NOT_FOUND, "用户不存在");
        }

        // 2. 生成新密码
        String newPassword = generateRandomPassword();

        // 3. 更新密码哈希
        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));

        log.info("重置用户密码: userId={}", userId);

        return newPassword;
    }

    /**
     * 生成8位随机密码（包含大小写字母和数字）
     * 这是私有方法，只在本类内部使用
     */
    private String generateRandomPassword() {
        // 密码字符池：大写字母 + 小写字母 + 数字
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            // SecureRandom.nextInt(n) 返回 0 到 n-1 之间的随机整数
            password.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return password.toString();
    }
}

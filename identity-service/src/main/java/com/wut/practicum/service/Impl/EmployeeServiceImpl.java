package com.wut.practicum.service.impl;

import com.wut.practicum.common.BusinessException;
import com.wut.practicum.dto.*;
import com.wut.practicum.entity.OaEmployee;
import com.wut.practicum.entity.SysDepartment;
import com.wut.practicum.entity.SysUser;
import com.wut.practicum.mapper.DepartmentMapper;
import com.wut.practicum.mapper.EmployeeMapper;
import com.wut.practicum.mapper.UserMapper;
import com.wut.practicum.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private final DepartmentMapper departmentMapper;
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
    public List<EmployeeResponse> listByDepartment(Long departmentId) {
        return employeeMapper.selectByDepartmentId(departmentId).stream()
                .map(EmployeeResponse::from).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        // 工号统一 Smart 开头并自动生成
        String empNo = request.employeeNo();
        if (empNo == null || empNo.isBlank()) {
            empNo = generateAutoEmployeeNo();
        } else if (!empNo.toUpperCase().startsWith("SMART")) {
            empNo = "Smart" + empNo;
        }

        OaEmployee exist = employeeMapper.selectByEmployeeNo(empNo);
        if (exist != null) {
            empNo = generateAutoEmployeeNo();
        }

        // 2. 创建实体对象，把请求数据复制进去
        OaEmployee emp = new OaEmployee();
        emp.setEmployeeNo(empNo);
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

    private String generateAutoEmployeeNo() {
        long count = employeeMapper.countPage(null, null, null, null);
        long seq = count + 1;
        String candidate = String.format("Smart%04d", seq);
        int attempts = 0;
        while (employeeMapper.selectByEmployeeNo(candidate) != null && attempts < 1000) {
            seq++;
            candidate = String.format("Smart%04d", seq);
            attempts++;
        }
        return candidate;
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

        // 5. 初次开通时密码全部统一默认 123456
        String tempPassword = "123456";

        // 6. 创建系统用户
        SysUser newUser = new SysUser();
        newUser.setUsername(username);
        newUser.setPasswordHash(passwordEncoder.encode(tempPassword)); // BCrypt加密

        String targetRole = "EMPLOYEE";
        if (request.role() != null && !request.role().isBlank()) {
            String r = request.role().trim().toUpperCase();
            if (List.of("SUPER_ADMIN", "DEPT_MANAGER", "EMPLOYEE", "ADMIN").contains(r)) {
                targetRole = r;
            }
        }
        // 自动识别：若该员工已经是某个部门的负责人，且选择的角色不是超级管理员，自动升为 DEPT_MANAGER
        if (!"SUPER_ADMIN".equalsIgnoreCase(targetRole) && departmentMapper.selectByLeaderId(employeeId) != null) {
            targetRole = "DEPT_MANAGER";
        }

        newUser.setRole(targetRole);
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

    @Override
    @Transactional
    public EmployeeResponse updateProfile(Long employeeId, EmployeeProfileUpdateRequest request) {
        OaEmployee emp = employeeMapper.selectById(employeeId);
        if (emp == null) {
            throw new BusinessException(2001, HttpStatus.NOT_FOUND, "员工不存在");
        }

        // 仅允许修改姓名和手机号
        if (request.name() != null && !request.name().isBlank()) {
            emp.setName(request.name());
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            emp.setPhone(request.phone());
        }

        employeeMapper.update(emp);
        log.info("员工自助更新信息: employeeId={}", employeeId);
        return EmployeeResponse.from(employeeMapper.selectById(employeeId));
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

    @Override
    @Transactional
    public EmployeeImportResultVO importEmployees(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "请选择要上传的 Excel 或 CSV 文件");
        }
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        List<String[]> rows = new ArrayList<>();

        try {
            if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                rows = parseExcel(file.getInputStream());
            } else if (fileName.endsWith(".csv")) {
                rows = parseCsv(file.getInputStream());
            } else {
                throw new BusinessException(400, HttpStatus.BAD_REQUEST, "仅支持 .xlsx, .xls 和 .csv 格式的文件");
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("解析导入文件失败", e);
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "解析文件失败: " + e.getMessage());
        }

        if (rows.isEmpty()) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "文件内容为空");
        }

        // 判断第 0 行是否为表头 (如 姓名, 部门, 职位, 电话号码)
        int startRow = 0;
        String[] header = rows.get(0);
        if (header.length > 0 && (header[0].contains("姓名") || header[0].contains("Name") || header[0].contains("部门"))) {
            startRow = 1;
        }

        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        List<String> successDetails = new ArrayList<>();

        for (int i = startRow; i < rows.size(); i++) {
            int lineNum = i + 1;
            String[] row = rows.get(i);
            if (row == null || row.length == 0) continue;

            String name = getCell(row, 0);
            String deptName = getCell(row, 1);
            String position = getCell(row, 2);
            String phone = getCell(row, 3);

            if ((name == null || name.isBlank()) && (phone == null || phone.isBlank())) {
                continue;
            }

            if (name == null || name.isBlank()) {
                errors.add("第 " + lineNum + " 行：姓名不能为空");
                failCount++;
                continue;
            }

            // 匹配部门
            Long deptId = null;
            if (deptName != null && !deptName.isBlank()) {
                SysDepartment dept = departmentMapper.selectByName(deptName.trim());
                if (dept != null) {
                    deptId = dept.getId();
                } else {
                    SysDepartment newDept = new SysDepartment();
                    newDept.setCode("DEPT_" + System.currentTimeMillis() % 100000);
                    newDept.setName(deptName.trim());
                    newDept.setStatus(1);
                    newDept.setSort(99);
                    departmentMapper.insert(newDept);
                    deptId = newDept.getId();
                }
            }

            try {
                EmployeeCreateRequest req = new EmployeeCreateRequest(
                        null, name.trim(), deptId,
                        position != null && !position.isBlank() ? position.trim() : null,
                        phone != null && !phone.isBlank() ? phone.trim() : null,
                        null
                );
                EmployeeResponse created = create(req);

                // 导入后自动开通系统账号，角色默认 EMPLOYEE
                try {
                    String pwd = createAccount(created.id(), new CreateAccountRequest(created.employeeNo(), "EMPLOYEE"));
                    successDetails.add("员工【" + name.trim() + "】(" + created.employeeNo() + ") — 账号: " + created.employeeNo() + " | 初始密码: " + pwd);
                } catch (Exception acctErr) {
                    successDetails.add("员工【" + name.trim() + "】(" + created.employeeNo() + ") — 自动开通账号提示: " + acctErr.getMessage());
                }

                successCount++;
            } catch (Exception e) {
                failCount++;
                errors.add("第 " + lineNum + " 行 (" + name + ")：" + e.getMessage());
            }
        }

        return new EmployeeImportResultVO(rows.size() - startRow, successCount, failCount, errors, successDetails);
    }

    private String getCell(String[] row, int index) {
        if (index < row.length && row[index] != null) {
            String val = row[index].trim();
            if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                val = val.substring(1, val.length() - 1);
            }
            return val;
        }
        return "";
    }

    private List<String[]> parseExcel(InputStream is) throws Exception {
        List<String[]> list = new ArrayList<>();
        try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(is)) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                int lastCellNum = row.getLastCellNum();
                if (lastCellNum <= 0) continue;
                String[] cellArr = new String[Math.max(4, lastCellNum)];
                for (int c = 0; c < cellArr.length; c++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(c);
                    cellArr[c] = cell != null ? formatter.formatCellValue(cell).trim() : "";
                }
                list.add(cellArr);
            }
        }
        return list;
    }

    private List<String[]> parseCsv(InputStream is) throws Exception {
        List<String[]> list = new ArrayList<>();
        byte[] bytes = is.readAllBytes();
        String content;
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            content = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        } else {
            content = new String(bytes, StandardCharsets.UTF_8);
            if (content.contains("")) {
                content = new String(bytes, Charset.forName("GBK"));
            }
        }
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                list.add(tokens);
            }
        }
        return list;
    }
}

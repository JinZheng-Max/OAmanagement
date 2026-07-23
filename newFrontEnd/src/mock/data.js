export const mockUsers = {
  admin: {
    username: 'admin',
    password: '123456',
    token: 'eyJhbGciOiJIUzI1NiJ9.xxx.mocktoken',
    userInfo: {
      id: 1,
      username: 'admin',
      role: 'ADMIN',
      employeeId: null,
      departmentId: null
    }
  },
  deptadmin: {
    username: 'deptadmin',
    password: '123456',
    token: 'eyJhbGciOiJIUzI1NiJ9.zzz.mocktoken',
    userInfo: {
      id: 3,
      username: 'deptadmin',
      role: 'DEPT_ADMIN',
      employeeId: 5,
      departmentId: 1
    }
  },
  deptadmin2: {
    username: 'deptadmin2',
    password: '123456',
    token: 'eyJhbGciOiJIUzI1NiJ9.aaa.mocktoken',
    userInfo: {
      id: 4,
      username: 'deptadmin2',
      role: 'DEPT_ADMIN',
      employeeId: 18,
      departmentId: 2
    }
  },
  deptadmin3: {
    username: 'deptadmin3',
    password: '123456',
    token: 'eyJhbGciOiJIUzI1NiJ9.bbb.mocktoken',
    userInfo: {
      id: 5,
      username: 'deptadmin3',
      role: 'DEPT_ADMIN',
      employeeId: 28,
      departmentId: 3
    }
  },
  deptadmin4: {
    username: 'deptadmin4',
    password: '123456',
    token: 'eyJhbGciOiJIUzI1NiJ9.ccc.mocktoken',
    userInfo: {
      id: 6,
      username: 'deptadmin4',
      role: 'DEPT_ADMIN',
      employeeId: 38,
      departmentId: 4
    }
  },
  deptadmin5: {
    username: 'deptadmin5',
    password: '123456',
    token: 'eyJhbGciOiJIUzI1NiJ9.ddd.mocktoken',
    userInfo: {
      id: 7,
      username: 'deptadmin5',
      role: 'DEPT_ADMIN',
      employeeId: 48,
      departmentId: 5
    }
  },
  deptadmin6: {
    username: 'deptadmin6',
    password: '123456',
    token: 'eyJhbGciOiJIUzI1NiJ9.eee.mocktoken',
    userInfo: {
      id: 8,
      username: 'deptadmin6',
      role: 'DEPT_ADMIN',
      employeeId: 58,
      departmentId: 6
    }
  },
  employee: {
    username: 'employee',
    password: '123456',
    token: 'eyJhbGciOiJIUzI1NiJ9.yyy.mocktoken',
    userInfo: {
      id: 2,
      username: 'employee',
      role: 'USER',
      employeeId: 1,
      departmentId: 1
    }
  },
  employee2: {
    username: 'employee2',
    password: '123456',
    token: 'eyJhbGciOiJIUzI1NiJ9.fff.mocktoken',
    userInfo: {
      id: 9,
      username: 'employee2',
      role: 'USER',
      employeeId: 16,
      departmentId: 2
    }
  },
  employee3: {
    username: 'employee3',
    password: '123456',
    token: 'eyJhbGciOiJIUzI1NiJ9.ggg.mocktoken',
    userInfo: {
      id: 10,
      username: 'employee3',
      role: 'USER',
      employeeId: 26,
      departmentId: 3
    }
  }
}

export const mockDepartments = [
  {
    id: 1,
    code: 'DEPT_IT',
    name: '技术部',
    leaderId: 5,
    status: 1,
    sort: 1,
    createTime: '2026-07-17T11:00:00',
    updateTime: '2026-07-17T11:00:00'
  },
  {
    id: 2,
    code: 'DEPT_HR',
    name: '人事部',
    leaderId: 18,
    status: 1,
    sort: 2,
    createTime: '2026-07-17T11:00:00',
    updateTime: '2026-07-17T11:00:00'
  },
  {
    id: 3,
    code: 'DEPT_FIN',
    name: '财务部',
    leaderId: 28,
    status: 1,
    sort: 3,
    createTime: '2026-07-17T11:00:00',
    updateTime: '2026-07-17T11:00:00'
  },
  {
    id: 4,
    code: 'DEPT_MARKET',
    name: '市场部',
    leaderId: 38,
    status: 1,
    sort: 4,
    createTime: '2026-07-17T11:00:00',
    updateTime: '2026-07-17T11:00:00'
  },
  {
    id: 5,
    code: 'DEPT_PRODUCT',
    name: '产品部',
    leaderId: 48,
    status: 1,
    sort: 5,
    createTime: '2026-07-17T11:00:00',
    updateTime: '2026-07-17T11:00:00'
  },
  {
    id: 6,
    code: 'DEPT_OPERATION',
    name: '运营部',
    leaderId: 58,
    status: 1,
    sort: 6,
    createTime: '2026-07-17T11:00:00',
    updateTime: '2026-07-17T11:00:00'
  }
]

export const mockEmployees = [
  { id: 1, employeeNo: 'EMP001', name: '张三', departmentId: 1, departmentName: '技术部', position: '高级前端开发', email: 'zhangsan@company.com', phone: '13800000001', status: 'ACTIVE', hireDate: '2024-01-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 2, employeeNo: 'EMP002', name: '李四', departmentId: 1, departmentName: '技术部', position: '前端开发工程师', email: 'lisi@company.com', phone: '13800000002', status: 'ACTIVE', hireDate: '2024-03-20', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 3, employeeNo: 'EMP003', name: '王五', departmentId: 1, departmentName: '技术部', position: '后端开发工程师', email: 'wangwu@company.com', phone: '13800000003', status: 'ACTIVE', hireDate: '2023-06-10', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 4, employeeNo: 'EMP004', name: '赵六', departmentId: 1, departmentName: '技术部', position: '后端开发工程师', email: 'zhaoliu@company.com', phone: '13800000004', status: 'ACTIVE', hireDate: '2025-02-28', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 5, employeeNo: 'EMP005', name: '钱七', departmentId: 1, departmentName: '技术部', position: '技术经理', email: 'qianqi@company.com', phone: '13800000005', status: 'ACTIVE', hireDate: '2022-08-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 6, employeeNo: 'EMP006', name: '孙八', departmentId: 1, departmentName: '技术部', position: '后端开发工程师', email: 'sunba@company.com', phone: '13800000006', status: 'ACTIVE', hireDate: '2024-05-20', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 7, employeeNo: 'EMP007', name: '周九', departmentId: 1, departmentName: '技术部', position: '测试工程师', email: 'zhoujiu@company.com', phone: '13800000007', status: 'ACTIVE', hireDate: '2025-01-10', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 8, employeeNo: 'EMP008', name: '吴十', departmentId: 1, departmentName: '技术部', position: '运维工程师', email: 'wushi@company.com', phone: '13800000008', status: 'ACTIVE', hireDate: '2024-09-01', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 9, employeeNo: 'EMP009', name: '郑十一', departmentId: 1, departmentName: '技术部', position: '全栈开发工程师', email: 'zheng11@company.com', phone: '13800000009', status: 'ACTIVE', hireDate: '2024-07-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 10, employeeNo: 'EMP010', name: '王十二', departmentId: 1, departmentName: '技术部', position: 'UI设计师', email: 'wang12@company.com', phone: '13800000010', status: 'ACTIVE', hireDate: '2023-11-20', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 11, employeeNo: 'EMP011', name: '李十三', departmentId: 1, departmentName: '技术部', position: '数据分析师', email: 'li13@company.com', phone: '13800000011', status: 'ACTIVE', hireDate: '2024-02-25', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 12, employeeNo: 'EMP012', name: '陈十四', departmentId: 1, departmentName: '技术部', position: 'DevOps工程师', email: 'chen14@company.com', phone: '13800000012', status: 'ACTIVE', hireDate: '2025-06-10', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 13, employeeNo: 'EMP013', name: '刘十五', departmentId: 2, departmentName: '人事部', position: '人事主管', email: 'liu15@company.com', phone: '13800000013', status: 'ACTIVE', hireDate: '2024-04-01', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 14, employeeNo: 'EMP014', name: '杨十六', departmentId: 2, departmentName: '人事部', position: '招聘专员', email: 'yang16@company.com', phone: '13800000014', status: 'ACTIVE', hireDate: '2025-08-20', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 15, employeeNo: 'EMP015', name: '黄十七', departmentId: 2, departmentName: '人事部', position: '培训专员', email: 'huang17@company.com', phone: '13800000015', status: 'ACTIVE', hireDate: '2024-09-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 16, employeeNo: 'EMP016', name: '朱十八', departmentId: 2, departmentName: '人事部', position: '绩效专员', email: 'zhu18@company.com', phone: '13800000016', status: 'ACTIVE', hireDate: '2025-03-01', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 17, employeeNo: 'EMP017', name: '胡十九', departmentId: 2, departmentName: '人事部', position: '薪酬专员', email: 'hu19@company.com', phone: '13800000017', status: 'ACTIVE', hireDate: '2024-06-10', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 18, employeeNo: 'EMP018', name: '林二十', departmentId: 2, departmentName: '人事部', position: '人事经理', email: 'lin20@company.com', phone: '13800000018', status: 'ACTIVE', hireDate: '2022-10-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 19, employeeNo: 'EMP019', name: '何二十一', departmentId: 2, departmentName: '人事部', position: '员工关系专员', email: 'he21@company.com', phone: '13800000019', status: 'ACTIVE', hireDate: '2024-08-20', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 20, employeeNo: 'EMP020', name: '罗二十二', departmentId: 2, departmentName: '人事部', position: '人事助理', email: 'luo22@company.com', phone: '13800000020', status: 'ACTIVE', hireDate: '2025-05-01', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 21, employeeNo: 'EMP021', name: '梁二十三', departmentId: 2, departmentName: '人事部', position: '企业文化专员', email: 'liang23@company.com', phone: '13800000021', status: 'ACTIVE', hireDate: '2024-11-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 22, employeeNo: 'EMP022', name: '谢二十四', departmentId: 2, departmentName: '人事部', position: '行政专员', email: 'xie24@company.com', phone: '13800000022', status: 'ACTIVE', hireDate: '2025-02-10', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 23, employeeNo: 'EMP023', name: '宋二十五', departmentId: 3, departmentName: '财务部', position: '财务经理', email: 'song25@company.com', phone: '13800000023', status: 'ACTIVE', hireDate: '2023-01-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 24, employeeNo: 'EMP024', name: '唐二十六', departmentId: 3, departmentName: '财务部', position: '会计', email: 'tang26@company.com', phone: '13800000024', status: 'ACTIVE', hireDate: '2024-07-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 25, employeeNo: 'EMP025', name: '许二十七', departmentId: 3, departmentName: '财务部', position: '出纳', email: 'xu27@company.com', phone: '13800000025', status: 'ACTIVE', hireDate: '2025-04-01', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 26, employeeNo: 'EMP026', name: '邓二十八', departmentId: 3, departmentName: '财务部', position: '成本会计', email: 'deng28@company.com', phone: '13800000026', status: 'ACTIVE', hireDate: '2024-03-20', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 27, employeeNo: 'EMP027', name: '韩二十九', departmentId: 3, departmentName: '财务部', position: '税务专员', email: 'han29@company.com', phone: '13800000027', status: 'ACTIVE', hireDate: '2023-09-10', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 28, employeeNo: 'EMP028', name: '曹三十', departmentId: 3, departmentName: '财务部', position: '财务总监', email: 'cao30@company.com', phone: '13800000028', status: 'ACTIVE', hireDate: '2021-06-01', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 29, employeeNo: 'EMP029', name: '彭三十一', departmentId: 3, departmentName: '财务部', position: '审计专员', email: 'peng31@company.com', phone: '13800000029', status: 'ACTIVE', hireDate: '2024-08-25', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 30, employeeNo: 'EMP030', name: '曾三十二', departmentId: 3, departmentName: '财务部', position: '预算专员', email: 'zeng32@company.com', phone: '13800000030', status: 'ACTIVE', hireDate: '2025-01-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 31, employeeNo: 'EMP031', name: '萧三十三', departmentId: 3, departmentName: '财务部', position: '总账会计', email: 'xiao33@company.com', phone: '13800000031', status: 'ACTIVE', hireDate: '2024-05-10', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 32, employeeNo: 'EMP032', name: '田三十四', departmentId: 3, departmentName: '财务部', position: '财务助理', email: 'tian34@company.com', phone: '13800000032', status: 'ACTIVE', hireDate: '2025-07-01', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 33, employeeNo: 'EMP033', name: '董三十五', departmentId: 4, departmentName: '市场部', position: '市场经理', email: 'dong35@company.com', phone: '13800000033', status: 'ACTIVE', hireDate: '2023-03-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 34, employeeNo: 'EMP034', name: '梁三十六', departmentId: 4, departmentName: '市场部', position: '市场专员', email: 'liang36@company.com', phone: '13800000034', status: 'ACTIVE', hireDate: '2024-10-01', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 35, employeeNo: 'EMP035', name: '袁三十七', departmentId: 4, departmentName: '市场部', position: '品牌专员', email: 'yuan37@company.com', phone: '13800000035', status: 'ACTIVE', hireDate: '2025-02-20', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 36, employeeNo: 'EMP036', name: '蔡三十八', departmentId: 4, departmentName: '市场部', position: '新媒体运营', email: 'cai38@company.com', phone: '13800000036', status: 'ACTIVE', hireDate: '2024-06-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 37, employeeNo: 'EMP037', name: '蒋三十九', departmentId: 4, departmentName: '市场部', position: 'SEO专员', email: 'jiang39@company.com', phone: '13800000037', status: 'ACTIVE', hireDate: '2023-12-01', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 38, employeeNo: 'EMP038', name: '余四十', departmentId: 4, departmentName: '市场部', position: '市场总监', email: 'yu40@company.com', phone: '13800000038', status: 'ACTIVE', hireDate: '2021-08-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 39, employeeNo: 'EMP039', name: '杜四十一', departmentId: 4, departmentName: '市场部', position: '活动策划', email: 'du41@company.com', phone: '13800000039', status: 'ACTIVE', hireDate: '2024-07-20', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 40, employeeNo: 'EMP040', name: '程四十二', departmentId: 4, departmentName: '市场部', position: '销售专员', email: 'cheng42@company.com', phone: '13800000040', status: 'ACTIVE', hireDate: '2025-03-10', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 41, employeeNo: 'EMP041', name: '傅四十三', departmentId: 4, departmentName: '市场部', position: '渠道专员', email: 'fu43@company.com', phone: '13800000041', status: 'ACTIVE', hireDate: '2024-09-05', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 42, employeeNo: 'EMP042', name: '沈四十四', departmentId: 4, departmentName: '市场部', position: '市场助理', email: 'shen44@company.com', phone: '13800000042', status: 'ACTIVE', hireDate: '2025-05-20', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 43, employeeNo: 'EMP043', name: '姜四十五', departmentId: 5, departmentName: '产品部', position: '产品经理', email: 'jiang45@company.com', phone: '13800000043', status: 'ACTIVE', hireDate: '2023-05-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 44, employeeNo: 'EMP044', name: '范四十六', departmentId: 5, departmentName: '产品部', position: '产品经理', email: 'fan46@company.com', phone: '13800000044', status: 'ACTIVE', hireDate: '2024-02-10', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 45, employeeNo: 'EMP045', name: '石四十七', departmentId: 5, departmentName: '产品部', position: '产品助理', email: 'shi47@company.com', phone: '13800000045', status: 'ACTIVE', hireDate: '2025-01-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 46, employeeNo: 'EMP046', name: '姚四十八', departmentId: 5, departmentName: '产品部', position: '产品总监', email: 'yao48@company.com', phone: '13800000046', status: 'ACTIVE', hireDate: '2021-11-01', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 47, employeeNo: 'EMP047', name: '谭四十九', departmentId: 5, departmentName: '产品部', position: 'UI/UX设计师', email: 'tan49@company.com', phone: '13800000047', status: 'ACTIVE', hireDate: '2024-04-20', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 48, employeeNo: 'EMP048', name: '廖五十', departmentId: 5, departmentName: '产品部', position: '产品经理', email: 'liao50@company.com', phone: '13800000048', status: 'ACTIVE', hireDate: '2022-06-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 49, employeeNo: 'EMP049', name: '邹五十一', departmentId: 5, departmentName: '产品部', position: '交互设计师', email: 'zou51@company.com', phone: '13800000049', status: 'ACTIVE', hireDate: '2024-08-01', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 50, employeeNo: 'EMP050', name: '熊五十二', departmentId: 5, departmentName: '产品部', position: '数据产品经理', email: 'xiong52@company.com', phone: '13800000050', status: 'ACTIVE', hireDate: '2023-09-10', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 51, employeeNo: 'EMP051', name: '金五十三', departmentId: 5, departmentName: '产品部', position: '产品运营', email: 'jin53@company.com', phone: '13800000051', status: 'ACTIVE', hireDate: '2025-02-25', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 52, employeeNo: 'EMP052', name: '陆五十四', departmentId: 5, departmentName: '产品部', position: '产品助理', email: 'lu54@company.com', phone: '13800000052', status: 'ACTIVE', hireDate: '2025-06-01', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 53, employeeNo: 'EMP053', name: '郝五十五', departmentId: 6, departmentName: '运营部', position: '运营主管', email: 'hao55@company.com', phone: '13800000053', status: 'ACTIVE', hireDate: '2023-07-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 54, employeeNo: 'EMP054', name: '孔五十六', departmentId: 6, departmentName: '运营部', position: '内容运营', email: 'kong56@company.com', phone: '13800000054', status: 'ACTIVE', hireDate: '2024-11-01', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 55, employeeNo: 'EMP055', name: '白五十七', departmentId: 6, departmentName: '运营部', position: '用户运营', email: 'bai57@company.com', phone: '13800000055', status: 'ACTIVE', hireDate: '2025-03-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 56, employeeNo: 'EMP056', name: '崔五十八', departmentId: 6, departmentName: '运营部', position: '活动运营', email: 'cui58@company.com', phone: '13800000056', status: 'ACTIVE', hireDate: '2024-05-20', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 57, employeeNo: 'EMP057', name: '康五十九', departmentId: 6, departmentName: '运营部', position: '数据运营', email: 'kang59@company.com', phone: '13800000057', status: 'ACTIVE', hireDate: '2023-12-10', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 58, employeeNo: 'EMP058', name: '毛六十', departmentId: 6, departmentName: '运营部', position: '运营总监', email: 'mao60@company.com', phone: '13800000058', status: 'ACTIVE', hireDate: '2021-07-01', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 59, employeeNo: 'EMP059', name: '邱六十一', departmentId: 6, departmentName: '运营部', position: '社群运营', email: 'qiu61@company.com', phone: '13800000059', status: 'ACTIVE', hireDate: '2024-08-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 60, employeeNo: 'EMP060', name: '秦六十二', departmentId: 6, departmentName: '运营部', position: '增长运营', email: 'qin62@company.com', phone: '13800000060', status: 'ACTIVE', hireDate: '2025-01-01', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 61, employeeNo: 'EMP061', name: '江六十三', departmentId: 6, departmentName: '运营部', position: '渠道运营', email: 'jiang63@company.com', phone: '13800000061', status: 'ACTIVE', hireDate: '2024-06-10', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 62, employeeNo: 'EMP062', name: '史六十四', departmentId: 6, departmentName: '运营部', position: '运营助理', email: 'shi64@company.com', phone: '13800000062', status: 'ACTIVE', hireDate: '2025-04-15', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-17T11:00:00' }
]

export const mockAttendanceRecords = [
  { id: 1, employeeId: 1, employeeName: '张三', departmentId: 1, departmentName: '技术部', workDate: '2026-07-15', checkIn: '2026-07-15T09:00:00', checkOut: '2026-07-15T18:00:00', status: 'CHECKED_OUT' },
  { id: 2, employeeId: 1, employeeName: '张三', departmentId: 1, departmentName: '技术部', workDate: '2026-07-16', checkIn: '2026-07-16T08:30:00', checkOut: '2026-07-16T18:30:00', status: 'CHECKED_OUT' },
  { id: 3, employeeId: 1, employeeName: '张三', departmentId: 1, departmentName: '技术部', workDate: '2026-07-17', checkIn: '2026-07-17T09:15:00', checkOut: '2026-07-17T18:15:00', status: 'CHECKED_OUT' },
  { id: 4, employeeId: 2, employeeName: '李四', departmentId: 1, departmentName: '技术部', workDate: '2026-07-15', checkIn: '2026-07-15T09:05:00', checkOut: '2026-07-15T18:05:00', status: 'CHECKED_OUT' },
  { id: 5, employeeId: 2, employeeName: '李四', departmentId: 1, departmentName: '技术部', workDate: '2026-07-16', checkIn: '2026-07-16T08:55:00', checkOut: '2026-07-16T18:20:00', status: 'CHECKED_OUT' },
  { id: 6, employeeId: 3, employeeName: '王五', departmentId: 1, departmentName: '技术部', workDate: '2026-07-15', checkIn: '2026-07-15T09:10:00', checkOut: '2026-07-15T17:30:00', status: 'CHECKED_OUT' },
  { id: 7, employeeId: 4, employeeName: '赵六', departmentId: 1, departmentName: '技术部', workDate: '2026-07-15', checkIn: '2026-07-15T09:20:00', checkOut: '2026-07-15T18:45:00', status: 'CHECKED_OUT' },
  { id: 8, employeeId: 5, employeeName: '钱七', departmentId: 1, departmentName: '技术部', workDate: '2026-07-15', checkIn: '2026-07-15T08:45:00', checkOut: '2026-07-15T19:00:00', status: 'CHECKED_OUT' },
  { id: 9, employeeId: 6, employeeName: '孙八', departmentId: 1, departmentName: '技术部', workDate: '2026-07-15', checkIn: '2026-07-15T09:00:00', checkOut: '2026-07-15T18:30:00', status: 'CHECKED_OUT' },
  { id: 10, employeeId: 7, employeeName: '周九', departmentId: 1, departmentName: '技术部', workDate: '2026-07-15', checkIn: '2026-07-15T09:05:00', checkOut: '2026-07-15T18:00:00', status: 'CHECKED_OUT' },
  { id: 11, employeeId: 13, employeeName: '刘十五', departmentId: 2, departmentName: '人事部', workDate: '2026-07-15', checkIn: '2026-07-15T09:00:00', checkOut: '2026-07-15T18:00:00', status: 'CHECKED_OUT' },
  { id: 12, employeeId: 14, employeeName: '杨十六', departmentId: 2, departmentName: '人事部', workDate: '2026-07-15', checkIn: '2026-07-15T09:15:00', checkOut: '2026-07-15T18:00:00', status: 'CHECKED_OUT' },
  { id: 13, employeeId: 15, employeeName: '黄十七', departmentId: 2, departmentName: '人事部', workDate: '2026-07-15', checkIn: '2026-07-15T09:00:00', checkOut: '2026-07-15T17:30:00', status: 'CHECKED_OUT' },
  { id: 14, employeeId: 23, employeeName: '宋二十五', departmentId: 3, departmentName: '财务部', workDate: '2026-07-15', checkIn: '2026-07-15T08:50:00', checkOut: '2026-07-15T18:20:00', status: 'CHECKED_OUT' },
  { id: 15, employeeId: 24, employeeName: '唐二十六', departmentId: 3, departmentName: '财务部', workDate: '2026-07-15', checkIn: '2026-07-15T09:00:00', checkOut: '2026-07-15T18:00:00', status: 'CHECKED_OUT' },
  { id: 16, employeeId: 25, employeeName: '许二十七', departmentId: 3, departmentName: '财务部', workDate: '2026-07-15', checkIn: '2026-07-15T09:20:00', checkOut: '2026-07-15T18:30:00', status: 'CHECKED_OUT' },
  { id: 17, employeeId: 33, employeeName: '董三十五', departmentId: 4, departmentName: '市场部', workDate: '2026-07-15', checkIn: '2026-07-15T08:30:00', checkOut: '2026-07-15T18:30:00', status: 'CHECKED_OUT' },
  { id: 18, employeeId: 34, employeeName: '梁三十六', departmentId: 4, departmentName: '市场部', workDate: '2026-07-15', checkIn: '2026-07-15T09:00:00', checkOut: '2026-07-15T18:00:00', status: 'CHECKED_OUT' },
  { id: 19, employeeId: 43, employeeName: '姜四十五', departmentId: 5, departmentName: '产品部', workDate: '2026-07-15', checkIn: '2026-07-15T09:00:00', checkOut: '2026-07-15T18:00:00', status: 'CHECKED_OUT' },
  { id: 20, employeeId: 44, employeeName: '范四十六', departmentId: 5, departmentName: '产品部', workDate: '2026-07-15', checkIn: '2026-07-15T09:00:00', checkOut: '2026-07-15T18:30:00', status: 'CHECKED_OUT' },
  { id: 21, employeeId: 53, employeeName: '郝五十五', departmentId: 6, departmentName: '运营部', workDate: '2026-07-15', checkIn: '2026-07-15T09:00:00', checkOut: '2026-07-15T18:15:00', status: 'CHECKED_OUT' },
  { id: 22, employeeId: 54, employeeName: '孔五十六', departmentId: 6, departmentName: '运营部', workDate: '2026-07-15', checkIn: '2026-07-15T09:10:00', checkOut: '2026-07-15T18:00:00', status: 'CHECKED_OUT' },
  { id: 23, employeeId: 8, employeeName: '吴十', departmentId: 1, departmentName: '技术部', workDate: '2026-07-15', checkIn: '2026-07-15T08:30:00', checkOut: '2026-07-15T18:30:00', status: 'CHECKED_OUT' },
  { id: 24, employeeId: 9, employeeName: '郑十一', departmentId: 1, departmentName: '技术部', workDate: '2026-07-15', checkIn: '2026-07-15T09:00:00', checkOut: '2026-07-15T18:00:00', status: 'CHECKED_OUT' },
  { id: 25, employeeId: 10, employeeName: '王十二', departmentId: 1, departmentName: '技术部', workDate: '2026-07-15', checkIn: '2026-07-15T08:50:00', checkOut: '2026-07-15T18:20:00', status: 'CHECKED_OUT' },
  { id: 26, employeeId: 11, employeeName: '李十三', departmentId: 1, departmentName: '技术部', workDate: '2026-07-15', checkIn: '2026-07-15T09:00:00', checkOut: '2026-07-15T18:00:00', status: 'CHECKED_OUT' },
  { id: 27, employeeId: 12, employeeName: '陈十四', departmentId: 1, departmentName: '技术部', workDate: '2026-07-15', checkIn: '2026-07-15T09:20:00', checkOut: '2026-07-15T18:30:00', status: 'CHECKED_OUT' },
  { id: 28, employeeId: 16, employeeName: '朱十八', departmentId: 2, departmentName: '人事部', workDate: '2026-07-15', checkIn: '2026-07-15T09:00:00', checkOut: '2026-07-15T18:00:00', status: 'CHECKED_OUT' },
  { id: 29, employeeId: 17, employeeName: '胡十九', departmentId: 2, departmentName: '人事部', workDate: '2026-07-15', checkIn: '2026-07-15T09:15:00', checkOut: '2026-07-15T17:30:00', status: 'CHECKED_OUT' },
  { id: 30, employeeId: 18, employeeName: '林二十', departmentId: 2, departmentName: '人事部', workDate: '2026-07-15', checkIn: '2026-07-15T08:45:00', checkOut: '2026-07-15T19:00:00', status: 'CHECKED_OUT' }
]

export const mockLeaveApplications = [
  { id: 1, applicantId: 1, applicantName: '张三', departmentId: 1, departmentName: '技术部', type: 'SICK', typeName: '病假', startTime: '2026-07-20 09:00:00', endTime: '2026-07-21 18:00:00', reason: '感冒发烧，需要休息两天', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-17T12:00:00', updateTime: '2026-07-17T12:00:00' },
  { id: 2, applicantId: 2, applicantName: '李四', departmentId: 1, departmentName: '技术部', type: 'ANNUAL', typeName: '年假', startTime: '2026-07-25 09:00:00', endTime: '2026-07-29 18:00:00', reason: '年度休假，计划外出旅游', status: 'APPROVED', statusName: '已通过', approverId: 5, approverName: '钱七', createTime: '2026-07-16T10:00:00', updateTime: '2026-07-16T14:00:00' },
  { id: 3, applicantId: 3, applicantName: '王五', departmentId: 1, departmentName: '技术部', type: 'PERSONAL', typeName: '事假', startTime: '2026-07-18 14:00:00', endTime: '2026-07-18 18:00:00', reason: '家中有事，需提前离开', status: 'REJECTED', statusName: '已驳回', approverId: 5, approverName: '钱七', createTime: '2026-07-17T09:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 4, applicantId: 6, applicantName: '孙八', departmentId: 1, departmentName: '技术部', type: 'ANNUAL', typeName: '年假', startTime: '2026-07-22 09:00:00', endTime: '2026-07-26 18:00:00', reason: '申请5天年假回家探亲', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T09:00:00', updateTime: '2026-07-18T09:00:00' },
  { id: 5, applicantId: 7, applicantName: '周九', departmentId: 1, departmentName: '技术部', type: 'SICK', typeName: '病假', startTime: '2026-07-19 09:00:00', endTime: '2026-07-19 18:00:00', reason: '急性肠胃炎，需要休息一天', status: 'APPROVED', statusName: '已通过', approverId: 5, approverName: '钱七', createTime: '2026-07-17T16:00:00', updateTime: '2026-07-18T10:00:00' },
  { id: 6, applicantId: 8, applicantName: '吴十', departmentId: 1, departmentName: '技术部', type: 'OTHER', typeName: '其他', startTime: '2026-07-24 09:00:00', endTime: '2026-07-24 12:00:00', reason: '参加技术培训课程', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T16:00:00', updateTime: '2026-07-18T16:00:00' },
  { id: 7, applicantId: 9, applicantName: '郑十一', departmentId: 1, departmentName: '技术部', type: 'PERSONAL', typeName: '事假', startTime: '2026-07-21 09:00:00', endTime: '2026-07-21 18:00:00', reason: '参加朋友婚礼', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T11:00:00', updateTime: '2026-07-18T11:00:00' },
  { id: 8, applicantId: 10, applicantName: '王十二', departmentId: 1, departmentName: '技术部', type: 'ANNUAL', typeName: '年假', startTime: '2026-08-01 09:00:00', endTime: '2026-08-07 18:00:00', reason: '暑假带孩子外出旅游', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T14:00:00', updateTime: '2026-07-18T14:00:00' },
  { id: 9, applicantId: 11, applicantName: '李十三', departmentId: 1, departmentName: '技术部', type: 'SICK', typeName: '病假', startTime: '2026-07-20 09:00:00', endTime: '2026-07-20 18:00:00', reason: '偏头痛发作', status: 'APPROVED', statusName: '已通过', approverId: 5, approverName: '钱七', createTime: '2026-07-17T08:00:00', updateTime: '2026-07-17T09:00:00' },
  { id: 10, applicantId: 12, applicantName: '陈十四', departmentId: 1, departmentName: '技术部', type: 'PERSONAL', typeName: '事假', startTime: '2026-07-25 09:00:00', endTime: '2026-07-25 18:00:00', reason: '家里装修需要监工', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T17:00:00', updateTime: '2026-07-18T17:00:00' },
  { id: 11, applicantId: 13, applicantName: '刘十五', departmentId: 2, departmentName: '人事部', type: 'ANNUAL', typeName: '年假', startTime: '2026-07-22 09:00:00', endTime: '2026-07-26 18:00:00', reason: '申请5天年假回家探亲', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T09:00:00', updateTime: '2026-07-18T09:00:00' },
  { id: 12, applicantId: 14, applicantName: '杨十六', departmentId: 2, departmentName: '人事部', type: 'SICK', typeName: '病假', startTime: '2026-07-19 09:00:00', endTime: '2026-07-19 18:00:00', reason: '感冒发烧，需要休息', status: 'APPROVED', statusName: '已通过', approverId: 18, approverName: '林二十', createTime: '2026-07-17T16:00:00', updateTime: '2026-07-18T10:00:00' },
  { id: 13, applicantId: 15, applicantName: '黄十七', departmentId: 2, departmentName: '人事部', type: 'PERSONAL', typeName: '事假', startTime: '2026-07-21 09:00:00', endTime: '2026-07-21 18:00:00', reason: '办理个人证件', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T11:00:00', updateTime: '2026-07-18T11:00:00' },
  { id: 14, applicantId: 16, applicantName: '朱十八', departmentId: 2, departmentName: '人事部', type: 'ANNUAL', typeName: '年假', startTime: '2026-08-01 09:00:00', endTime: '2026-08-05 18:00:00', reason: '年度休假', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T14:00:00', updateTime: '2026-07-18T14:00:00' },
  { id: 15, applicantId: 17, applicantName: '胡十九', departmentId: 2, departmentName: '人事部', type: 'SICK', typeName: '病假', startTime: '2026-07-23 09:00:00', endTime: '2026-07-23 18:00:00', reason: '急性肠胃炎', status: 'APPROVED', statusName: '已通过', approverId: 18, approverName: '林二十', createTime: '2026-07-17T08:00:00', updateTime: '2026-07-17T09:00:00' },
  { id: 16, applicantId: 19, applicantName: '何二十一', departmentId: 2, departmentName: '人事部', type: 'PERSONAL', typeName: '事假', startTime: '2026-07-26 09:00:00', endTime: '2026-07-26 18:00:00', reason: '参加培训课程', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T17:00:00', updateTime: '2026-07-18T17:00:00' },
  { id: 17, applicantId: 20, applicantName: '罗二十二', departmentId: 2, departmentName: '人事部', type: 'OTHER', typeName: '其他', startTime: '2026-07-24 09:00:00', endTime: '2026-07-24 12:00:00', reason: '办理社保手续', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T16:00:00', updateTime: '2026-07-18T16:00:00' },
  { id: 18, applicantId: 23, applicantName: '宋二十五', departmentId: 3, departmentName: '财务部', type: 'ANNUAL', typeName: '年假', startTime: '2026-07-25 09:00:00', endTime: '2026-07-29 18:00:00', reason: '年度休假', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T14:00:00', updateTime: '2026-07-18T14:00:00' },
  { id: 19, applicantId: 24, applicantName: '唐二十六', departmentId: 3, departmentName: '财务部', type: 'SICK', typeName: '病假', startTime: '2026-07-19 09:00:00', endTime: '2026-07-19 18:00:00', reason: '感冒发烧', status: 'APPROVED', statusName: '已通过', approverId: 28, approverName: '曹三十', createTime: '2026-07-17T16:00:00', updateTime: '2026-07-18T10:00:00' },
  { id: 20, applicantId: 25, applicantName: '许二十七', departmentId: 3, departmentName: '财务部', type: 'PERSONAL', typeName: '事假', startTime: '2026-07-21 09:00:00', endTime: '2026-07-21 18:00:00', reason: '家中有事', status: 'REJECTED', statusName: '已驳回', approverId: 28, approverName: '曹三十', createTime: '2026-07-17T09:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 21, applicantId: 26, applicantName: '邓二十八', departmentId: 3, departmentName: '财务部', type: 'ANNUAL', typeName: '年假', startTime: '2026-08-01 09:00:00', endTime: '2026-08-07 18:00:00', reason: '暑假带孩子旅游', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T09:00:00', updateTime: '2026-07-18T09:00:00' },
  { id: 22, applicantId: 27, applicantName: '韩二十九', departmentId: 3, departmentName: '财务部', type: 'SICK', typeName: '病假', startTime: '2026-07-20 09:00:00', endTime: '2026-07-20 18:00:00', reason: '偏头痛', status: 'APPROVED', statusName: '已通过', approverId: 28, approverName: '曹三十', createTime: '2026-07-17T08:00:00', updateTime: '2026-07-17T09:00:00' },
  { id: 23, applicantId: 29, applicantName: '彭三十一', departmentId: 3, departmentName: '财务部', type: 'PERSONAL', typeName: '事假', startTime: '2026-07-26 09:00:00', endTime: '2026-07-26 18:00:00', reason: '参加培训', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T17:00:00', updateTime: '2026-07-18T17:00:00' },
  { id: 24, applicantId: 30, applicantName: '曾三十二', departmentId: 3, departmentName: '财务部', type: 'OTHER', typeName: '其他', startTime: '2026-07-24 09:00:00', endTime: '2026-07-24 12:00:00', reason: '办理税务手续', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T16:00:00', updateTime: '2026-07-18T16:00:00' },
  { id: 25, applicantId: 33, applicantName: '董三十五', departmentId: 4, departmentName: '市场部', type: 'ANNUAL', typeName: '年假', startTime: '2026-07-22 09:00:00', endTime: '2026-07-26 18:00:00', reason: '申请5天年假', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T09:00:00', updateTime: '2026-07-18T09:00:00' },
  { id: 26, applicantId: 34, applicantName: '梁三十六', departmentId: 4, departmentName: '市场部', type: 'SICK', typeName: '病假', startTime: '2026-07-19 09:00:00', endTime: '2026-07-19 18:00:00', reason: '感冒发烧', status: 'APPROVED', statusName: '已通过', approverId: 38, approverName: '余四十', createTime: '2026-07-17T16:00:00', updateTime: '2026-07-18T10:00:00' },
  { id: 27, applicantId: 35, applicantName: '袁三十七', departmentId: 4, departmentName: '市场部', type: 'PERSONAL', typeName: '事假', startTime: '2026-07-21 09:00:00', endTime: '2026-07-21 18:00:00', reason: '办理个人事务', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T11:00:00', updateTime: '2026-07-18T11:00:00' },
  { id: 28, applicantId: 36, applicantName: '蔡三十八', departmentId: 4, departmentName: '市场部', type: 'ANNUAL', typeName: '年假', startTime: '2026-08-01 09:00:00', endTime: '2026-08-05 18:00:00', reason: '年度休假', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T14:00:00', updateTime: '2026-07-18T14:00:00' },
  { id: 29, applicantId: 37, applicantName: '蒋三十九', departmentId: 4, departmentName: '市场部', type: 'SICK', typeName: '病假', startTime: '2026-07-20 09:00:00', endTime: '2026-07-20 18:00:00', reason: '身体不适', status: 'APPROVED', statusName: '已通过', approverId: 38, approverName: '余四十', createTime: '2026-07-17T08:00:00', updateTime: '2026-07-17T09:00:00' },
  { id: 30, applicantId: 39, applicantName: '杜四十一', departmentId: 4, departmentName: '市场部', type: 'PERSONAL', typeName: '事假', startTime: '2026-07-26 09:00:00', endTime: '2026-07-26 18:00:00', reason: '参加活动策划', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T17:00:00', updateTime: '2026-07-18T17:00:00' },
  { id: 31, applicantId: 40, applicantName: '程四十二', departmentId: 4, departmentName: '市场部', type: 'OTHER', typeName: '其他', startTime: '2026-07-24 09:00:00', endTime: '2026-07-24 12:00:00', reason: '客户拜访', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T16:00:00', updateTime: '2026-07-18T16:00:00' },
  { id: 32, applicantId: 43, applicantName: '姜四十五', departmentId: 5, departmentName: '产品部', type: 'ANNUAL', typeName: '年假', startTime: '2026-07-25 09:00:00', endTime: '2026-07-29 18:00:00', reason: '年度休假', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T14:00:00', updateTime: '2026-07-18T14:00:00' },
  { id: 33, applicantId: 44, applicantName: '范四十六', departmentId: 5, departmentName: '产品部', type: 'SICK', typeName: '病假', startTime: '2026-07-19 09:00:00', endTime: '2026-07-19 18:00:00', reason: '感冒发烧', status: 'APPROVED', statusName: '已通过', approverId: 48, approverName: '廖五十', createTime: '2026-07-17T16:00:00', updateTime: '2026-07-18T10:00:00' },
  { id: 34, applicantId: 45, applicantName: '石四十七', departmentId: 5, departmentName: '产品部', type: 'PERSONAL', typeName: '事假', startTime: '2026-07-21 09:00:00', endTime: '2026-07-21 18:00:00', reason: '家中有事', status: 'REJECTED', statusName: '已驳回', approverId: 48, approverName: '廖五十', createTime: '2026-07-17T09:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 35, applicantId: 47, applicantName: '谭四十九', departmentId: 5, departmentName: '产品部', type: 'ANNUAL', typeName: '年假', startTime: '2026-08-01 09:00:00', endTime: '2026-08-07 18:00:00', reason: '暑假带孩子旅游', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T09:00:00', updateTime: '2026-07-18T09:00:00' },
  { id: 36, applicantId: 49, applicantName: '邹五十一', departmentId: 5, departmentName: '产品部', type: 'SICK', typeName: '病假', startTime: '2026-07-20 09:00:00', endTime: '2026-07-20 18:00:00', reason: '偏头痛', status: 'APPROVED', statusName: '已通过', approverId: 48, approverName: '廖五十', createTime: '2026-07-17T08:00:00', updateTime: '2026-07-17T09:00:00' },
  { id: 37, applicantId: 50, applicantName: '熊五十二', departmentId: 5, departmentName: '产品部', type: 'PERSONAL', typeName: '事假', startTime: '2026-07-26 09:00:00', endTime: '2026-07-26 18:00:00', reason: '参加培训', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T17:00:00', updateTime: '2026-07-18T17:00:00' },
  { id: 38, applicantId: 51, applicantName: '金五十三', departmentId: 5, departmentName: '产品部', type: 'OTHER', typeName: '其他', startTime: '2026-07-24 09:00:00', endTime: '2026-07-24 12:00:00', reason: '用户调研', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T16:00:00', updateTime: '2026-07-18T16:00:00' },
  { id: 39, applicantId: 53, applicantName: '郝五十五', departmentId: 6, departmentName: '运营部', type: 'ANNUAL', typeName: '年假', startTime: '2026-07-22 09:00:00', endTime: '2026-07-26 18:00:00', reason: '申请5天年假', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T09:00:00', updateTime: '2026-07-18T09:00:00' },
  { id: 40, applicantId: 54, applicantName: '孔五十六', departmentId: 6, departmentName: '运营部', type: 'SICK', typeName: '病假', startTime: '2026-07-19 09:00:00', endTime: '2026-07-19 18:00:00', reason: '感冒发烧', status: 'APPROVED', statusName: '已通过', approverId: 58, approverName: '毛六十', createTime: '2026-07-17T16:00:00', updateTime: '2026-07-18T10:00:00' },
  { id: 41, applicantId: 55, applicantName: '白五十七', departmentId: 6, departmentName: '运营部', type: 'PERSONAL', typeName: '事假', startTime: '2026-07-21 09:00:00', endTime: '2026-07-21 18:00:00', reason: '办理个人事务', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T11:00:00', updateTime: '2026-07-18T11:00:00' },
  { id: 42, applicantId: 56, applicantName: '崔五十八', departmentId: 6, departmentName: '运营部', type: 'ANNUAL', typeName: '年假', startTime: '2026-08-01 09:00:00', endTime: '2026-08-05 18:00:00', reason: '年度休假', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T14:00:00', updateTime: '2026-07-18T14:00:00' },
  { id: 43, applicantId: 57, applicantName: '康五十九', departmentId: 6, departmentName: '运营部', type: 'SICK', typeName: '病假', startTime: '2026-07-20 09:00:00', endTime: '2026-07-20 18:00:00', reason: '身体不适', status: 'APPROVED', statusName: '已通过', approverId: 58, approverName: '毛六十', createTime: '2026-07-17T08:00:00', updateTime: '2026-07-17T09:00:00' },
  { id: 44, applicantId: 59, applicantName: '邱六十一', departmentId: 6, departmentName: '运营部', type: 'PERSONAL', typeName: '事假', startTime: '2026-07-26 09:00:00', endTime: '2026-07-26 18:00:00', reason: '参加社群活动', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T17:00:00', updateTime: '2026-07-18T17:00:00' },
  { id: 45, applicantId: 60, applicantName: '秦六十二', departmentId: 6, departmentName: '运营部', type: 'OTHER', typeName: '其他', startTime: '2026-07-24 09:00:00', endTime: '2026-07-24 12:00:00', reason: '数据分析会议', status: 'PENDING', statusName: '待审批', approverId: null, approverName: null, createTime: '2026-07-18T16:00:00', updateTime: '2026-07-18T16:00:00' }
]

export const mockContents = [
  { id: 1, title: '关于2026年暑期放假安排的通知', content: '各部门：\n\n根据公司年度工作计划，2026年暑期放假安排如下：\n\n一、放假时间\n2026年8月1日至8月5日，共5天。\n\n二、注意事项\n1. 各部门需安排好假期值班人员\n2. 放假前请做好安全检查\n3. 假期期间如有紧急事务，请联系各部门负责人\n\n特此通知。\n\n行政部', type: 'ANNOUNCEMENT', authorId: 1, authorName: 'admin', status: 'PUBLISHED', publishTime: '2026-07-15T09:00:00', createTime: '2026-07-14T16:00:00', updateTime: '2026-07-15T09:00:00' },
  { id: 2, title: '新员工入职培训安排', content: '人力资源部定于2026年7月20日举办新员工入职培训，内容包括公司文化、规章制度、办公系统使用等。请各部门新入职员工准时参加。\n\n培训时间：2026年7月20日 上午9:00-12:00\n培训地点：公司会议室A\n\n人力资源部', type: 'ANNOUNCEMENT', authorId: 18, authorName: '林二十', status: 'PUBLISHED', publishTime: '2026-07-18T10:00:00', createTime: '2026-07-17T14:00:00', updateTime: '2026-07-18T10:00:00' },
  { id: 3, title: '办公区域卫生管理规定', content: '为营造整洁、舒适的办公环境，现对办公区域卫生管理做出如下规定：\n\n1. 每位员工需保持个人工位整洁\n2. 公共区域卫生由各部门轮流负责\n3. 禁止在办公区域吸烟\n4. 下班前请整理好个人物品\n\n行政部', type: 'ANNOUNCEMENT', authorId: 1, authorName: 'admin', status: 'PUBLISHED', publishTime: '2026-07-16T14:00:00', createTime: '2026-07-15T10:00:00', updateTime: '2026-07-16T14:00:00' },
  { id: 4, title: '关于开展年度员工体检的通知', content: '公司将组织2026年度员工体检，具体安排如下：\n\n体检时间：2026年7月25日-7月30日\n体检地点：市第一人民医院体检中心\n\n请各部门统计参加体检人员名单，于7月22日前报人力资源部。\n\n人力资源部', type: 'ANNOUNCEMENT', authorId: 18, authorName: '林二十', status: 'PUBLISHED', publishTime: '2026-07-19T09:00:00', createTime: '2026-07-18T16:00:00', updateTime: '2026-07-19T09:00:00' },
  { id: 5, title: '技术部代码规范更新', content: '技术部近期更新了代码规范，请所有开发人员认真学习并严格遵守。主要更新内容包括：\n\n1. 新增TypeScript类型定义规范\n2. 统一代码格式化工具配置\n3. 加强代码注释要求\n\n请在7月20日前完成学习。\n\n技术部', type: 'ANNOUNCEMENT', authorId: 5, authorName: '王五', status: 'PUBLISHED', publishTime: '2026-07-17T11:00:00', createTime: '2026-07-16T14:00:00', updateTime: '2026-07-17T11:00:00' },
  { id: 6, title: '财务部报销流程调整', content: '为提高工作效率，财务部对报销流程进行了调整：\n\n1. 报销单需通过OA系统提交\n2. 每月15日为报销截止日期\n3. 超过30天的费用不予报销\n\n请各部门员工注意。\n\n财务部', type: 'ANNOUNCEMENT', authorId: 28, authorName: '曹三十', status: 'PUBLISHED', publishTime: '2026-07-18T14:00:00', createTime: '2026-07-17T09:00:00', updateTime: '2026-07-18T14:00:00' },
  { id: 7, title: '市场部季度工作总结要求', content: '市场部各团队请于7月25日前提交第二季度工作总结，内容包括：\n\n1. 季度目标完成情况\n2. 主要工作成果\n3. 存在问题及改进措施\n4. 下季度工作计划\n\n市场部', type: 'ANNOUNCEMENT', authorId: 38, authorName: '余四十', status: 'PUBLISHED', publishTime: '2026-07-16T16:00:00', createTime: '2026-07-15T11:00:00', updateTime: '2026-07-16T16:00:00' },
  { id: 8, title: '产品部需求评审会议通知', content: '产品部定于7月21日召开需求评审会议，请各相关人员准时参加。\n\n会议时间：7月21日 下午2:00-4:00\n会议地点：公司会议室B\n\n产品部', type: 'ANNOUNCEMENT', authorId: 48, authorName: '廖五十', status: 'PUBLISHED', publishTime: '2026-07-19T14:00:00', createTime: '2026-07-18T10:00:00', updateTime: '2026-07-19T14:00:00' },
  { id: 9, title: '运营部活动策划方案征集', content: '运营部现面向全体员工征集下半年活动策划方案，欢迎各部门积极参与。\n\n方案提交截止日期：7月23日\n提交方式：发送至运营部邮箱\n\n运营部', type: 'ANNOUNCEMENT', authorId: 58, authorName: '毛六十', status: 'PUBLISHED', publishTime: '2026-07-17T14:00:00', createTime: '2026-07-16T09:00:00', updateTime: '2026-07-17T14:00:00' },
  { id: 10, title: '关于加强网络安全管理的通知', content: '近期网络安全形势严峻，请全体员工注意：\n\n1. 定期更换密码\n2. 不点击不明链接\n3. 不随意接入公共WiFi\n4. 发现异常及时上报\n\nIT部门', type: 'ANNOUNCEMENT', authorId: 1, authorName: 'admin', status: 'PUBLISHED', publishTime: '2026-07-20T09:00:00', createTime: '2026-07-19T16:00:00', updateTime: '2026-07-20T09:00:00' },
  { id: 11, title: '公司年会筹备工作启动', content: '2026年公司年会筹备工作正式启动，欢迎各部门推荐节目和志愿者。\n\n筹备组联系方式：行政部\n\n行政部', type: 'ANNOUNCEMENT', authorId: 1, authorName: 'admin', status: 'PUBLISHED', publishTime: '2026-07-14T10:00:00', createTime: '2026-07-13T14:00:00', updateTime: '2026-07-14T10:00:00' },
  { id: 12, title: '技术部团建活动安排', content: '技术部定于7月26日组织团建活动，地点为郊区度假村。\n\n集合时间：7月26日 上午8:30\n集合地点：公司门口\n\n请大家准时参加。\n\n技术部', type: 'ANNOUNCEMENT', authorId: 5, authorName: '王五', status: 'PUBLISHED', publishTime: '2026-07-21T14:00:00', createTime: '2026-07-20T10:00:00', updateTime: '2026-07-21T14:00:00' },
  { id: 13, title: '人事部招聘计划更新', content: '人事部近期招聘计划更新，新增以下岗位：\n\n1. 高级前端开发工程师 2名\n2. 产品经理 1名\n3. 运营专员 3名\n\n有意向的员工可推荐候选人。\n\n人力资源部', type: 'ANNOUNCEMENT', authorId: 18, authorName: '林二十', status: 'PUBLISHED', publishTime: '2026-07-20T16:00:00', createTime: '2026-07-19T11:00:00', updateTime: '2026-07-20T16:00:00' },
  { id: 14, title: '财务部年度预算编制通知', content: '各部门请于8月1日前完成2027年度预算编制工作，提交至财务部。\n\n预算编制要求详见OA系统通知。\n\n财务部', type: 'ANNOUNCEMENT', authorId: 28, authorName: '曹三十', status: 'PUBLISHED', publishTime: '2026-07-19T10:00:00', createTime: '2026-07-18T14:00:00', updateTime: '2026-07-19T10:00:00' },
  { id: 15, title: '市场部新品发布会准备', content: '市场部即将举办新品发布会，请各部门配合做好准备工作。\n\n发布会时间：8月10日\n发布会地点：国际会议中心\n\n市场部', type: 'ANNOUNCEMENT', authorId: 38, authorName: '余四十', status: 'PUBLISHED', publishTime: '2026-07-21T09:00:00', createTime: '2026-07-20T14:00:00', updateTime: '2026-07-21T09:00:00' },
  { id: 16, title: '产品部用户反馈收集', content: '产品部现收集用户反馈，请各部门协助收集并汇总至产品部。\n\n反馈收集截止日期：7月25日\n\n产品部', type: 'ANNOUNCEMENT', authorId: 48, authorName: '廖五十', status: 'PUBLISHED', publishTime: '2026-07-18T16:00:00', createTime: '2026-07-17T11:00:00', updateTime: '2026-07-18T16:00:00' },
  { id: 17, title: '运营部公众号内容规范', content: '运营部公众号内容发布规范已更新，请所有编辑人员认真学习。\n\n主要更新：\n1. 新增内容审核流程\n2. 统一排版格式\n3. 加强错别字检查\n\n运营部', type: 'ANNOUNCEMENT', authorId: 58, authorName: '毛六十', status: 'PUBLISHED', publishTime: '2026-07-15T16:00:00', createTime: '2026-07-14T10:00:00', updateTime: '2026-07-15T16:00:00' },
  { id: 18, title: '关于优化办公环境的通知', content: '公司将对办公区域进行优化改造，具体安排如下：\n\n改造时间：7月22日-7月28日\n改造区域：A栋办公区\n\n期间请相关部门配合做好工作安排。\n\n行政部', type: 'ANNOUNCEMENT', authorId: 1, authorName: 'admin', status: 'PUBLISHED', publishTime: '2026-07-16T09:00:00', createTime: '2026-07-15T14:00:00', updateTime: '2026-07-16T09:00:00' },
  { id: 19, title: 'IT部门系统维护通知', content: 'IT部门将于7月23日凌晨进行系统维护，期间OA系统将暂停使用。\n\n维护时间：7月23日 0:00-4:00\n\n请大家提前做好工作安排。\n\nIT部门', type: 'ANNOUNCEMENT', authorId: 1, authorName: 'admin', status: 'PUBLISHED', publishTime: '2026-07-21T16:00:00', createTime: '2026-07-20T11:00:00', updateTime: '2026-07-21T16:00:00' },
  { id: 20, title: '员工福利政策调整', content: '公司对员工福利政策进行了调整，新增以下福利：\n\n1. 年度旅游补贴\n2. 健康体检升级\n3. 餐补标准提高\n\n具体细则请查看OA系统通知。\n\n人力资源部', type: 'ANNOUNCEMENT', authorId: 18, authorName: '林二十', status: 'PUBLISHED', publishTime: '2026-07-22T09:00:00', createTime: '2026-07-21T14:00:00', updateTime: '2026-07-22T09:00:00' },
  { id: 21, title: '技术部项目进度汇报要求', content: '技术部各项目组请于每周五下午提交项目进度汇报，内容包括：\n\n1. 本周完成情况\n2. 下周工作计划\n3. 存在问题及风险\n\n技术部', type: 'ANNOUNCEMENT', authorId: 5, authorName: '王五', status: 'PUBLISHED', publishTime: '2026-07-13T10:00:00', createTime: '2026-07-12T14:00:00', updateTime: '2026-07-13T10:00:00' },
  { id: 22, title: '市场部客户答谢会筹备', content: '市场部将于8月15日举办客户答谢会，请各部门协助邀请重要客户。\n\n答谢会地点：五星级酒店\n\n市场部', type: 'ANNOUNCEMENT', authorId: 38, authorName: '余四十', status: 'PUBLISHED', publishTime: '2026-07-22T14:00:00', createTime: '2026-07-21T10:00:00', updateTime: '2026-07-22T14:00:00' },
  { id: 23, title: '产品部版本发布计划', content: '产品部第三季度版本发布计划已确定：\n\nV2.3版本：7月25日\nV2.4版本：8月15日\nV2.5版本：9月5日\n\n请各部门做好配合工作。\n\n产品部', type: 'ANNOUNCEMENT', authorId: 48, authorName: '廖五十', status: 'PUBLISHED', publishTime: '2026-07-14T14:00:00', createTime: '2026-07-13T09:00:00', updateTime: '2026-07-14T14:00:00' },
  { id: 24, title: '运营部直播活动预告', content: '运营部将于7月24日举办直播活动，主题为"产品使用指南"。\n\n直播时间：7月24日 晚上8:00-9:00\n直播平台：抖音、快手\n\n请大家积极参与。\n\n运营部', type: 'ANNOUNCEMENT', authorId: 58, authorName: '毛六十', status: 'PUBLISHED', publishTime: '2026-07-22T16:00:00', createTime: '2026-07-21T14:00:00', updateTime: '2026-07-22T16:00:00' }
]

export const mockDashboardData = {
  todayAttendance: { total: 60, checkedIn: 58, checkedOut: 45 },
  weeklyAttendance: [
    { day: '周一', checkedIn: 58, checkedOut: 55 },
    { day: '周二', checkedIn: 60, checkedOut: 58 },
    { day: '周三', checkedIn: 59, checkedOut: 56 },
    { day: '周四', checkedIn: 60, checkedOut: 57 },
    { day: '周五', checkedIn: 58, checkedOut: 54 }
  ],
  leaveStatistics: { pending: 25, approved: 15, rejected: 5 },
  departmentEmployeeCount: [
    { departmentId: 1, departmentName: '技术部', count: 12 },
    { departmentId: 2, departmentName: '人事部', count: 10 },
    { departmentId: 3, departmentName: '财务部', count: 10 },
    { departmentId: 4, departmentName: '市场部', count: 10 },
    { departmentId: 5, departmentName: '产品部', count: 10 },
    { departmentId: 6, departmentName: '运营部', count: 10 }
  ],
  recentActivities: [
    { id: 1, type: 'ATTENDANCE', content: '张三 今日已签到', time: '09:05' },
    { id: 2, type: 'LEAVE', content: '李四 提交了请假申请', time: '10:30' },
    { id: 3, type: 'ATTENDANCE', content: '王五 今日已签到', time: '09:10' },
    { id: 4, type: 'ANNOUNCEMENT', content: '发布了新公告', time: '11:00' },
    { id: 5, type: 'LEAVE', content: '赵六 提交了请假申请', time: '14:20' }
  ]
}

export const mockAiAnswers = {
  '公司请假制度': {
    answer: '公司请假制度规定：事假需提前1天申请，年假需提前3天申请，病假需提供医院证明。单次请假最长不超过15天，年度累计不超过30天。',
    sources: ['公司规章制度第3章', '员工手册第5节']
  },
  '考勤规则': {
    answer: '公司考勤时间为工作日上午9:00至下午18:00，中午12:00-13:30为午休时间。迟到15分钟内不计入考勤，超过15分钟按迟到处理。每月迟到超过3次将扣除绩效。',
    sources: ['考勤管理制度']
  },
  '工资发放时间': {
    answer: '公司工资每月10号发放，如遇节假日则提前至最近的工作日。工资包含基本工资、绩效奖金、餐补和交通补贴。',
    sources: ['薪酬管理制度']
  }
}

export const mockSearchResults = {
  '请假': [
    { id: 1, type: 'ANNOUNCEMENT', title: '关于2026年暑期放假安排的通知', highlightTitle: '关于2026年暑期放假安排的通知', body: '放假安排相关内容', highlightBody: '放假安排相关内容', score: 0.9 },
    { id: 2, type: 'ANNOUNCEMENT', title: '新员工入职培训安排', highlightTitle: '新员工入职培训安排', body: '培训相关内容', highlightBody: '培训相关内容', score: 0.7 }
  ],
  '考勤': [
    { id: 1, type: 'ANNOUNCEMENT', title: '办公区域卫生管理规定', highlightTitle: '办公区域卫生管理规定', body: '管理规定相关内容', highlightBody: '管理规定相关内容', score: 0.85 },
    { id: 2, type: 'ANNOUNCEMENT', title: '关于加强网络安全管理的通知', highlightTitle: '关于加强网络安全管理的通知', body: '安全管理相关内容', highlightBody: '安全管理相关内容', score: 0.6 }
  ]
}

export const mockLogs = [
  { id: 1, userId: 1, username: 'admin', action: 'LOGIN', description: '管理员登录系统', ip: '127.0.0.1', createTime: '2026-07-21 09:00:00' },
  { id: 2, userId: 2, username: 'employee', action: 'LOGIN', description: '员工登录系统', ip: '127.0.0.1', createTime: '2026-07-21 09:15:00' },
  { id: 3, userId: 3, username: 'deptadmin', action: 'LOGIN', description: '部门管理员登录系统', ip: '127.0.0.1', createTime: '2026-07-21 09:30:00' },
  { id: 4, userId: 1, username: 'admin', action: 'VIEW', description: '查看部门列表', ip: '127.0.0.1', createTime: '2026-07-21 10:00:00' },
  { id: 5, userId: 2, username: 'employee', action: 'APPLY', description: '提交请假申请', ip: '127.0.0.1', createTime: '2026-07-21 10:30:00' },
  { id: 6, userId: 3, username: 'deptadmin', action: 'AUDIT', description: '审批请假申请', ip: '127.0.0.1', createTime: '2026-07-21 11:00:00' },
  { id: 7, userId: 1, username: 'admin', action: 'CREATE', description: '发布新公告', ip: '127.0.0.1', createTime: '2026-07-21 14:00:00' },
  { id: 8, userId: 2, username: 'employee', action: 'VIEW', description: '查看公告详情', ip: '127.0.0.1', createTime: '2026-07-21 14:30:00' },
  { id: 9, userId: 1, username: 'admin', action: 'UPDATE', description: '编辑部门信息', ip: '127.0.0.1', createTime: '2026-07-21 15:00:00' },
  { id: 10, userId: 3, username: 'deptadmin', action: 'VIEW', description: '查看部门待审批动态', ip: '127.0.0.1', createTime: '2026-07-21 15:30:00' }
]
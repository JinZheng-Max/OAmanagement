> [!WARNING]
> 本文档基于历史接口草案持续更新。当前以 `http://localhost:10002` 为网关入口，所有 API 需通过网关访问。

# 智办AI OA 系统 — API 接口文档

## 概述

本文档详细描述了智办AI OA系统的后端API接口，包括认证管理、部门管理、员工管理、考勤管理、请假审批、公告制度、全文检索、AI智慧办公和运维管理等模块。

## 基础URL

```
http://localhost:8080
```

## 认证方式

大部分接口需要认证，认证通过在请求头中添加 `Authorization` 字段实现：

```
Authorization: Bearer <token值>
```

> 其中 `<token值>` 通过登录接口获取，Token 有效期为30分钟，可通过刷新机制延长。

## 响应格式

所有接口返回JSON格式数据，通用响应结构如下：

### 成功响应

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

### 失败响应

```json
{
  "code": 500,
  "msg": "错误描述信息",
  "traceId": "请求追踪标识"
}
```

### 分页响应（data字段结构）

```json
{
  "records": [],
  "total": 0,
  "size": 10,
  "current": 1,
  "pages": 0
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| records | array | 当前页数据列表 |
| total | integer | 总记录数 |
| size | integer | 每页条数 |
| current | integer | 当前页码 |
| pages | integer | 总页数 |

### 业务状态码说明

| code | 说明 |
|------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或登录已过期 |
| 403 | 无权限访问 |
| 404 | 请求资源不存在 |
| 500 | 服务器内部错误 |

---

## 接口详情

---

### 一、认证管理模块

#### 1. 用户登录

- **接口地址**: `POST /api/auth/login`
- **请求头**: 无需认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | string | 是 | 登录账号 |
| password | string | 是 | 密码 |

- **请求示例**:

```json
{
  "username": "admin",
  "password": "<your-current-password>"
}
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.xxx...",
    "tokenType": "Bearer",
    "expiresIn": 1800000,
    "userInfo": {
      "id": 1,
      "username": "admin",
      "role": "SUPER_ADMIN",
      "employeeId": 10086
    }
  }
}
```

**角色说明**（`更新.sql` 扩展）：

| 角色 | 说明 |
|------|------|
| `SUPER_ADMIN` | 总管理员，替代原 ADMIN，可访问全部管理功能 |
| `DEPT_MANAGER` | 部门经理，可访问全公司公开资料及本部门授权资料（预留） |
| `EMPLOYEE` | 普通员工，可访问公共信息及本人相关数据 |

---

#### 2. 用户退出

- **接口地址**: `POST /api/auth/logout`
- **请求头**: 需要认证
- **响应示例**:

```json
{
  "code": 200,
  "msg": "退出成功",
  "data": null
}
```

---

#### 3. 获取当前用户信息

- **接口地址**: `GET /api/auth/me`
- **请求头**: 需要认证
- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "username": "admin",
    "role": "SUPER_ADMIN",
    "status": 1,
    "employeeId": 10086,
    "employee": { "name": "管理员", "employeeNo": "EMP000" },
    "createTime": "2026-07-17 11:00:00",
    "updateTime": "2026-07-17 11:00:00"
  }
}
```

---

#### 4. 修改密码

- **接口地址**: `PUT /api/auth/password`
- **请求头**: 需要认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| oldPassword | string | 是 | 当前密码 |
| newPassword | string | 是 | 新密码 |

- **请求示例**:

```json
{
  "oldPassword": "<your-current-password>",
  "newPassword": "newpassword456"
}
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "密码修改成功",
  "data": null
}
```

---

### 二、部门管理模块

#### 1. 分页查询部门列表

- **接口地址**: `GET /api/departments`
- **请求头**: 需要认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | integer | 否 | 页码，默认1 |
| size | integer | 否 | 每页条数，默认10 |

- **请求示例**:

```
GET /api/departments?page=1&size=10
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "code": "DEPT_IT",
        "name": "技术部",
        "leaderId": null,
        "status": 1,
        "sort": 1,
        "createTime": "2026-07-17T11:00:00",
        "updateTime": "2026-07-17T11:00:00"
      },
      {
        "id": 2,
        "code": "DEPT_HR",
        "name": "人事部",
        "leaderId": null,
        "status": 1,
        "sort": 2,
        "createTime": "2026-07-17T11:00:00",
        "updateTime": "2026-07-17T11:00:00"
      }
    ],
    "total": 4,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

#### 2. 查询部门详情

- **接口地址**: `GET /api/departments/{id}`
- **请求头**: 需要认证
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | integer | 是 | 部门ID |

- **请求示例**:

```
GET /api/departments/1
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "code": "DEPT_IT",
    "name": "技术部",
    "leaderId": null,
    "status": 1,
    "sort": 1,
    "createTime": "2026-07-17T11:00:00",
    "updateTime": "2026-07-17T11:00:00"
  }
}
```

---

#### 3. 新增部门

- **接口地址**: `POST /api/departments`
- **请求头**: 需要认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | string | 是 | 部门编码（唯一） |
| name | string | 是 | 部门名称 |
| leaderId | integer | 否 | 部门负责人员工ID |
| status | integer | 否 | 状态：1-启用(默认) 0-停用 |
| sort | integer | 否 | 排序号，默认0 |

- **请求示例**:

```json
{
  "code": "DEPT_MARKET",
  "name": "市场部",
  "leaderId": null,
  "sort": 5
}
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 5,
    "code": "DEPT_MARKET",
    "name": "市场部",
    "leaderId": null,
    "status": 1,
    "sort": 5,
    "createTime": "2026-07-17T12:00:00",
    "updateTime": "2026-07-17T12:00:00"
  }
}
```

---

#### 4. 编辑部门

- **接口地址**: `PUT /api/departments/{id}`
- **请求头**: 需要认证
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | integer | 是 | 部门ID |

- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | string | 否 | 部门编码 |
| name | string | 否 | 部门名称 |
| leaderId | integer | 否 | 部门负责人员工ID |
| status | integer | 否 | 状态：1-启用 0-停用 |
| sort | integer | 否 | 排序号 |

- **请求示例**:

```json
{
  "name": "市场部（更名）",
  "sort": 6
}
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

---

#### 5. 停用/启用部门

- **接口地址**: `PUT /api/departments/{id}/status`
- **请求头**: 需要认证
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | integer | 是 | 部门ID |

- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | integer | 是 | 状态：1-启用 0-停用 |

- **请求示例**:

```
PUT /api/departments/5/status?status=0
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

---

### 三、员工管理模块

#### 1. 分页查询员工列表

- **接口地址**: `GET /api/employees`
- **请求头**: 需要认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | integer | 否 | 页码，默认1 |
| size | integer | 否 | 每页条数，默认10 |
| departmentId | integer | 否 | 部门ID筛选 |
| keyword | string | 否 | 姓名/编号/手机号模糊搜索 |

- **请求示例**:

```
GET /api/employees?page=1&size=10&departmentId=1
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "employeeNo": "EMP001",
        "name": "张三",
        "departmentId": 1,
        "departmentName": "技术部",
        "position": "技术经理",
        "phone": "13800000001",
        "status": 1,
        "hireDate": null,
        "createTime": "2026-07-17T11:00:00",
        "updateTime": "2026-07-17T11:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

#### 2. 查询员工详情

- **接口地址**: `GET /api/employees/{id}`
- **请求头**: 需要认证
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | integer | 是 | 员工ID |

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "employeeNo": "EMP001",
    "name": "张三",
    "departmentId": 1,
    "departmentName": "技术部",
    "position": "技术经理",
    "phone": "13800000001",
    "status": 1,
    "hireDate": "2024-01-15",
    "createTime": "2026-07-17T11:00:00",
    "updateTime": "2026-07-17T11:00:00"
  }
}
```

---

#### 3. 新增员工

- **接口地址**: `POST /api/employees`
- **请求头**: 需要认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| employeeNo | string | 是 | 员工编号（唯一） |
| name | string | 是 | 员工姓名 |
| departmentId | integer | 否 | 所属部门ID |
| position | string | 否 | 职位 |
| phone | string | 否 | 联系方式 |
| hireDate | string | 否 | 入职日期(yyyy-MM-dd) |

- **请求示例**:

```json
{
  "employeeNo": "EMP005",
  "name": "陈七",
  "departmentId": 1,
  "position": "前端开发工程师",
  "phone": "13800000005",
  "hireDate": "2026-07-01"
}
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 5,
    "employeeNo": "EMP005",
    "name": "陈七",
    "departmentId": 1,
    "departmentName": "技术部",
    "position": "前端开发工程师",
    "phone": "13800000005",
    "status": 1,
    "hireDate": "2026-07-01",
    "createTime": "2026-07-17T12:00:00",
    "updateTime": "2026-07-17T12:00:00"
  }
}
```

---

#### 4. 编辑员工

- **接口地址**: `PUT /api/employees/{id}`
- **请求头**: 需要认证
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | integer | 是 | 员工ID |

- **请求参数**: （至少传一个字段）

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | string | 否 | 员工姓名 |
| departmentId | integer | 否 | 所属部门ID |
| position | string | 否 | 职位 |
| phone | string | 否 | 联系方式 |
| status | integer | 否 | 状态：1-在职 0-离职 |
| hireDate | string | 否 | 入职日期(yyyy-MM-dd) |

- **请求示例**:

```json
{
  "position": "高级技术经理",
  "phone": "13800000099"
}
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

---

#### 5. 开通账号

- **接口地址**: `POST /api/employees/{id}/account`
- **请求头**: 需要认证
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | integer | 是 | 员工ID |

- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | string | 否 | 登录账号（默认使用员工编号） |

- **请求示例**:

```json
{
  "username": "chenqi"
}
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "账号开通成功，默认密码为: <generated-temporary-password>",
  "data": {
    "userId": 6,
    "username": "chenqi",
    "employeeId": 5
  }
}
```

---

#### 6. 重置密码

- **接口地址**: `PUT /api/employees/account/{userId}/reset-password`
- **请求头**: 需要认证，需要ADMIN角色
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | integer | 是 | 系统用户ID |

- **响应示例**:

```json
{
  "code": 200,
  "msg": "密码已重置为: <generated-temporary-password>",
  "data": null
}
```

---

### 四、考勤管理模块

#### 1. 签到

- **接口地址**: `POST /api/attendance/check-in`
- **请求头**: 需要认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| photo | string | 否 | 签到照片（Base64） |
| latitude | number | 否 | 签到纬度 |
| longitude | number | 否 | 签到经度 |
| address | string | 否 | 签到地址描述 |

- **请求示例**:

```json
{
  "latitude": 39.9042,
  "longitude": 116.4074,
  "address": "北京市朝阳区xxx"
}
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "签到成功",
  "data": {
    "id": 1,
    "employeeId": 1,
    "workDate": "2026-07-17",
    "checkIn": "2026-07-17T09:00:00",
    "checkOut": null,
    "status": "CHECKED_IN"
  }
}
```

---

#### 2. 签退

- **接口地址**: `POST /api/attendance/check-out`
- **请求头**: 需要认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| photo | string | 否 | 签退照片（Base64） |
| latitude | number | 否 | 签退纬度 |
| longitude | number | 否 | 签退经度 |
| address | string | 否 | 签退地址描述 |

- **请求示例**:

```json
{
  "address": "北京市朝阳区xxx"
}
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "签退成功",
  "data": {
    "id": 1,
    "employeeId": 1,
    "workDate": "2026-07-17",
    "checkIn": "2026-07-17T09:00:00",
    "checkOut": "2026-07-17T18:00:00",
    "status": "CHECKED_OUT"
  }
}
```

---

#### 3. 个人考勤查询

- **接口地址**: `GET /api/attendance/records`
- **请求头**: 需要认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | integer | 否 | 页码，默认1 |
| size | integer | 否 | 每页条数，默认10 |
| startDate | string | 否 | 开始日期(yyyy-MM-dd) |
| endDate | string | 否 | 结束日期(yyyy-MM-dd) |

- **请求示例**:

```
GET /api/attendance/records?page=1&size=10&startDate=2026-07-01&endDate=2026-07-17
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "employeeId": 1,
        "employeeName": "张三",
        "workDate": "2026-07-17",
        "checkIn": "2026-07-17T09:00:00",
        "checkOut": "2026-07-17T18:00:00",
        "status": "CHECKED_OUT"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

#### 4. 管理员考勤查询

- **接口地址**: `GET /api/attendance/admin/records`
- **请求头**: 需要认证，需要ADMIN角色
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | integer | 否 | 页码，默认1 |
| size | integer | 否 | 每页条数，默认10 |
| departmentId | integer | 否 | 部门ID筛选 |
| employeeId | integer | 否 | 员工ID筛选 |
| startDate | string | 否 | 开始日期(yyyy-MM-dd) |
| endDate | string | 否 | 结束日期(yyyy-MM-dd) |
| status | string | 否 | 考勤状态筛选 |

- **请求示例**:

```
GET /api/attendance/admin/records?page=1&size=10&departmentId=1&startDate=2026-07-01&endDate=2026-07-17
```

- **响应示例**: 同个人考勤查询的分页结构

---

### 五、请假审批模块

#### 1. 提交请假申请

- **接口地址**: `POST /api/leaves`
- **请求头**: 需要认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| type | string | 是 | 请假类型：ANNUAL(年假) / SICK(病假) / PERSONAL(事假) / OTHER(其他) |
| startTime | string | 是 | 开始时间(yyyy-MM-dd HH:mm:ss) |
| endTime | string | 是 | 结束时间(yyyy-MM-dd HH:mm:ss) |
| reason | string | 是 | 请假原因 |

- **请求示例**:

```json
{
  "type": "SICK",
  "startTime": "2026-07-20 09:00:00",
  "endTime": "2026-07-21 18:00:00",
  "reason": "感冒发烧，需要休息"
}
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "提交成功",
  "data": {
    "id": 1,
    "applicantId": 1,
    "type": "SICK",
    "startTime": "2026-07-20 09:00:00",
    "endTime": "2026-07-21 18:00:00",
    "reason": "感冒发烧，需要休息",
    "status": "PENDING",
    "createTime": "2026-07-17T12:00:00",
    "updateTime": "2026-07-17T12:00:00"
  }
}
```

---

#### 2. 撤回请假申请

- **接口地址**: `POST /api/leaves/{id}/withdraw`
- **请求头**: 需要认证
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | integer | 是 | 请假申请ID |

- **响应示例**:

```json
{
  "code": 200,
  "msg": "撤回成功",
  "data": null
}
```

---

#### 3. 审批请假申请

- **接口地址**: `POST /api/leaves/{id}/audit`
- **请求头**: 需要认证（审批人）
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | integer | 是 | 请假申请ID |

- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| action | string | 是 | 审批动作：APPROVED(通过) / REJECTED(驳回) |
| comment | string | 否 | 审批意见 |

- **请求示例**:

```json
{
  "action": "APPROVED",
  "comment": "同意请假，注意休息"
}
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "审批完成",
  "data": {
    "leaveId": 1,
    "status": "APPROVED",
    "auditId": 1,
    "auditorId": 2,
    "action": "APPROVED",
    "comment": "同意请假，注意休息",
    "auditTime": "2026-07-17T14:00:00"
  }
}
```

---

#### 4. 查询申请列表

- **接口地址**: `GET /api/leaves`
- **请求头**: 需要认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | integer | 否 | 页码，默认1 |
| size | integer | 否 | 每页条数，默认10 |
| status | string | 否 | 状态筛选：PENDING / APPROVED / REJECTED / WITHDRAWN |
| startDate | string | 否 | 开始日期(yyyy-MM-dd) |
| endDate | string | 否 | 结束日期(yyyy-MM-dd) |

- **请求示例**:

```
GET /api/leaves?page=1&size=10&status=PENDING
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "applicantId": 1,
        "applicantName": "张三",
        "type": "SICK",
        "typeName": "病假",
        "startTime": "2026-07-20 09:00:00",
        "endTime": "2026-07-21 18:00:00",
        "reason": "感冒发烧",
        "status": "PENDING",
        "statusName": "待审批",
        "createTime": "2026-07-17T12:00:00",
        "updateTime": "2026-07-17T12:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

#### 5. 查询申请详情

- **接口地址**: `GET /api/leaves/{id}`
- **请求头**: 需要认证
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | integer | 是 | 请假申请ID |

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "applicantId": 1,
    "applicantName": "张三",
    "type": "SICK",
    "typeName": "病假",
    "startTime": "2026-07-20 09:00:00",
    "endTime": "2026-07-21 18:00:00",
    "reason": "感冒发烧",
    "status": "PENDING",
    "statusName": "待审批",
    "createTime": "2026-07-17T12:00:00",
    "updateTime": "2026-07-17T12:00:00",
    "auditRecords": []
  }
}
```

---

### 六、公告制度模块

#### 1. 分页查询内容列表

- **接口地址**: `GET /api/contents`
- **请求头**: 需要认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | integer | 否 | 页码，默认1 |
| size | integer | 否 | 每页条数，默认10 |
| type | string | 否 | 类型：ANNOUNCEMENT(公告) / POLICY(制度) |
| status | string | 否 | 状态筛选 |

- **请求示例**:

```
GET /api/contents?page=1&size=10&type=ANNOUNCEMENT
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "type": "ANNOUNCEMENT",
        "title": "关于2026年年中总结的通知",
        "category": "公司通知",
        "status": "PUBLISHED",
        "publisherId": 1,
        "publisherName": "admin",
        "publishTime": "2026-07-15T10:00:00",
        "version": 1,
        "viewCount": 128,
        "createTime": "2026-07-15T09:00:00",
        "updateTime": "2026-07-15T10:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

#### 2. 查询内容详情

- **接口地址**: `GET /api/contents/{id}`
- **请求头**: 需要认证
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | integer | 是 | 内容ID |

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "type": "ANNOUNCEMENT",
    "title": "关于2026年年中总结的通知",
    "category": "公司通知",
    "body": "各部门：\n\n请于7月25日前提交年中总结报告。\n\n行政部\n2026年7月15日",
    "status": "PUBLISHED",
    "scope": "ALL",
    "publisherId": 1,
    "publisherName": "admin",
    "publishTime": "2026-07-15T10:00:00",
    "version": 2,
    "createTime": "2026-07-15T09:00:00",
    "updateTime": "2026-07-16T10:00:00"
  }
}
```

---

#### 3. 创建草稿

- **接口地址**: `POST /api/contents`
- **请求头**: 需要认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| type | string | 是 | 类型：ANNOUNCEMENT(公告) / POLICY(制度) |
| title | string | 是 | 标题 |
| category | string | 否 | 分类 |
| body | string | 是 | 正文内容 |
| scope | string | 否 | 可见范围：ALL(全部) / DEPARTMENT(指定部门)，默认ALL |

- **请求示例**:

```json
{
  "type": "ANNOUNCEMENT",
  "title": "关于2026年年中总结的通知",
  "category": "公司通知",
  "body": "各部门：\n\n请于7月25日前提交年中总结报告。\n\n行政部\n2026年7月15日"
}
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "草稿保存成功",
  "data": {
    "id": 1,
    "type": "ANNOUNCEMENT",
    "title": "关于2026年年中总结的通知",
    "status": "DRAFT",
    "version": 1,
    "createTime": "2026-07-17T12:00:00",
    "updateTime": "2026-07-17T12:00:00"
  }
}
```

---

#### 4. 编辑内容

- **接口地址**: `PUT /api/contents/{id}`
- **请求头**: 需要认证
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | integer | 是 | 内容ID |

- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| title | string | 否 | 标题 |
| category | string | 否 | 分类 |
| body | string | 否 | 正文内容 |
| scope | string | 否 | 可见范围 |

- **请求示例**:

```json
{
  "title": "关于2026年年中总结的通知（修订）",
  "body": "各部门：\n\n请于7月20日前提交年中总结报告。\n\n行政部\n2026年7月17日"
}
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "更新成功",
  "data": null
}
```

---

#### 5. 发布内容

- **接口地址**: `POST /api/contents/{id}/publish`
- **请求头**: 需要认证（需要管理员权限）
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | integer | 是 | 内容ID |

- **响应示例**:

```json
{
  "code": 200,
  "msg": "发布成功",
  "data": {
    "id": 1,
    "status": "PUBLISHED",
    "publishTime": "2026-07-17T12:00:00",
    "version": 2
  }
}
```

---

#### 6. 下架内容

- **接口地址**: `POST /api/contents/{id}/unpublish`
- **请求头**: 需要认证（需要管理员权限）
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | integer | 是 | 内容ID |

- **响应示例**:

```json
{
  "code": 200,
  "msg": "下架成功",
  "data": null
}
```

---

### 七、全文检索模块

#### 1. 关键词全文检索

- **接口地址**: `GET /api/search`
- **请求头**: 需要认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | string | 是 | 搜索关键词 |
| page | integer | 否 | 页码，默认1 |
| size | integer | 否 | 每页条数，默认10 |

- **请求示例**:

```
GET /api/search?keyword=考勤制度&page=1&size=10
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "type": "POLICY",
        "title": "公司考勤管理制度",
        "highlightTitle": "公司<em>考勤</em>管理制度",
        "body": "第一条 为规范公司考勤管理...",
        "highlightBody": "第一条 为规范公司<em>考勤</em>管理...",
        "score": 2.85
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

### 八、AI智慧办公模块

#### 1. 企业制度智能问答

- **接口地址**: `POST /api/ai/qa`
- **请求头**: 需要认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| question | string | 是 | 用户问题 |

- **请求示例**:

```json
{
  "question": "请假需要提前几天申请？"
}
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "sessionId": 1,
    "question": "请假需要提前几天申请？",
    "answer": "根据《公司考勤管理制度》第三条规定：……",
    "sources": [
      {
        "contentId": 1,
        "title": "公司考勤管理制度",
        "fragmentSummary": "第三条 员工请假需提前1个工作日申请...",
        "score": 0.95
      }
    ]
  }
}
```

---

#### 2. 公告草稿辅助生成

- **接口地址**: `POST /api/ai/announcement-draft`
- **请求头**: 需要认证
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| topic | string | 是 | 公告主题 |
| keyPoints | string[] | 否 | 要点列表 |
| style | string | 否 | 风格：FORMAL(正式) / CONCISE(简洁)，默认FORMAL |

- **请求示例**:

```json
{
  "topic": "2026年中秋节放假通知",
  "keyPoints": ["10月1日至3日放假", "各部门做好安全检查"],
  "style": "FORMAL"
}
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "draftTitle": "关于2026年中秋节放假安排的通知",
    "draftBody": "各部门：\n\n根据国家法定节假日规定...\n\n行政部\n2026年7月17日"
  }
}
```

---

#### 3. 企业知识库（RAG）说明

`更新.sql` 引入企业知识库（RAG）相关表，用于 AI 问答的知识来源管理：

| 表名 | 说明 |
|------|------|
| `oa_ai_source` | RAG 企业知识来源表，存储上传的 Word/PDF 文档信息，含文件哈希去重、解析状态、索引状态、访问权限控制 |
| `oa_ai_source_chunk` | RAG 知识文档分片表，存储文档解析后的文本分片，含字符数、向量Key、元数据JSON等 |
| `oa_ai_citation` | AI回答引用记录（原 `oa_ai_source` 重命名），记录每次 AI 回答引用的来源，支持 CONTENT(公告) 和 KNOWLEDGE_FILE(知识文件) 两种引用类型 |

- 支持文档分类（企业文化/新人培训/技术部资料/行政管理等）
- 支持访问权限控制：全公司可见/指定部门可见/按角色等级
- 文档状态：`PENDING` → `PROCESSING` → `SUCCESS` / `FAILED`
- 索引状态：`PENDING` → `PROCESSING` → `SUCCESS` / `FAILED`

---

### 九、运维管理模块

#### 1. 基础数据看板

- **接口地址**: `GET /api/admin/dashboard`
- **请求头**: 需要认证，需要ADMIN角色
- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "employeeCount": 4,
    "departmentCount": 4,
    "todayAttendanceRate": "75%",
    "pendingLeaveCount": 2,
    "publishedContentCount": 5
  }
}
```

---

#### 2. 操作日志查询

- **接口地址**: `GET /api/admin/logs`
- **请求头**: 需要认证，需要ADMIN角色
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | integer | 否 | 页码，默认1 |
| size | integer | 否 | 每页条数，默认10 |
| module | string | 否 | 操作模块筛选 |
| operatorId | integer | 否 | 操作人ID筛选 |
| startDate | string | 否 | 开始日期(yyyy-MM-dd) |
| endDate | string | 否 | 结束日期(yyyy-MM-dd) |

- **请求示例**:

```
GET /api/admin/logs?page=1&size=10&module=部门管理
```

- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "operatorId": 1,
        "operatorName": "admin",
        "module": "部门管理",
        "action": "新增部门",
        "targetId": "5",
        "result": "SUCCESS",
        "detail": "新增部门：市场部",
        "traceId": "abc123",
        "createTime": "2026-07-17T12:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

#### 3. 全量索引重建

- **接口地址**: `POST /api/admin/index/rebuild`
- **请求头**: 需要认证，需要ADMIN角色
- **响应示例**:

```json
{
  "code": 200,
  "msg": "索引重建任务已提交",
  "data": {
    "taskId": "rebuild-20260717-001",
    "status": "PROCESSING"
  }
}
```

---

#### 4. Nacos动态配置刷新演示

- **接口地址**: `GET /api/admin/config/demo`
- **请求头**: 需要认证
- **响应示例**:

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "configKey": "example.key",
    "configValue": "示例配置值"
  }
}
```

---

## 附录

### 枚举值汇总

#### 用户角色

| 值 | 说明 |
|----|------|
| SUPER_ADMIN | 总管理员（原ADMIN迁移） |
| DEPT_MANAGER | 部门经理（预留） |
| EMPLOYEE | 普通员工 |

#### 请假类型

| 值 | 说明 |
|----|------|
| ANNUAL | 年假 |
| SICK | 病假 |
| PERSONAL | 事假 |
| OTHER | 其他 |

#### 请假状态

| 值 | 说明 |
|----|------|
| PENDING | 待审批 |
| APPROVED | 已通过 |
| REJECTED | 已驳回 |
| WITHDRAWN | 已撤回 |

#### 考勤状态

| 值 | 说明 |
|----|------|
| UNCHECKED | 未签到 |
| CHECKED_IN | 已签到 |
| CHECKED_OUT | 已签退 |

#### 内容类型

| 值 | 说明 |
|----|------|
| ANNOUNCEMENT | 公告 |
| POLICY | 制度 |

#### 内容状态

| 值 | 说明 |
|----|------|
| DRAFT | 草稿 |
| PUBLISHED | 已发布 |
| UNPUBLISHED | 已下架 |

#### 审批动作

| 值 | 说明 |
|----|------|
| APPROVED | 通过 |
| REJECTED | 驳回 |

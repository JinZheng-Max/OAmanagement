<template>
  <div class="attendance-container">
    <!-- 页头标题与说明 -->
    <div class="page-header">
      <div class="header-top">
        <div>
          <h2 class="page-title">⏰ 考勤管理中心</h2>
          <p class="page-subtitle">部门独立考勤规则维护、多场次打卡控制、多级分流补签审批与数据看板追溯</p>
        </div>
      </div>
    </div>

    <!-- 顶部实时打卡控制台 (普通员工与部门经理可用，最高管理员免除签到打卡) -->
    <div class="employee-dashboard" v-if="!isSuperAdmin">
      <div class="clock-section">
        <div class="clock-card">
          <div class="clock-glow"></div>
          <div class="clock-header">
            <el-icon class="clock-icon"><Clock /></el-icon>
            <span class="clock-title">考勤打卡控制台</span>
          </div>
          <div class="clock-time">{{ timeStr }}</div>
          <div class="clock-date">{{ dateStr }}</div>
          <div class="clock-btns">
            <el-button type="primary" size="large" @click="handleClock('check-in')" class="clock-btn">
              <el-icon><CircleCheck /></el-icon>
              <span>上班签到</span>
            </el-button>
            <el-button type="warning" size="large" @click="handleClock('check-out')" class="clock-btn">
              <el-icon><CircleClose /></el-icon>
              <span>下班签退</span>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 今日打卡任务卡片 -->
      <div class="today-tasks-section">
        <div class="tasks-card">
          <div class="card-header">
            <span class="card-title">📌 今日打卡任务</span>
            <span class="card-subtitle">当前场次规则要求</span>
          </div>
          <div v-if="todayTasks.length > 0" class="tasks-list">
            <div v-for="task in todayTasks" :key="task.id" class="task-item">
              <div class="task-info">
                <span class="task-session">{{ task.sessionName || '常规场次' }}</span>
                <span class="task-time">
                  签到窗口: {{ task.checkInStartTime }} ~ {{ task.normalCheckInEndTime }}
                </span>
              </div>
              <div class="task-status">
                <el-tag :type="getStatusBadge(task.status, task.replenishStatus).type" size="small">
                  {{ getStatusBadge(task.status, task.replenishStatus).label }}
                </el-tag>
              </div>
            </div>
          </div>
          <el-empty v-else description="今日暂无考勤打卡任务" :image-size="70" />
        </div>
      </div>
    </div>

    <!-- 最高管理员专属免打卡提示 -->
    <el-alert
      v-else
      title="👑 您当前为最高管理员，免除每日日常签到打卡要求。可直接在下方维护全公司考勤看板、规则与终审补签申请。"
      type="success"
      show-icon
      :closable="false"
      style="margin-bottom: 20px; border-radius: 12px;"
    />

    <!-- 考勤功能主页 Tab 切换 -->
    <div class="view-tab-switcher">
      <button 
        v-if="!isSuperAdmin"
        class="tab-btn" 
        :class="{ active: activeTab === 'personal' }" 
        @click="activeTab = 'personal'"
      >
        <span class="tab-icon">📅</span>
        <span>我的考勤记录</span>
      </button>
      <button 
        v-if="isDeptManager || isSuperAdmin"
        class="tab-btn" 
        :class="{ active: activeTab === 'admin' }" 
        @click="activeTab = 'admin'"
      >
        <span class="tab-icon">📊</span>
        <span>部门考勤看板与发布</span>
      </button>
      <button 
        v-if="isDeptManager || isSuperAdmin"
        class="tab-btn" 
        :class="{ active: activeTab === 'rules' }" 
        @click="activeTab = 'rules'"
      >
        <span class="tab-icon">⚙️</span>
        <span>部门考勤规则维护</span>
      </button>
      <button 
        v-if="isDeptManager || isSuperAdmin"
        class="tab-btn" 
        :class="{ active: activeTab === 'replenish' }" 
        @click="activeTab = 'replenish'"
      >
        <span class="tab-icon">✍️</span>
        <span>考勤补签多级审批</span>
        <span class="badge" v-if="pendingReplenishCount > 0">{{ pendingReplenishCount }}</span>
      </button>
    </div>

    <!-- ===== Tab 1: 我的考勤记录 ===== -->
    <div v-if="activeTab === 'personal' && !isSuperAdmin" class="tab-pane-content">
      <el-card class="glass-card">
        <div class="filter-card">
          <div class="filter-row">
            <div class="filter-group">
              <span class="filter-label">日期筛选：</span>
              <el-date-picker
                v-model="personalDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                @change="fetchPersonalRecords"
              />
            </div>
            <el-button type="primary" @click="fetchPersonalRecords">查询</el-button>
          </div>
        </div>

        <el-table v-loading="personalLoading" :data="personalList" stripe style="width: 100%; margin-top: 16px;">
          <el-table-column prop="workDate" label="工作日期" width="130" />
          <el-table-column prop="sessionName" label="考勤场次" width="120" />
          <el-table-column prop="checkIn" label="签到打卡时间" min-width="160">
            <template #default="{ row }">
              <span v-if="row.checkIn">{{ row.checkIn }} <small v-if="row.checkInIp" class="text-muted">({{ row.checkInIp }})</small></span>
              <span v-else class="text-muted">--</span>
            </template>
          </el-table-column>
          <el-table-column prop="checkOut" label="签退打卡时间" min-width="160">
            <template #default="{ row }">
              <span v-if="row.checkOut">{{ row.checkOut }} <small v-if="row.checkOutIp" class="text-muted">({{ row.checkOutIp }})</small></span>
              <span v-else class="text-muted">--</span>
            </template>
          </el-table-column>
          <el-table-column label="考勤状态" width="130">
            <template #default="{ row }">
              <el-tag :type="getStatusBadge(row.status, row.replenishStatus).type" size="small">
                {{ getStatusBadge(row.status, row.replenishStatus).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="补签申请说明 / 审批意见" min-width="220">
            <template #default="{ row }">
              <div v-if="row.replenishReason" style="font-size: 13px;">
                <p style="margin: 0; color: #475569;"><strong>原因：</strong>{{ row.replenishReason }}</p>
                <p v-if="row.approveComment" style="margin: 2px 0 0 0; color: #0284c7;">
                  <strong>审批：</strong>{{ row.approveComment }} <span v-if="row.approverName">({{ row.approverName }})</span>
                </p>
              </div>
              <span v-else class="text-muted">--</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="['UNCHECKED', 'LATE', 'EARLY_LEAVE', 'ABSENT'].includes(row.status) && row.replenishStatus !== 'PENDING'"
                type="warning"
                size="small"
                @click="openApplyModal(row)"
                class="replenish-btn"
              >
                申请补签
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="personalPage"
            v-model:page-size="personalSize"
            :total="personalTotal"
            layout="total, prev, pager, next, jumper"
            @current-change="fetchPersonalRecords"
          />
        </div>
      </el-card>
    </div>

    <!-- ===== Tab 2: 部门考勤看板与发布 ===== -->
    <div v-if="activeTab === 'admin' && (isDeptManager || isSuperAdmin)" class="tab-pane-content">
      <el-card class="glass-card">
        <div class="filter-card">
          <div class="filter-row">
            <div class="filter-group" v-if="isSuperAdmin">
              <span class="filter-label">所属部门：</span>
              <el-select v-model="adminDeptId" placeholder="全部部门" clearable style="width: 160px" @change="fetchAdminRecords">
                <el-option label="全部部门" :value="undefined" />
                <el-option v-for="d in departmentList" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </div>

            <div class="filter-group">
              <span class="filter-label">状态筛选：</span>
              <el-select v-model="adminStatus" placeholder="全部状态" clearable style="width: 140px" @change="fetchAdminRecords">
                <el-option label="正常签到" value="CHECKED_IN" />
                <el-option label="迟到" value="LATE" />
                <el-option label="早退" value="EARLY_LEAVE" />
                <el-option label="正常签退" value="CHECKED_OUT" />
                <el-option label="未打卡" value="UNCHECKED" />
                <el-option label="已补签" value="REPLENISHED" />
              </el-select>
            </div>

            <div class="filter-group">
              <span class="filter-label">日期范围：</span>
              <el-date-picker
                v-model="adminDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                @change="fetchAdminRecords"
              />
            </div>

            <el-button type="primary" @click="fetchAdminRecords">刷新看板</el-button>
          </div>
        </div>

        <el-table v-loading="adminLoading" :data="adminList" stripe style="width: 100%; margin-top: 16px;">
          <el-table-column prop="workDate" label="工作日期" width="130" />
          <el-table-column prop="employeeName" label="员工姓名" width="130" />
          <el-table-column prop="departmentName" label="部门" width="140" />
          <el-table-column prop="sessionName" label="考勤场次" width="120" />
          <el-table-column prop="checkIn" label="签到时间" min-width="160">
            <template #default="{ row }">
              <span v-if="row.checkIn">{{ row.checkIn }}</span>
              <span v-else class="text-muted">未签到</span>
            </template>
          </el-table-column>
          <el-table-column prop="checkOut" label="签退时间" min-width="160">
            <template #default="{ row }">
              <span v-if="row.checkOut">{{ row.checkOut }}</span>
              <span v-else class="text-muted">未签退</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <el-tag :type="getStatusBadge(row.status, row.replenishStatus).type" size="small">
                {{ getStatusBadge(row.status, row.replenishStatus).label }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="adminPage"
            v-model:page-size="adminSize"
            :total="adminTotal"
            :page-sizes="[5, 10]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="fetchAdminRecords"
            @size-change="fetchAdminRecords"
          />
        </div>
      </el-card>
    </div>

    <!-- ===== Tab 3: 部门独立考勤规则配置 ===== -->
    <div v-if="activeTab === 'rules' && (isDeptManager || isSuperAdmin)" class="tab-pane-content">
      <el-card class="glass-card">
        <div class="filter-card">
          <div class="filter-row">
            <div class="filter-group" v-if="isSuperAdmin">
              <span class="filter-label">部门选择：</span>
              <el-select v-model="filterRuleDeptId" placeholder="全部部门规则" clearable style="width: 180px" @change="fetchRules">
                <el-option label="全部部门" :value="undefined" />
                <el-option v-for="d in departmentList" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </div>
            <el-button type="primary" @click="fetchRules">刷新规则</el-button>
            <el-button type="success" style="margin-left: auto;" @click="openAddRuleModal">
              <el-icon><Plus /></el-icon>
              添加场次规则
            </el-button>
          </div>
        </div>

        <el-table v-loading="ruleLoading" :data="paginatedRuleList" stripe style="width: 100%; margin-top: 16px;">
          <el-table-column prop="departmentName" label="所属部门" width="150" />
          <el-table-column prop="sessionName" label="考勤场次名称" width="140" />
          <el-table-column label="签到窗口 (允许开始 ~ 正常截止 ~ 最晚)" min-width="280">
            <template #default="{ row }">
              <span style="color: #10b981; font-weight: 600;">{{ row.checkInStartTime }}</span>
              ~ 正常 <span style="color: #f59e0b; font-weight: 600;">{{ row.normalCheckInEndTime }}</span>
              ~ 最晚 {{ row.checkInEndTime }}
            </template>
          </el-table-column>
          <el-table-column label="签退窗口 (正常最早 ~ 最晚)" min-width="240">
            <template #default="{ row }">
              正常最早 <span style="color: #3b82f6; font-weight: 600;">{{ row.normalCheckOutStartTime }}</span>
              ~ 最晚 {{ row.checkOutEndTime }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
                {{ row.enabled === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="openEditRuleModal(row)">编辑</el-button>
              <el-button type="danger" size="small" link @click="handleDeleteRule(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="ruleCurrentPage"
            v-model:page-size="rulePageSize"
            :page-sizes="[5, 10]"
            :total="ruleList.length"
            layout="total, sizes, prev, pager, next, jumper"
          />
        </div>
      </el-card>
    </div>

    <!-- ===== Tab 4: 考勤补签多级分流审批 ===== -->
    <div v-if="activeTab === 'replenish' && (isDeptManager || isSuperAdmin)" class="tab-pane-content">
      <el-card class="glass-card">
        <!-- 部门经理子 Tab 切换：【本部门员工补签审批】与【我的个人补签记录】 -->
        <div v-if="isDeptManager" class="sub-tab-bar" style="margin-bottom: 16px;">
          <el-radio-group v-model="deptManagerSubTab" @change="fetchReplenishRecords">
            <el-radio-button label="APPROVAL">📋 本部门员工补签审批</el-radio-button>
            <el-radio-button label="MY_RECORDS">👤 我的个人补签申请</el-radio-button>
          </el-radio-group>
        </div>

        <div class="filter-card">
          <div class="filter-row">
            <div class="filter-group">
              <span class="filter-label">审批状态：</span>
              <el-select v-model="filterReplenishStatus" placeholder="审批状态" clearable style="width: 140px" @change="fetchReplenishRecords">
                <el-option label="待审批" value="PENDING" />
                <el-option label="已通过" value="APPROVED" />
                <el-option label="已驳回" value="REJECTED" />
              </el-select>
            </div>
            <el-button type="primary" @click="fetchReplenishRecords">刷新列表</el-button>
          </div>
        </div>

        <el-table v-loading="replenishListLoading" :data="displayedReplenishList" stripe style="width: 100%; margin-top: 16px;">
          <el-table-column prop="workDate" label="工作日期" width="120" />
          <el-table-column prop="employeeName" label="申请人" width="120">
            <template #default="{ row }">
              <span>{{ row.employeeName }}</span>
              <el-tag v-if="isDeptManagerUser(row)" type="warning" size="small" style="margin-left: 4px;">部门经理</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="departmentName" label="部门" width="130" />
          <el-table-column prop="sessionName" label="考勤场次" width="110" />
          <el-table-column prop="replenishReason" label="补签原因说明" min-width="200" />
          <el-table-column label="审批状态" width="150">
            <template #default="{ row }">
              <el-tag :type="getStatusBadge(row.status, row.replenishStatus).type" size="small">
                {{ getStatusBadge(row.status, row.replenishStatus).label }}
              </el-tag>
              <el-tag v-if="isEscalated(row)" type="danger" size="small" style="margin-left: 4px;">📌 呈报高管</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="approverName" label="审批人" width="120" />
          <el-table-column prop="approveComment" label="审批意见" min-width="160" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <!-- 一级分流控制：待审批状态且可审核时展示相应操作 -->
              <template v-if="isPending(row) && canAudit(row)">
                <el-button type="success" size="small" @click="handleQuickApprove(row)">
                  同意
                </el-button>
                <el-button type="danger" size="small" @click="openRejectModal(row)">
                  驳回
                </el-button>
                <el-button v-if="isDeptManager && deptManagerSubTab === 'APPROVAL'" type="warning" size="small" plain @click="openEscalateModal(row)">
                  上报高管
                </el-button>
              </template>
              <span v-else-if="isPending(row) && isDeptManager && deptManagerSubTab === 'MY_RECORDS'" class="text-muted" style="font-size: 12px;">
                ⏳ 等待高管审批
              </span>
              <span v-else class="text-muted" style="font-size: 12px;">已归档</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="replenishPage"
            v-model:page-size="replenishSize"
            :total="replenishTotal"
            layout="total, prev, pager, next, jumper"
            @current-change="fetchReplenishRecords"
          />
        </div>
      </el-card>
    </div>

    <!-- ===== 1. 提交补签 弹窗 ===== -->
    <el-dialog v-model="replenishDialogVisible" title="提交考勤补签申请" width="480px" append-to-body align-center :close-on-click-modal="false">
      <div v-if="targetRecord">
        <el-descriptions :column="1" border size="small" style="margin-bottom: 16px;">
          <el-descriptions-item label="工作日期">{{ targetRecord.workDate }}</el-descriptions-item>
          <el-descriptions-item label="考勤场次">{{ targetRecord.sessionName }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusBadge(targetRecord.status, targetRecord.replenishStatus).type" size="small">
              {{ getStatusBadge(targetRecord.status, targetRecord.replenishStatus).label }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top">
          <el-form-item label="补签原因说明" required>
            <el-input v-model="applyReason" type="textarea" :rows="4" placeholder="请输入因公无法即时打卡或系统异常的原因说明..." />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="replenishDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="applyLoading" @click="handleApplySubmit">确认提交</el-button>
      </template>
    </el-dialog>

    <!-- ===== 2. 规则编辑/添加 弹窗 ===== -->
    <el-dialog v-model="ruleDialogVisible" :title="ruleForm.id ? '编辑部门考勤规则' : '添加部门考勤规则'" width="560px" append-to-body align-center :close-on-click-modal="false">
      <el-form :model="ruleForm" label-width="140px">
        <el-form-item label="所属部门" required>
          <el-select 
            v-model="ruleForm.departmentId" 
            style="width: 100%" 
            :disabled="isDeptManager && !isSuperAdmin"
            placeholder="请选择部门"
          >
            <el-option v-for="d in departmentList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
          <div v-if="isDeptManager && !isSuperAdmin" class="text-muted" style="font-size: 12px; margin-top: 4px; color: #e6a23c;">
            🔒 部门经理仅可维护您所属部门的考勤场次规则
          </div>
        </el-form-item>

        <el-form-item label="考勤场次名称" required>
          <el-input v-model="ruleForm.sessionName" placeholder="如：上午场 / 下午场 / 晚班场" />
        </el-form-item>

        <el-form-item label="允许签到开始" required>
          <el-time-picker v-model="ruleForm.checkInStartTime" value-format="HH:mm" format="HH:mm" placeholder="08:50" style="width: 100%" />
        </el-form-item>

        <el-form-item label="正常签到截止" required>
          <el-time-picker v-model="ruleForm.normalCheckInEndTime" value-format="HH:mm" format="HH:mm" placeholder="09:10 (超时计迟到)" style="width: 100%" />
        </el-form-item>

        <el-form-item label="签到最晚截止" required>
          <el-time-picker v-model="ruleForm.checkInEndTime" value-format="HH:mm" format="HH:mm" placeholder="12:10" style="width: 100%" />
        </el-form-item>

        <el-form-item label="正常签退最早" required>
          <el-time-picker v-model="ruleForm.normalCheckOutStartTime" value-format="HH:mm" format="HH:mm" placeholder="11:50 (早于计早退)" style="width: 100%" />
        </el-form-item>

        <el-form-item label="签退最晚截止" required>
          <el-time-picker v-model="ruleForm.checkOutEndTime" value-format="HH:mm" format="HH:mm" placeholder="12:10" style="width: 100%" />
        </el-form-item>

        <el-form-item label="启用状态">
          <el-radio-group v-model="ruleForm.enabled">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ruleFormLoading" @click="handleSaveRule">保存规则</el-button>
      </template>
    </el-dialog>

    <!-- ===== 3. 驳回理由 弹窗 (强校验必填) ===== -->
    <el-dialog v-model="rejectModalVisible" title="❌ 驳回考勤补签申请" width="480px" append-to-body align-center :close-on-click-modal="false">
      <div v-if="currentAuditRecord">
        <el-descriptions :column="1" border size="small" style="margin-bottom: 16px;">
          <el-descriptions-item label="申请人">{{ currentAuditRecord.employeeName }} ({{ currentAuditRecord.departmentName }})</el-descriptions-item>
          <el-descriptions-item label="工作日期">{{ currentAuditRecord.workDate }}</el-descriptions-item>
          <el-descriptions-item label="补签原因">{{ currentAuditRecord.replenishReason }}</el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top">
          <el-form-item label="驳回原因 (必填强校验)" required>
            <el-input v-model="rejectionReason" type="textarea" :rows="3" placeholder="请输入明确的驳回原由与打卡核查说明..." />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="rejectModalVisible = false">取消</el-button>
        <el-button type="danger" :loading="approveSubmitLoading" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- ===== 5. 呈报最高管理员 弹窗 ===== -->
    <el-dialog v-model="escalateModalVisible" title="📌 呈报最高管理员批示" width="480px" append-to-body align-center :close-on-click-modal="false">
      <div v-if="currentAuditRecord">
        <el-descriptions :column="1" border size="small" style="margin-bottom: 16px;">
          <el-descriptions-item label="申请人">{{ currentAuditRecord.employeeName }} ({{ currentAuditRecord.departmentName }})</el-descriptions-item>
          <el-descriptions-item label="工作日期">{{ currentAuditRecord.workDate }}</el-descriptions-item>
          <el-descriptions-item label="补签原因">{{ currentAuditRecord.replenishReason }}</el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top">
          <el-form-item label="呈报说明 (可选)">
            <el-input v-model="escalationNote" type="textarea" :rows="3" placeholder="请输入呈报高管批示的原因，如涉及特殊因公出差或考勤跨部门调整..." />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="escalateModalVisible = false">取消</el-button>
        <el-button type="warning" :loading="approveSubmitLoading" @click="submitEscalate">确认呈报高管</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, CircleCheck, CircleClose, User, Plus } from '@element-plus/icons-vue'
import {
  getTodayPersonalTasks,
  checkIn,
  checkOut,
  applyReplenishment,
  approveReplenishment,
  getReplenishRecords,
  getPersonalRecords,
  getAdminRecords,
  getDepartmentRules,
  saveDepartmentRule,
  deleteDepartmentRule
} from '../api/attendance'
import { listActiveDepartments, getDepartmentPage } from '../api/org'

// 时间与角色
const timeStr = ref(new Date().toLocaleTimeString())
const dateStr = ref(new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }))
const storedRole = ref(localStorage.getItem('role') || 'EMPLOYEE')
const currentUserId = ref(Number(localStorage.getItem('userId') || '0'))
const currentEmpId = ref(Number(localStorage.getItem('employeeId') || '0'))

const isSuperAdmin = computed(() => storedRole.value === 'SUPER_ADMIN' || storedRole.value === 'ADMIN')
const isDeptManager = computed(() => storedRole.value === 'DEPT_MANAGER')

// 初始默认选中的选项卡
const activeTab = ref(isSuperAdmin.value ? 'admin' : 'personal')
const deptManagerSubTab = ref('APPROVAL') // 部门经理视图: 'APPROVAL' | 'MY_RECORDS'

// 基础数据
const departmentList = ref([])
const todayTasks = ref([])

async function loadDepartments() {
  try {
    const res = await listActiveDepartments().catch(() => null)
    let rawList = []
    if (res) {
      if (Array.isArray(res)) rawList = res
      else if (Array.isArray(res.data)) rawList = res.data
      else if (res.data && Array.isArray(res.data.records)) rawList = res.data.records
    }
    
    if (rawList.length === 0) {
      const pageRes = await getDepartmentPage({ page: 1, size: 100 }).catch(() => null)
      if (pageRes) {
        if (Array.isArray(pageRes.data?.records)) rawList = pageRes.data.records
        else if (Array.isArray(pageRes.data)) rawList = pageRes.data
        else if (Array.isArray(pageRes)) rawList = pageRes
      }
    }

    // 严苛过滤：只保留包含合规 id 与中文 name 的对象，彻底排除任何杂乱数据或裸数字 1
    departmentList.value = rawList.filter(item => item && typeof item === 'object' && item.id && item.name && typeof item.name === 'string')
  } catch (e) {
    console.error('加载部门数据失败', e)
  }
}

async function loadTodayTasks() {
  if (isSuperAdmin.value) return
  try {
    const res = await getTodayPersonalTasks()
    if (res && res.data) {
      todayTasks.value = res.data
      if (todayTasks.value.length === 0) {
        setTimeout(async () => {
          const retry = await getTodayPersonalTasks().catch(() => null)
          if (retry && retry.data && retry.data.length > 0) {
            todayTasks.value = retry.data
          }
        }, 400)
      }
    }
  } catch (err) {
    console.error('加载今日打卡任务失败', err)
  }
}

// 智能匹配当前时间对应的最合适打卡场次
function selectTargetTask(tasks, type) {
  if (!tasks || tasks.length === 0) return null
  const now = new Date()
  const nowHHmm = now.toTimeString().slice(0, 5) // "14:16"

  // 过滤未完成当前操作（签到/签退）的场次任务
  const uncompleted = tasks.filter(t => {
    if (type === 'check-in') {
      return !t.checkIn && t.status !== 'CHECKED_IN' && t.status !== 'LATE'
    } else {
      return !t.checkOut && t.status !== 'CHECKED_OUT' && t.status !== 'EARLY_LEAVE'
    }
  })

  // 如果所有场次均已完成该操作，返回 null，由外层给出明确提示，不再盲目退回 tasks[0] (即上午场)
  if (uncompleted.length === 0) {
    return null
  }

  // 1. 优先精准寻找当前系统时间落在该场次开放窗口 (checkInStartTime ~ checkOutEndTime / checkInEndTime) 内的任务
  const activeTask = uncompleted.find(t => {
    const start = t.checkInStartTime || '00:00'
    const end = (t.checkOutEndTime && t.checkOutEndTime > start) ? t.checkOutEndTime : (t.checkInEndTime || '23:59')
    return nowHHmm >= start && nowHHmm <= end
  })
  if (activeTask) return activeTask

  // 2. 如果没有精准落入窗口的，寻找开场时间早于或等于当前时间的最晚场次（如下午 14 点优先选下午场而不是上午场）
  const pastTasks = uncompleted.filter(t => nowHHmm >= (t.checkInStartTime || '00:00'))
  if (pastTasks.length > 0) {
    // 按开始时间倒序取最后一个最符合当前时间的场次
    pastTasks.sort((a, b) => (b.checkInStartTime || '00:00').localeCompare(a.checkInStartTime || '00:00'))
    return pastTasks[0]
  }

  return uncompleted[0]
}

const handleClock = async (type) => {
  try {
    const tasksRes = await getTodayPersonalTasks()
    const tasks = tasksRes ? tasksRes.data || [] : []
    if (tasks.length === 0) {
      return ElMessage.warning('今天暂无签到任务卡片，系统已自动初始化该部门考勤规则，请刷新重试！')
    }
    
    let targetTask = selectTargetTask(tasks, type)
    
    // 若 targetTask 为 null，说明今天的所有场次都已经完成了此项操作（签到/签退）
    if (!targetTask) {
      if (type === 'check-in') {
        return ElMessage.warning('今日所有考勤场次均已完成【签到】，请勿重复打卡！')
      } else {
        return ElMessage.warning('今日所有考勤场次均已完成【签退】，无需重复打卡！')
      }
    }

    if (type === 'check-in') {
      const nowHHmm = new Date().toTimeString().slice(0, 5)
      const checkInStart = targetTask.checkInStartTime || '00:00'
      const checkInEnd = targetTask.checkInEndTime || '23:59'
      if (nowHHmm < checkInStart) {
        return ElMessage.warning(`非签到窗口期，禁止签到（签到开始时间：${checkInStart}）`)
      }
      if (nowHHmm > checkInEnd) {
        return ElMessage.warning(`非签到窗口期，禁止签到（签到截止时间：${checkInEnd}）`)
      }
      const res = await checkIn(targetTask.id)
      if (res && res.code === 200) {
        if (res.data && res.data.status === 'LATE') {
          ElMessage.warning(`【${targetTask.sessionName || '常规场次'}】签到成功！由于已超正常截止点，已判定为【迟到】`)
        } else {
          ElMessage.success(`【${targetTask.sessionName || '常规场次'}】签到成功！`)
        }
      }
    } else {
      const now = new Date()
      const nowHHmm = now.toTimeString().slice(0, 5)
      const checkOutStart = targetTask.checkOutStartTime || '00:00'
      const checkOutEnd = targetTask.checkOutEndTime || '23:59'
      if (nowHHmm < checkOutStart) {
        return ElMessage.warning(`非签退窗口期，禁止签退（签退开始时间：${checkOutStart}）`)
      }
      if (nowHHmm > checkOutEnd) {
        return ElMessage.warning(`非签退窗口期，禁止签退（签退截止时间：${checkOutEnd}）`)
      }

      const res = await checkOut(targetTask.id)
      if (res && res.code === 200) {
        if (res.data && res.data.status === 'EARLY_LEAVE') {
          ElMessage.warning(`【${targetTask.sessionName || '常规场次'}】签退成功！记录为【早退】`)
        } else {
          ElMessage.success(`【${targetTask.sessionName || '常规场次'}】签退成功！`)
        }
      }
    }
    loadTodayTasks()
    fetchPersonalRecords()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '打卡异常')
  }
}

// ================= Tab 1: 我的考勤记录 =================
const personalLoading = ref(false)
const personalList = ref([])
const personalTotal = ref(0)
const personalPage = ref(1)
const personalSize = ref(10)
const personalDateRange = ref(null)

async function fetchPersonalRecords() {
  if (isSuperAdmin.value) return
  personalLoading.value = true
  try {
    const res = await getPersonalRecords({
      page: personalPage.value,
      size: personalSize.value,
      startDate: personalDateRange.value ? personalDateRange.value[0] : undefined,
      endDate: personalDateRange.value ? personalDateRange.value[1] : undefined
    })
    if (res && res.data) {
      personalList.value = res.data.records || []
      personalTotal.value = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '查询个人记录失败')
  } finally {
    personalLoading.value = false
  }
}

// 补签申请弹窗
const replenishDialogVisible = ref(false)
const targetRecord = ref(null)
const applyReason = ref('')
const applyLoading = ref(false)

function openApplyModal(record) {
  targetRecord.value = record
  applyReason.value = ''
  replenishDialogVisible.value = true
}

async function handleApplySubmit() {
  if (!targetRecord.value) return
  if (!applyReason.value.trim()) {
    ElMessage.warning('请输入补签申请原因说明')
    return
  }
  applyLoading.value = true
  try {
    const res = await applyReplenishment(targetRecord.value.id, applyReason.value.trim())
    if (res && res.code === 200) {
      ElMessage.success('补签申请提交成功，请等待主管审批')
      replenishDialogVisible.value = false
      fetchPersonalRecords()
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '申请提交失败')
  } finally {
    applyLoading.value = false
  }
}

// ================= Tab 2: 部门考勤看板与发布 =================
const adminLoading = ref(false)
const adminList = ref([])
const adminTotal = ref(0)
const adminPage = ref(1)
const adminSize = ref(10)
const adminDeptId = ref(undefined)
const adminStatus = ref('')
const adminDateRange = ref(null)

const departmentStats = computed(() => {
  const stats = {}
  departmentList.value.forEach(dept => {
    stats[String(dept.id)] = { name: dept.name, total: 0, checkedIn: 0, unchecked: 0 }
  })
  adminList.value.forEach(record => {
    const deptId = String(record.departmentId || 1)
    if (!stats[deptId]) {
      const dept = departmentList.value.find(d => String(d.id) === deptId)
      stats[deptId] = { name: dept ? dept.name : record.departmentName || ('部门 ' + deptId), total: 0, checkedIn: 0, unchecked: 0 }
    }
    stats[deptId].total++
    if (['CHECKED_IN', 'CHECKED_OUT', 'REPLENISHED', 'LATE', 'EARLY_LEAVE'].includes(record.status)) {
      stats[deptId].checkedIn++
    } else {
      stats[deptId].unchecked++
    }
  })
  return Object.values(stats).map(data => ({
    department: data.name,
    total: data.total,
    checkedIn: data.checkedIn,
    unchecked: data.unchecked,
    rate: data.total > 0 ? Math.round((data.checkedIn / data.total) * 100) : 0
  }))
})

async function fetchAdminRecords() {
  adminLoading.value = true
  try {
    const hasFilter = adminDeptId.value || adminStatus.value || adminDateRange.value
    const fetchSize = hasFilter ? adminSize.value : 500
    
    const res = await getAdminRecords({
      page: adminPage.value,
      size: fetchSize,
      departmentId: adminDeptId.value,
      status: adminStatus.value || undefined,
      startDate: adminDateRange.value ? adminDateRange.value[0] : undefined,
      endDate: adminDateRange.value ? adminDateRange.value[1] : undefined
    })
    if (res && res.data) {
      adminList.value = res.data.records || []
      adminTotal.value = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '查询考勤看板失败')
  } finally {
    adminLoading.value = false
  }
}

// ================= Tab 3: 部门独立考勤规则维护 =================
const ruleLoading = ref(false)
const ruleList = ref([])
const filterRuleDeptId = ref(undefined)
const ruleCurrentPage = ref(1)
const rulePageSize = ref(5)

const paginatedRuleList = computed(() => {
  const start = (ruleCurrentPage.value - 1) * rulePageSize.value
  const end = start + rulePageSize.value
  return ruleList.value.slice(start, end)
})

const ruleDialogVisible = ref(false)
const ruleForm = ref({
  departmentId: undefined,
  sessionName: '上午场',
  checkInStartTime: '08:50',
  normalCheckInEndTime: '09:10',
  checkInEndTime: '12:10',
  normalCheckOutStartTime: '11:50',
  checkOutEndTime: '12:10',
  enabled: 1
})
const ruleFormLoading = ref(false)

async function fetchRules() {
  ruleLoading.value = true
  try {
    const res = await getDepartmentRules(filterRuleDeptId.value)
    if (res && res.data) {
      ruleList.value = res.data || []
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '获取规则失败')
  } finally {
    ruleLoading.value = false
  }
}

function openAddRuleModal() {
  let targetDeptId = filterRuleDeptId.value || departmentList.value[0]?.id || undefined
  
  if (isDeptManager.value && !isSuperAdmin.value) {
    const userDeptId = Number(localStorage.getItem('departmentId') || '0')
    if (userDeptId > 0) {
      targetDeptId = userDeptId
    } else if (departmentList.value.length > 0) {
      targetDeptId = departmentList.value[0].id
    }
  }

  ruleForm.value = {
    id: null,
    departmentId: targetDeptId,
    sessionName: '上午场',
    checkInStartTime: '08:50',
    normalCheckInEndTime: '09:10',
    checkInEndTime: '12:10',
    normalCheckOutStartTime: '11:50',
    checkOutEndTime: '12:10',
    enabled: 1
  }
  ruleDialogVisible.value = true
}

function openEditRuleModal(rule) {
  ruleForm.value = { ...rule }
  ruleDialogVisible.value = true
}

async function handleSaveRule() {
  if (!ruleForm.value.departmentId) {
    ElMessage.warning('请选择部门')
    return
  }
  if (!ruleForm.value.sessionName || !ruleForm.value.sessionName.trim()) {
    ElMessage.warning('请输入场次名称 (如 上午场 / 下午场)')
    return
  }
  ruleFormLoading.value = true
  try {
    const res = await saveDepartmentRule(ruleForm.value)
    if (res && res.code === 200) {
      ElMessage.success('部门考勤规则保存成功')
      ruleDialogVisible.value = false
      fetchRules()
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存规则失败')
  } finally {
    ruleFormLoading.value = false
  }
}

async function handleDeleteRule(id) {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定要删除该部门考勤场次规则吗？删除后不再自动生成该场次任务。', '删除提示', { type: 'warning' })
    const res = await deleteDepartmentRule(id)
    if (res && res.code === 200) {
      ElMessage.success('规则删除成功')
      fetchRules()
    }
  } catch {}
}

// ================= Tab 4: 考勤补签多级分流审批 =================
const replenishListLoading = ref(false)
const replenishList = ref([])
const replenishTotal = ref(0)
const replenishPage = ref(1)
const replenishSize = ref(10)
const filterReplenishStatus = ref('PENDING')

const rejectModalVisible = ref(false)
const escalateModalVisible = ref(false)
const currentAuditRecord = ref(null)
const rejectionReason = ref('')
const escalationNote = ref('')
const approveSubmitLoading = ref(false)

const pendingReplenishCount = computed(() => {
  return replenishList.value.filter(r => isPending(r)).length
})

// 多级展示列表计算属性：部门经理可在【本部门员工审批】与【我的个人补签】间智能分流
const displayedReplenishList = computed(() => {
  if (!isDeptManager.value || isSuperAdmin.value) {
    return replenishList.value
  }
  if (deptManagerSubTab.value === 'MY_RECORDS') {
    return replenishList.value.filter(r => r.employeeId === currentEmpId.value)
  } else {
    // 审批本部门员工（排除部门经理本人发起的）
    return replenishList.value.filter(r => r.employeeId !== currentEmpId.value)
  }
})

function isPending(row) {
  return row.replenishStatus === 'PENDING' || row.status === 'PENDING'
}

function isDeptManagerUser(row) {
  return row.employeeName && (row.employeeName.includes('经理') || row.employeeName.includes('主管'))
}

function isEscalated(row) {
  return row.approveComment && row.approveComment.includes('呈报最高管理员')
}

function canAudit(row) {
  if (isSuperAdmin.value) return true
  if (isDeptManager.value) {
    // 部门经理不能审批自己的补签，必须上呈给最高管理员
    return row.employeeId !== currentEmpId.value
  }
  return false
}

async function fetchReplenishRecords() {
  replenishListLoading.value = true
  try {
    const res = await getReplenishRecords({
      page: replenishPage.value,
      size: replenishSize.value,
      replenishStatus: filterReplenishStatus.value || undefined
    })
    if (res && res.data) {
      replenishList.value = res.data.records || []
      replenishTotal.value = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '获取补签列表失败')
  } finally {
    replenishListLoading.value = false
  }
}

// 同意补签
async function handleQuickApprove(record) {
  try {
    await ElMessageBox.confirm(`确定批复同意 ${record.employeeName} 的【${record.workDate} ${record.sessionName}】考勤补签申请吗？`, '同意确认', { type: 'success' })
    approveSubmitLoading.value = true
    const res = await approveReplenishment(record.id, true, '同意补签申请')
    if (res && res.code === 200) {
      ElMessage.success('补签申请已成功同意！')
      fetchReplenishRecords()
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '审批操作失败')
    }
  } finally {
    approveSubmitLoading.value = false
  }
}

// 驳回模态框
function openRejectModal(record) {
  currentAuditRecord.value = record
  rejectionReason.value = ''
  rejectModalVisible.value = true
}

async function submitReject() {
  if (!rejectionReason.value.trim()) {
    ElMessage.warning('驳回理由为强校验必填项，请输入具体说明！')
    return
  }
  approveSubmitLoading.value = true
  try {
    const res = await approveReplenishment(currentAuditRecord.value.id, false, rejectionReason.value.trim())
    if (res && res.code === 200) {
      ElMessage.success('已驳回该补签申请')
      rejectModalVisible.value = false
      fetchReplenishRecords()
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '驳回操作失败')
  } finally {
    approveSubmitLoading.value = false
  }
}

// 上报高管模态框
function openEscalateModal(record) {
  currentAuditRecord.value = record
  escalationNote.value = ''
  escalateModalVisible.value = true
}

async function submitEscalate() {
  approveSubmitLoading.value = true
  try {
    const commentText = `【呈报最高管理员批示】${escalationNote.value.trim() ? escalationNote.value.trim() : '呈报高管进行核验批示'}`
    const res = await approveReplenishment(currentAuditRecord.value.id, false, commentText)
    if (res && res.code === 200) {
      ElMessage.success('已成功呈报给最高管理员批示！')
      escalateModalVisible.value = false
      fetchReplenishRecords()
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '呈报操作失败')
  } finally {
    approveSubmitLoading.value = false
  }
}

function getStatusBadge(status, replenishStatus) {
  if (replenishStatus === 'PENDING') return { type: 'warning', label: '补签待审批' }
  if (replenishStatus === 'APPROVED' || status === 'REPLENISHED') return { type: 'success', label: '已补签' }
  if (replenishStatus === 'REJECTED') return { type: 'danger', label: '补签被驳回' }

  switch (status) {
    case 'CHECKED_IN': return { type: 'success', label: '正常签到' }
    case 'CHECKED_OUT': return { type: 'success', label: '正常签退' }
    case 'LATE': return { type: 'warning', label: '迟到' }
    case 'EARLY_LEAVE': return { type: 'warning', label: '早退' }
    case 'ABSENT': return { type: 'danger', label: '缺勤' }
    default: return { type: 'info', label: '未打卡' }
  }
}

onMounted(() => {
  if (!localStorage.getItem('token')) return
  loadDepartments()
  
  if (!isSuperAdmin.value) {
    loadTodayTasks()
    fetchPersonalRecords()
  }

  if (isDeptManager.value || isSuperAdmin.value) {
    fetchAdminRecords()
    fetchRules()
    fetchReplenishRecords()
  }

  setInterval(() => { 
    timeStr.value = new Date().toLocaleTimeString() 
    dateStr.value = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
  }, 1000)
})
</script>

<style scoped>
.attendance-container {
  max-width: 1400px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 26px;
  font-weight: bold;
  color: #1a1a2e;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: #888;
  margin: 6px 0 0 0;
}

.employee-dashboard {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.clock-card {
  position: relative;
  padding: 24px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.04);
}

.clock-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 12px;
}

.clock-time {
  font-size: 38px;
  font-weight: 800;
  color: #1e293b;
  letter-spacing: -1px;
}

.clock-date {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 4px;
  margin-bottom: 20px;
}

.clock-btns {
  display: flex;
  gap: 12px;
}

.clock-btn {
  flex: 1;
  border-radius: 12px;
}

.tasks-card {
  padding: 24px;
  height: 100%;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.04);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.card-subtitle {
  font-size: 12px;
  color: #94a3b8;
}

.tasks-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(248, 250, 252, 0.8);
  border-radius: 12px;
  border: 1px solid #f1f5f9;
}

.task-session {
  font-weight: 600;
  color: #334155;
  margin-right: 12px;
}

.task-time {
  font-size: 13px;
  color: #64748b;
}

.view-tab-switcher {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  font-size: 15px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab-btn:hover {
  background: rgba(255, 255, 255, 0.95);
  color: #1a1a2e;
}

.tab-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.35);
}

.badge {
  background: #ef4444;
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.glass-card {
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.04);
}

.filter-card {
  padding: 16px 20px;
  background: rgba(248, 250, 252, 0.8);
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  align-items: center;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}

.dept-overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.overview-card {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.03);
}

.attendance-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.overview-title {
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 8px;
}

.overview-stats {
  display: flex;
  gap: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 15px;
  font-weight: 700;
  color: #334155;
}

.stat-value.checked { color: #10b981; }
.stat-value.unchecked { color: #ef4444; }
.stat-value.rate { color: #3b82f6; }

.stat-label {
  font-size: 11px;
  color: #94a3b8;
}

.pagination-bar {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.text-muted {
  color: #94a3b8;
}

.replenish-btn {
  color: white !important;
}
</style>
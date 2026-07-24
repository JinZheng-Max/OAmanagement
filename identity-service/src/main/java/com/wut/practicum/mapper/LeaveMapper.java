package com.wut.practicum.mapper;

import com.wut.practicum.entity.OaLeave;
import com.wut.practicum.entity.OaLeaveAttachment;
import com.wut.practicum.entity.OaLeaveAudit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface LeaveMapper {
    // ---- 请假申请 ----
    List<OaLeave> selectPage(@Param("applicantId") Long applicantId,
                             @Param("status") String status,
                             @Param("type") String type,
                             @Param("offset") int offset,
                             @Param("limit") int limit,
                             @Param("deptId") Long deptId);
    long countPage(@Param("applicantId") Long applicantId,
                   @Param("status") String status,
                   @Param("type") String type,
                   @Param("deptId") Long deptId);
    OaLeave selectById(@Param("id") Long id);
    int insert(OaLeave leave);
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    // ---- 审批记录 ----
    int insertAudit(OaLeaveAudit audit);
    List<OaLeaveAudit> selectAuditsByLeaveId(@Param("leaveId") Long leaveId);

    // ---- 统计大屏 ----
    List<java.util.Map<String, Object>> selectDeptPendingLeaveCounts();
    List<java.util.Map<String, Object>> selectDeptPendingReplenishCounts();
    long countPendingLeaves();
    long countPendingReplenishes();

    // ---- 附件 ----
    int insertAttachment(OaLeaveAttachment attachment);
    List<OaLeaveAttachment> selectAttachmentsByLeaveId(@Param("leaveId") Long leaveId);
    OaLeaveAttachment selectAttachmentById(@Param("id") Long id);
}

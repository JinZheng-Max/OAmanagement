package com.wut.practicum.dto;

import com.wut.practicum.entity.OaLeave;
import com.wut.practicum.entity.OaLeaveAttachment;
import com.wut.practicum.entity.OaLeaveAudit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "请假申请信息")
public record LeaveResponse(
        Long id, Long applicantId, String applicantName, String applicantNo,
        String type, LocalDateTime startTime, LocalDateTime endTime,
        String reason, String status, LocalDateTime createTime, LocalDateTime updateTime,
        List<AuditRecord> audits,
        List<AttachmentRecord> attachments) {

    public static LeaveResponse from(OaLeave leave, List<OaLeaveAudit> audits, List<OaLeaveAttachment> attachments) {
        List<AuditRecord> auditRecords = audits == null ? List.of() :
                audits.stream().map(a -> new AuditRecord(a.getId(), a.getLeaveId(),
                        a.getAuditorId(), a.getAuditorName(), a.getAction(), a.getComment(), a.getAuditTime())).toList();
        List<AttachmentRecord> attachmentRecords = attachments == null ? List.of() :
                attachments.stream().map(att -> new AttachmentRecord(att.getId(), att.getLeaveId(),
                        att.getFileName(), att.getMimeType(), att.getFileSize(), att.getCreateTime())).toList();
        return new LeaveResponse(leave.getId(), leave.getApplicantId(),
                leave.getApplicantName(), leave.getApplicantNo(),
                leave.getType(), leave.getStartTime(), leave.getEndTime(),
                leave.getReason(), leave.getStatus(), leave.getCreateTime(), leave.getUpdateTime(),
                auditRecords, attachmentRecords);
    }

    public static LeaveResponse from(OaLeave leave, List<OaLeaveAudit> audits) {
        return from(leave, audits, List.of());
    }

    public record AuditRecord(Long id, Long leaveId, Long auditorId, String auditorName,
                               String action, String comment, LocalDateTime auditTime) {}

    public record AttachmentRecord(Long id, Long leaveId, String fileName, String mimeType,
                                   Long fileSize, LocalDateTime createTime) {}
}

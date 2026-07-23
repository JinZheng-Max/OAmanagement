package com.wut.practicum.service;

import com.wut.practicum.dto.*;

public interface LeaveService {
    PageResult<LeaveResponse> page(PageQuery query, Long currentUserId, String role, String status, String type);
    LeaveResponse getById(Long id);
    LeaveResponse create(Long applicantId, LeaveCreateRequest request);
    void withdraw(Long id, Long applicantId);
    void audit(Long id, Long auditorId, LeaveAuditRequest request);
}

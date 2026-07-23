package com.wut.practicum.controller;

import com.wut.practicum.client.EmployeeClient;
import com.wut.practicum.common.ApiResult;
import com.wut.practicum.common.BusinessException;
import com.wut.practicum.dto.*;
import com.wut.practicum.security.JwtService;
import com.wut.practicum.service.ContentSearchService;
import com.wut.practicum.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "公告制度与全文检索 API")
@RestController
@RequestMapping("/api/contents")
public class ContentController {

    private final ContentService contentService;
    private final ContentSearchService contentSearchService;
    private final EmployeeClient employeeClient;

    public ContentController(ContentService contentService,
                             ContentSearchService contentSearchService,
                             EmployeeClient employeeClient) {
        this.contentService = contentService;
        this.contentSearchService = contentSearchService;
        this.employeeClient = employeeClient;
    }

    // ======================== FR-CNT-01 草稿维护 (管理员) ========================

    @Operation(summary = "创建或修改草稿/内容 (管理员)")
    @PostMapping("/draft")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPT_MANAGER', 'ADMIN')")
    public ApiResult<ContentDetailVO> saveDraft(@Valid @RequestBody ContentSaveDTO dto) {
        JwtService.UserPrincipal user = getCurrentUser();
        // 部门管理员权限限制：只能发本部门，强制设为 DEPARTMENT 范围并绑定本部门ID
        if (isDeptManagerOnly(user.role())) {
            Long deptId = getDepartmentId(user);
            if (deptId == null) {
                throw new BusinessException(400, HttpStatus.BAD_REQUEST, "无法获取您的所属部门，部门管理员禁止发布全公司/跨部门公告");
            }
            dto.setScope("DEPARTMENT");
            dto.setAccessDepartmentId(deptId);
        }
        ContentDetailVO vo = contentService.saveDraft(dto, user.userId(), user.role());
        return ApiResult.success("草稿保存成功", vo);
    }

    @Operation(summary = "删除草稿 (管理员)")
    @DeleteMapping("/draft/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPT_MANAGER', 'ADMIN')")
    public ApiResult<Void> deleteDraft(@PathVariable("id") Long id) {
        JwtService.UserPrincipal user = getCurrentUser();
        contentService.deleteDraft(id, user.userId(), user.role());
        return ApiResult.success("草稿删除成功", null);
    }

    // ======================== FR-CNT-02 发布与下架 (管理员) ========================

    @Operation(summary = "发布公告制度 (管理员)")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPT_MANAGER', 'ADMIN')")
    public ApiResult<ContentDetailVO> publish(@PathVariable("id") Long id) {
        JwtService.UserPrincipal user = getCurrentUser();
        ContentDetailVO vo = contentService.publish(id, user.userId(), user.role());
        return ApiResult.success("发布成功，已同步 ES 索引与缓存", vo);
    }

    @Operation(summary = "下架公告制度 (管理员)")
    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPT_MANAGER', 'ADMIN')")
    public ApiResult<Void> unpublish(@PathVariable("id") Long id) {
        JwtService.UserPrincipal user = getCurrentUser();
        contentService.unpublish(id, user.userId(), user.role());
        return ApiResult.success("下架成功，已从 ES 索引移除", null);
    }

    @Operation(summary = "管理员分页查询列表 (包含草稿/发布/下架)")
    @GetMapping("/admin/page")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPT_MANAGER', 'ADMIN')")
    public ApiResult<PageResult<ContentDetailVO>> adminList(ContentQueryDTO query) {
        JwtService.UserPrincipal user = getCurrentUser();
        PageResult<ContentDetailVO> page = contentService.adminList(query, user.userId(), user.role());
        return ApiResult.success(page);
    }

    // ======================== FR-CNT-04 ES 索引重建 (管理员) ========================

    @Operation(summary = "手动触发 ES 索引全量重建 (管理员)")
    @PostMapping("/admin/reindex")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPT_MANAGER', 'ADMIN')")
    public ApiResult<EsReindexResultVO> reindexAll() {
        EsReindexResultVO result = contentSearchService.reindexAll();
        return ApiResult.success(result.getMessage(), result);
    }

    // ======================== FR-CNT-03 员工浏览与详情 ========================

    @Operation(summary = "员工浏览已发布公告制度列表")
    @GetMapping("/page")
    public ApiResult<PageResult<ContentDetailVO>> employeeList(ContentQueryDTO query) {
        JwtService.UserPrincipal user = getCurrentUser();
        Long deptId = getDepartmentId(user);
        PageResult<ContentDetailVO> page = contentService.employeeList(query, user.employeeId(), deptId);
        return ApiResult.success(page);
    }

    @Operation(summary = "查看公告制度详情 (支持 Redis 缓存与热点浏览量统计)")
    @GetMapping("/{id}")
    public ApiResult<ContentDetailVO> getDetail(@PathVariable("id") Long id) {
        JwtService.UserPrincipal user = getCurrentUser();
        boolean isSuperAdmin = !isDeptManagerOnly(user.role()) && isAdminRole(user.role());
        Long deptId = getDepartmentId(user);
        ContentDetailVO vo = contentService.getDetail(id, user.employeeId(), deptId, isSuperAdmin);
        return ApiResult.success(vo);
    }

    // ======================== FR-SCH-01 ~ FR-SCH-03 全文检索 ========================

    @Operation(summary = "关键词全文检索 (多字段加权搜索、高亮、权限过滤)")
    @GetMapping("/search")
    public ApiResult<PageResult<ContentDetailVO>> search(ContentSearchDTO searchDTO) {
        JwtService.UserPrincipal user = getCurrentUser();
        Long deptId = getDepartmentId(user);
        PageResult<ContentDetailVO> result = contentSearchService.search(searchDTO, user.userId(), user.role(), deptId);
        return ApiResult.success(result);
    }

    // ======================== 私有辅助方法 ========================

    private JwtService.UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtService.UserPrincipal principal) {
            return principal;
        }
        return new JwtService.UserPrincipal(0L, "anonymous", "EMPLOYEE", null, "");
    }

    private Long getDepartmentId(JwtService.UserPrincipal user) {
        if (user.employeeId() == null) return null;
        try {
            ApiResult<EmployeeResponse> res = employeeClient.getById(user.employeeId());
            if (res != null && res.getData() != null) {
                return res.getData().getDepartmentId();
            }
        } catch (Exception e) {
            log.warn("调用 Feign 查询员工部门失败，employeeId={}: {}", user.employeeId(), e.getMessage());
        }
        return null;
    }

    private boolean isAdminRole(String role) {
        if (role == null) return false;
        String r = role.toUpperCase();
        return r.contains("SUPER_ADMIN") || r.contains("DEPT_MANAGER") || r.contains("ADMIN");
    }

    private boolean isDeptManagerOnly(String role) {
        if (role == null) return false;
        String r = role.toUpperCase();
        if (r.contains("SUPER_ADMIN") || r.contains("ADMIN") || r.contains("ROLE_ADMIN")) {
            return false;
        }
        return r.contains("DEPT_MANAGER") || r.contains("MANAGER");
    }
}

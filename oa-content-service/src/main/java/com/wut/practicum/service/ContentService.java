package com.wut.practicum.service;

import com.wut.practicum.dto.*;

public interface ContentService {

    /**
     * 管理员新建或编辑草稿
     */
    ContentDetailVO saveDraft(ContentSaveDTO dto, Long operatorId);

    /**
     * 发布内容 (管理员操作)
     */
    ContentDetailVO publish(Long id, Long operatorId);

    /**
     * 下架内容 (管理员操作)
     */
    void unpublish(Long id, Long operatorId);

    /**
     * 删除草稿 (管理员操作)
     */
    void deleteDraft(Long id, Long operatorId);

    /**
     * 管理员分页查询 (支持草稿/发布/下架)
     */
    PageResult<ContentDetailVO> adminList(ContentQueryDTO query);

    /**
     * 员工分页浏览已发布内容 (Redis 缓存)
     */
    PageResult<ContentDetailVO> employeeList(ContentQueryDTO query, Long employeeId, Long deptId);

    /**
     * 员工查看详情 (Redis 缓存 + view_count 计数)
     */
    ContentDetailVO getDetail(Long id, Long employeeId, Long deptId, boolean isAdmin);
}

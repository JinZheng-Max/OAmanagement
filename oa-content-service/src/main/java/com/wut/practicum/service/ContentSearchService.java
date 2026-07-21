package com.wut.practicum.service;

import com.wut.practicum.dto.ContentDetailVO;
import com.wut.practicum.dto.ContentSearchDTO;
import com.wut.practicum.dto.EsReindexResultVO;
import com.wut.practicum.dto.PageResult;
import com.wut.practicum.entity.ContentEntity;

public interface ContentSearchService {

    /**
     * 单条记录增量写入/更新 ES
     */
    void indexContent(ContentEntity content);

    /**
     * 单条记录删除 ES 文档
     */
    void deleteContentIndex(Long id);

    /**
     * 全量重建 ES 索引
     */
    EsReindexResultVO reindexAll();

    /**
     * 全文检索（含关键词加权、多条件筛选、高亮、权限控制及 ES 故障降级）
     */
    PageResult<ContentDetailVO> search(ContentSearchDTO searchDTO, Long currentUserId, String currentRole, Long userDeptId);
}

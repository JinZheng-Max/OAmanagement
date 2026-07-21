package com.wut.practicum.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wut.practicum.common.BusinessException;
import com.wut.practicum.dto.*;
import com.wut.practicum.entity.ContentEntity;
import com.wut.practicum.mapper.ContentMapper;
import com.wut.practicum.service.ContentSearchService;
import com.wut.practicum.service.ContentService;
import com.wut.practicum.util.XssUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ContentServiceImpl implements ContentService {

    // Redis 键前缀
    private static final String KEY_CONTENT_DETAIL = "oa:content:detail:";
    private static final String KEY_VIEW_COUNT_DELTA = "oa:content:vc:delta:";
    // 热点公告缓存阈值：访问量超过 200 时写入短期缓存
    private static final long HOT_VIEW_THRESHOLD = 200L;
    // 详情缓存 TTL：1小时；热点缓存 TTL：10分钟
    private static final Duration DETAIL_TTL = Duration.ofHours(1);
    private static final Duration HOT_TTL = Duration.ofMinutes(10);

    private final ContentMapper contentMapper;
    private final ContentSearchService contentSearchService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ContentServiceImpl(ContentMapper contentMapper,
                               ContentSearchService contentSearchService,
                               StringRedisTemplate redisTemplate) {
        this.contentMapper = contentMapper;
        this.contentSearchService = contentSearchService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // ======================== 管理员操作 ========================

    @Override
    @Transactional
    public ContentDetailVO saveDraft(ContentSaveDTO dto, Long operatorId) {
        // 1. XSS / 脚本校验
        if (XssUtils.containsScript(dto.getBody())) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "正文中包含可执行脚本，保存被拒绝");
        }
        if (XssUtils.containsScript(dto.getTitle())) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "标题中包含非法内容，保存被拒绝");
        }

        // 2. 标题重复提示（不阻断）
        ContentEntity existing = contentMapper.selectByTitle(dto.getTitle());
        boolean titleDuplicated = (existing != null && (dto.getId() == null || !existing.getId().equals(dto.getId())));

        ContentEntity entity;
        if (dto.getId() == null) {
            // 新建
            entity = new ContentEntity();
            entity.setType(dto.getType());
            entity.setStatus("DRAFT");
            entity.setVersion(1);
            entity.setViewCount(0L);
            entity.setPublisherId(operatorId);
            fillFromSaveDTO(entity, dto);
            contentMapper.insert(entity);
        } else {
            // 编辑已有
            entity = contentMapper.selectById(dto.getId());
            if (entity == null) {
                throw new BusinessException(404, HttpStatus.NOT_FOUND, "内容不存在，id=" + dto.getId());
            }
            if ("PUBLISHED".equalsIgnoreCase(entity.getStatus())) {
                // 编辑已发布内容：版本+1，保持发布状态，更新 ES
                entity.setVersion(entity.getVersion() + 1);
            }
            fillFromSaveDTO(entity, dto);
            contentMapper.update(entity);
            // 若原已发布，则同步更新 ES
            if ("PUBLISHED".equalsIgnoreCase(entity.getStatus())) {
                contentSearchService.indexContent(entity);
                evictDetailCache(entity.getId());
            }
        }

        ContentDetailVO vo = convertToVO(entity);
        if (titleDuplicated) {
            vo.setHighlightTitle("[⚠️ 标题与已有内容重复，请确认] " + entity.getTitle());
        }
        return vo;
    }

    @Override
    @Transactional
    public ContentDetailVO publish(Long id, Long operatorId) {
        ContentEntity entity = contentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "内容不存在，id=" + id);
        }
        if ("PUBLISHED".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "内容已经处于发布状态");
        }

        entity.setStatus("PUBLISHED");
        entity.setPublisherId(operatorId);
        entity.setPublishTime(LocalDateTime.now());
        contentMapper.update(entity);

        // 同步 ES 索引 + 清理缓存
        try {
            contentSearchService.indexContent(entity);
        } catch (Exception e) {
            log.error("发布后同步 ES 失败，id={}，错误: {}", id, e.getMessage());
        }
        evictDetailCache(id);

        return convertToVO(entity);
    }

    @Override
    @Transactional
    public void unpublish(Long id, Long operatorId) {
        ContentEntity entity = contentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "内容不存在，id=" + id);
        }

        entity.setStatus("UNPUBLISHED");
        contentMapper.update(entity);

        // 从 ES 删除/标记下架 + 清理缓存
        try {
            contentSearchService.deleteContentIndex(id);
        } catch (Exception e) {
            log.error("下架后从 ES 删除文档失败，id={}，错误: {}", id, e.getMessage());
        }
        evictDetailCache(id);
    }

    @Override
    @Transactional
    public void deleteDraft(Long id, Long operatorId) {
        ContentEntity entity = contentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "内容不存在，id=" + id);
        }
        if (!"DRAFT".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException(400, HttpStatus.BAD_REQUEST, "只允许删除草稿，当前状态: " + entity.getStatus());
        }
        contentMapper.deleteById(id);
    }

    @Override
    public PageResult<ContentDetailVO> adminList(ContentQueryDTO query) {
        int page = Math.max(1, query.getPage() != null ? query.getPage() : 1);
        int size = Math.min(100, Math.max(1, query.getSize() != null ? query.getSize() : 10));
        int offset = (page - 1) * size;

        List<ContentEntity> entities = contentMapper.selectList(query.getType(), query.getCategory(), query.getStatus(), null, true, offset, size);
        long total = contentMapper.countList(query.getType(), query.getCategory(), query.getStatus(), null, true);
        List<ContentDetailVO> voList = entities.stream().map(this::convertToVO).collect(Collectors.toList());
        return new PageResult<>(voList, total, page, size);
    }

    // ======================== 员工操作 ========================

    @Override
    public PageResult<ContentDetailVO> employeeList(ContentQueryDTO query, Long employeeId, Long deptId) {
        int page = Math.max(1, query.getPage() != null ? query.getPage() : 1);
        int size = Math.min(100, Math.max(1, query.getSize() != null ? query.getSize() : 10));
        int offset = (page - 1) * size;

        // 员工只能看 PUBLISHED
        List<ContentEntity> entities = contentMapper.selectList(query.getType(), query.getCategory(), "PUBLISHED", deptId, false, offset, size);
        long total = contentMapper.countList(query.getType(), query.getCategory(), "PUBLISHED", deptId, false);
        List<ContentDetailVO> voList = entities.stream().map(this::convertToVO).collect(Collectors.toList());
        return new PageResult<>(voList, total, page, size);
    }

    @Override
    public ContentDetailVO getDetail(Long id, Long employeeId, Long deptId, boolean isAdmin) {
        // 1. 尝试从 Redis 读取热点缓存
        String cacheKey = KEY_CONTENT_DETAIL + id;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                ContentDetailVO vo = objectMapper.readValue(cached, ContentDetailVO.class);
                // 非管理员做权限二次校验
                if (!isAdmin && !checkPermission(vo, deptId)) {
                    throw new BusinessException(403, HttpStatus.FORBIDDEN, "您无权查看该内容");
                }
                // 异步增量计数（Redis 累积，定期刷回 MySQL）
                incrementViewCountAsync(id);
                return vo;
            } catch (JsonProcessingException e) {
                log.warn("Redis 缓存解析失败，回源 MySQL, id={}", id);
            }
        }

        // 2. 回源 MySQL
        ContentEntity entity = contentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, HttpStatus.NOT_FOUND, "内容不存在，id=" + id);
        }
        if (!isAdmin && !"PUBLISHED".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException(403, HttpStatus.FORBIDDEN, "内容未发布");
        }
        if (!isAdmin && !checkEntityPermission(entity, deptId)) {
            throw new BusinessException(403, HttpStatus.FORBIDDEN, "您无权查看该内容");
        }

        // 3. 计数 + 缓存写入
        incrementViewCountAsync(id);
        long currentViewCount = entity.getViewCount() != null ? entity.getViewCount() : 0L;
        ContentDetailVO vo = convertToVO(entity);

        // 4. 热点内容写入短期缓存，普通内容写入较长缓存
        Duration ttl = currentViewCount >= HOT_VIEW_THRESHOLD ? HOT_TTL : DETAIL_TTL;
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(vo), ttl);
        } catch (JsonProcessingException e) {
            log.error("写入 Redis 缓存失败，id={}: {}", id, e.getMessage());
        }

        return vo;
    }

    // ======================== 私有辅助方法 ========================

    private void fillFromSaveDTO(ContentEntity entity, ContentSaveDTO dto) {
        if (dto.getType() != null) entity.setType(dto.getType());
        if (dto.getTitle() != null) entity.setTitle(dto.getTitle());
        if (dto.getCategory() != null) entity.setCategory(dto.getCategory());
        if (dto.getBody() != null) entity.setBody(dto.getBody());
        if (dto.getScope() != null) entity.setScope(dto.getScope());
        if (dto.getAccessDepartmentId() != null) entity.setAccessDepartmentId(dto.getAccessDepartmentId());
    }

    private void evictDetailCache(Long id) {
        redisTemplate.delete(KEY_CONTENT_DETAIL + id);
    }

    /**
     * 将 view_count 增量累积到 Redis，定期由 ViewCountFlushTask 刷回 MySQL
     */
    private void incrementViewCountAsync(Long id) {
        try {
            redisTemplate.opsForValue().increment(KEY_VIEW_COUNT_DELTA + id);
        } catch (Exception e) {
            log.warn("Redis 访问量计数失败，id={}", id);
        }
    }

    private boolean checkPermission(ContentDetailVO vo, Long deptId) {
        if (!"PUBLISHED".equalsIgnoreCase(vo.getStatus())) return false;
        if ("ALL".equalsIgnoreCase(vo.getScope())) return true;
        return vo.getAccessDepartmentId() != null && vo.getAccessDepartmentId().equals(deptId);
    }

    private boolean checkEntityPermission(ContentEntity entity, Long deptId) {
        if ("ALL".equalsIgnoreCase(entity.getScope())) return true;
        return entity.getAccessDepartmentId() != null && entity.getAccessDepartmentId().equals(deptId);
    }

    private ContentDetailVO convertToVO(ContentEntity entity) {
        ContentDetailVO vo = new ContentDetailVO();
        vo.setId(entity.getId());
        vo.setType(entity.getType());
        vo.setTitle(entity.getTitle());
        vo.setCategory(entity.getCategory());
        vo.setBody(entity.getBody());
        vo.setStatus(entity.getStatus());
        vo.setScope(entity.getScope());
        vo.setAccessDepartmentId(entity.getAccessDepartmentId());
        vo.setPublisherId(entity.getPublisherId());
        vo.setPublishTime(entity.getPublishTime());
        vo.setVersion(entity.getVersion());
        vo.setViewCount(entity.getViewCount());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}

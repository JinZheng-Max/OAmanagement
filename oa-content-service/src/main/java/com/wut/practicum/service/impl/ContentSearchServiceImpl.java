package com.wut.practicum.service.impl;

import com.wut.practicum.doc.ContentEsDoc;
import com.wut.practicum.dto.ContentDetailVO;
import com.wut.practicum.dto.ContentSearchDTO;
import com.wut.practicum.dto.EsReindexResultVO;
import com.wut.practicum.dto.PageResult;
import com.wut.practicum.entity.ContentEntity;
import com.wut.practicum.mapper.ContentMapper;
import com.wut.practicum.service.ContentSearchService;
import com.wut.practicum.util.XssUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Service
public class ContentSearchServiceImpl implements ContentSearchService {

    private static final String INDEX_NAME = "oa_content";
    private static final String REINDEX_LOCK_KEY = "oa:lock:es_reindex";

    @Autowired(required = false)
    private ElasticsearchOperations elasticsearchOperations;
    private final ContentMapper contentMapper;
    private final StringRedisTemplate redisTemplate;

    public ContentSearchServiceImpl(ContentMapper contentMapper,
                                    StringRedisTemplate redisTemplate) {
        this.contentMapper = contentMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void indexContent(ContentEntity content) {
        if (elasticsearchOperations == null || content == null || !"PUBLISHED".equalsIgnoreCase(content.getStatus())) {
            return;
        }
        try {
            ContentEsDoc doc = buildEsDoc(content);
            elasticsearchOperations.save(doc);
            log.info("成功同步 ES 索引文档, id: {}, title: {}", content.getId(), content.getTitle());
        } catch (Throwable e) {
            log.error("同步文档至 ES 失败(已优雅降级), id: {}, 错误信息: {}", content.getId(), e.getMessage());
        }
    }

    @Override
    public void deleteContentIndex(Long id) {
        if (elasticsearchOperations == null || id == null) return;
        try {
            elasticsearchOperations.delete(String.valueOf(id), IndexCoordinates.of(INDEX_NAME));
            log.info("成功从 ES 删除索引文档, id: {}", id);
        } catch (Throwable e) {
            log.error("从 ES 删除文档失败(已优雅降级), id: {}, 错误信息: {}", id, e.getMessage());
        }
    }

    @Override
    public EsReindexResultVO reindexAll() {
        if (elasticsearchOperations == null) {
            return new EsReindexResultVO(0, 0, 0, 0, "当前未启用或未配置 Elasticsearch，跳过全量重建");
        }
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(REINDEX_LOCK_KEY, "1", Duration.ofMinutes(5));
        if (Boolean.FALSE.equals(acquired)) {
            return new EsReindexResultVO(0, 0, 0, 0, "后台全量重建正在进行中，请勿重复操作");
        }

        long start = System.currentTimeMillis();
        long success = 0;
        long failure = 0;
        List<ContentEntity> publishedList = Collections.emptyList();

        try {
            publishedList = contentMapper.selectAllPublished();
            for (ContentEntity content : publishedList) {
                try {
                    ContentEsDoc doc = buildEsDoc(content);
                    elasticsearchOperations.save(doc);
                    success++;
                } catch (Throwable ex) {
                    failure++;
                    log.error("重建单条文档失败 id={}: {}", content.getId(), ex.getMessage());
                }
            }
        } catch (Throwable e) {
            log.error("全量重建过程发生异常: {}", e.getMessage(), e);
            return new EsReindexResultVO(publishedList.size(), success, failure, System.currentTimeMillis() - start, "重建过程发生异常: " + e.getMessage());
        } finally {
            redisTemplate.delete(REINDEX_LOCK_KEY);
        }

        long cost = System.currentTimeMillis() - start;
        return new EsReindexResultVO(publishedList.size(), success, failure, cost, "全量重建成功");
    }

    @Override
    public PageResult<ContentDetailVO> search(ContentSearchDTO searchDTO, Long currentUserId, String currentRole, Long userDeptId) {
        boolean isAdmin = "SUPER_ADMIN".equalsIgnoreCase(currentRole) || "DEPT_MANAGER".equalsIgnoreCase(currentRole) || "ADMIN".equalsIgnoreCase(currentRole) || "ROLE_ADMIN".equalsIgnoreCase(currentRole);
        int page = Math.max(1, searchDTO.getPage() != null ? searchDTO.getPage() : 1);
        int size = Math.min(100, Math.max(1, searchDTO.getSize() != null ? searchDTO.getSize() : 10));
        int offset = (page - 1) * size;

        if (elasticsearchOperations == null) {
            return searchFallbackMysql(searchDTO, isAdmin, userDeptId, page, size, offset);
        }

        try {
            // 尝试走 Elasticsearch 全文检索
            NativeQueryBuilder queryBuilder = new NativeQueryBuilder();

            // 多字段匹配与权重设置: title 权重 3.0, body 权重 1.0
            String kw = searchDTO.getKeyword();
            if (kw != null && !kw.trim().isEmpty()) {
                queryBuilder.withQuery(q -> q.multiMatch(m -> m.query(kw.trim()).fields("title^3.0", "body^1.0")));
            }

            // 过滤条件与权限隔离
            queryBuilder.withFilter(f -> f.bool(b -> {
                b.must(m -> m.term(t -> t.field("status").value("PUBLISHED")));

                if (searchDTO.getType() != null && !searchDTO.getType().isEmpty()) {
                    b.must(m -> m.term(t -> t.field("type").value(searchDTO.getType())));
                }
                if (searchDTO.getCategory() != null && !searchDTO.getCategory().isEmpty()) {
                    b.must(m -> m.term(t -> t.field("category").value(searchDTO.getCategory())));
                }

                // 权限过滤: 非管理员只能看 ALL 范围或本人部门资料
                if (!isAdmin) {
                    b.must(sb -> sb.bool(sbb -> {
                        sbb.should(s -> s.term(t -> t.field("scope").value("ALL")));
                        if (userDeptId != null) {
                            sbb.should(s -> s.term(t -> t.field("accessDepartmentId").value(userDeptId)));
                        }
                        return sbb;
                    }));
                }

                return b;
            }));

            // 分页与排序 (相关度得分优先，再按发布时间降序)
            queryBuilder.withPageable(PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "_score", "publishTime")));

            // 关键词高亮
            Highlight highlight = new Highlight(List.of(
                    new HighlightField("title"),
                    new HighlightField("body")
            ));
            queryBuilder.withHighlightQuery(new HighlightQuery(highlight, ContentEsDoc.class));

            NativeQuery nativeQuery = queryBuilder.build();
            SearchHits<ContentEsDoc> searchHits = elasticsearchOperations.search(nativeQuery, ContentEsDoc.class);

            List<ContentDetailVO> voList = new ArrayList<>();
            for (SearchHit<ContentEsDoc> hit : searchHits.getSearchHits()) {
                ContentEsDoc doc = hit.getContent();
                ContentDetailVO vo = convertEsDocToVO(doc);

                // 提取高亮
                Map<String, List<String>> highlightFields = hit.getHighlightFields();
                if (highlightFields.containsKey("title") && !highlightFields.get("title").isEmpty()) {
                    vo.setHighlightTitle(XssUtils.sanitizeHighlight(highlightFields.get("title").get(0)));
                } else {
                    vo.setHighlightTitle(XssUtils.escapeHtml(vo.getTitle()));
                }

                if (highlightFields.containsKey("body") && !highlightFields.get("body").isEmpty()) {
                    vo.setHighlightBody(XssUtils.sanitizeHighlight(highlightFields.get("body").get(0)));
                } else {
                    String plainBody = vo.getBody() != null ? vo.getBody() : "";
                    String snippet = plainBody.length() > 100 ? plainBody.substring(0, 100) + "..." : plainBody;
                    vo.setHighlightBody(XssUtils.escapeHtml(snippet));
                }

                voList.add(vo);
            }

            return new PageResult<>(voList, searchHits.getTotalHits(), page, size);

        } catch (Throwable e) {
            log.warn("ES 检索异常或服务不可用，触发平滑降级至 MySQL 全文/模糊匹配: {}", e.getMessage());
            // 降级方案：回源 MySQL 分页模糊匹配
            return searchFallbackMysql(searchDTO, isAdmin, userDeptId, page, size, offset);
        }
    }

    private PageResult<ContentDetailVO> searchFallbackMysql(ContentSearchDTO searchDTO, boolean isAdmin, Long userDeptId, int page, int size, int offset) {
        List<ContentEntity> entities = contentMapper.selectList(searchDTO.getType(), searchDTO.getCategory(), "PUBLISHED", userDeptId, isAdmin, offset, size);
        long total = contentMapper.countList(searchDTO.getType(), searchDTO.getCategory(), "PUBLISHED", userDeptId, isAdmin);

        String kw = searchDTO.getKeyword();
        List<ContentDetailVO> voList = entities.stream().map(entity -> {
            ContentDetailVO vo = convertEntityToVO(entity);
            if (kw != null && !kw.isEmpty()) {
                String safeKw = XssUtils.escapeHtml(kw);
                if (vo.getTitle() != null && vo.getTitle().contains(kw)) {
                    vo.setHighlightTitle(XssUtils.escapeHtml(vo.getTitle()).replace(safeKw, "<em class=\"highlight\">" + safeKw + "</em>"));
                } else {
                    vo.setHighlightTitle(XssUtils.escapeHtml(vo.getTitle()));
                }

                if (vo.getBody() != null && vo.getBody().contains(kw)) {
                    vo.setHighlightBody(XssUtils.escapeHtml(vo.getBody()).replace(safeKw, "<em class=\"highlight\">" + safeKw + "</em>"));
                } else {
                    String plainBody = vo.getBody() != null ? vo.getBody() : "";
                    String snippet = plainBody.length() > 100 ? plainBody.substring(0, 100) + "..." : plainBody;
                    vo.setHighlightBody(XssUtils.escapeHtml(snippet));
                }
            } else {
                vo.setHighlightTitle(XssUtils.escapeHtml(vo.getTitle()));
                String plainBody = vo.getBody() != null ? vo.getBody() : "";
                String snippet = plainBody.length() > 100 ? plainBody.substring(0, 100) + "..." : plainBody;
                vo.setHighlightBody(XssUtils.escapeHtml(snippet));
            }
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, total, page, size);
    }

    private ContentEsDoc buildEsDoc(ContentEntity content) {
        long pubTime = content.getPublishTime() != null ? content.getPublishTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli() : System.currentTimeMillis();
        long updTime = content.getUpdateTime() != null ? content.getUpdateTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli() : System.currentTimeMillis();
        return ContentEsDoc.builder()
                .id(content.getId())
                .type(content.getType())
                .title(content.getTitle())
                .category(content.getCategory())
                .body(content.getBody())
                .status(content.getStatus())
                .scope(content.getScope())
                .accessDepartmentId(content.getAccessDepartmentId())
                .publisherId(content.getPublisherId())
                .publishTime(pubTime)
                .viewCount(content.getViewCount() != null ? content.getViewCount() : 0L)
                .updateTime(updTime)
                .build();
    }

    private ContentDetailVO convertEsDocToVO(ContentEsDoc doc) {
        ContentDetailVO vo = new ContentDetailVO();
        vo.setId(doc.getId());
        vo.setType(doc.getType());
        vo.setTitle(doc.getTitle());
        vo.setCategory(doc.getCategory());
        vo.setBody(doc.getBody());
        vo.setStatus(doc.getStatus());
        vo.setScope(doc.getScope());
        vo.setAccessDepartmentId(doc.getAccessDepartmentId());
        vo.setPublisherId(doc.getPublisherId());
        vo.setViewCount(doc.getViewCount());
        return vo;
    }

    private ContentDetailVO convertEntityToVO(ContentEntity entity) {
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

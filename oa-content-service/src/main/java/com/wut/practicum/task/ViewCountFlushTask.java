package com.wut.practicum.task;

import com.wut.practicum.mapper.ContentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 定时将 Redis 中累积的 view_count 增量刷回 MySQL，避免高频直接写库
 */
@Slf4j
@Component
public class ViewCountFlushTask {

    private static final String KEY_VC_DELTA_PATTERN = "oa:content:vc:delta:*";
    private static final String KEY_VC_DELTA_PREFIX = "oa:content:vc:delta:";

    private final StringRedisTemplate redisTemplate;
    private final ContentMapper contentMapper;

    public ViewCountFlushTask(StringRedisTemplate redisTemplate, ContentMapper contentMapper) {
        this.redisTemplate = redisTemplate;
        this.contentMapper = contentMapper;
    }

    /**
     * 每 60 秒批量刷回一次
     */
    @Scheduled(fixedDelay = 60_000)
    public void flushViewCounts() {
        Set<String> keys = redisTemplate.keys(KEY_VC_DELTA_PATTERN);
        if (keys == null || keys.isEmpty()) return;

        int flushed = 0;
        for (String key : keys) {
            try {
                String valueStr = redisTemplate.opsForValue().get(key);
                if (valueStr == null) continue;
                redisTemplate.delete(key);
                long delta = Long.parseLong(valueStr);
                if (delta <= 0) continue;
                String idStr = key.replace(KEY_VC_DELTA_PREFIX, "");
                Long id = Long.parseLong(idStr);
                contentMapper.incrementViewCount(id, delta);
                flushed++;
            } catch (Exception e) {
                log.error("刷回访问量失败，key={}: {}", key, e.getMessage());
            }
        }
        if (flushed > 0) {
            log.info("定时任务 ViewCountFlushTask 成功刷回 {} 条访问量至 MySQL", flushed);
        }
    }
}

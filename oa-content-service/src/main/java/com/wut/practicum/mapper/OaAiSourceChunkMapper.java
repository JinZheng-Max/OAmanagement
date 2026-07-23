package com.wut.practicum.mapper;

import com.wut.practicum.entity.OaAiSourceChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OaAiSourceChunkMapper {
    int insert(OaAiSourceChunk chunk);

    /** 批量插入分片 */
    int insertBatch(@Param("list") List<OaAiSourceChunk> chunks);

    /** 按sourceId查询分片 */
    List<OaAiSourceChunk> selectBySourceId(@Param("sourceId") Long sourceId);

    /** 按sourceId批量更新状态 */
    int updateStatusBySourceId(@Param("sourceId") Long sourceId, @Param("status") String status);

    /** 按sourceId物理删除旧分片 */
    int deleteBySourceId(@Param("sourceId") Long sourceId);

    /** 按vector_key列表查询 */
    List<OaAiSourceChunk> selectByVectorKeys(@Param("keys") List<String> keys);
}

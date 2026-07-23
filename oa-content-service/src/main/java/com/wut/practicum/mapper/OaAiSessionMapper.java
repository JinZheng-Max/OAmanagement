package com.wut.practicum.mapper;

import com.wut.practicum.entity.OaAiSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OaAiSessionMapper {
    int insert(OaAiSession session);
    OaAiSession selectById(@Param("id") Long id);
    int updateById(OaAiSession session);
}

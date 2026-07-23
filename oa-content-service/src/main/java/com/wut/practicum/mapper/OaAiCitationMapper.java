package com.wut.practicum.mapper;

import com.wut.practicum.entity.OaAiCitation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OaAiCitationMapper {
    int insert(OaAiCitation citation);
    int insertBatch(@Param("list") List<OaAiCitation> citations);
    List<OaAiCitation> selectBySessionId(@Param("sessionId") Long sessionId);
}

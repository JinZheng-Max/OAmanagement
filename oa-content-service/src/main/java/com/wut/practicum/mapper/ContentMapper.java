package com.wut.practicum.mapper;

import com.wut.practicum.entity.ContentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContentMapper {

    int insert(ContentEntity content);

    int update(ContentEntity content);

    int deleteById(@Param("id") Long id);

    ContentEntity selectById(@Param("id") Long id);

    ContentEntity selectByTitle(@Param("title") String title);

    List<ContentEntity> selectList(@Param("type") String type,
                                   @Param("category") String category,
                                   @Param("status") String status,
                                   @Param("deptId") Long deptId,
                                   @Param("isAdmin") boolean isAdmin,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    long countList(@Param("type") String type,
                   @Param("category") String category,
                   @Param("status") String status,
                   @Param("deptId") Long deptId,
                   @Param("isAdmin") boolean isAdmin);

    List<ContentEntity> selectAllPublished();

    int incrementViewCount(@Param("id") Long id, @Param("delta") long delta);
}

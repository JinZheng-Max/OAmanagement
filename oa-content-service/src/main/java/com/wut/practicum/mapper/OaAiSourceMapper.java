package com.wut.practicum.mapper;

import com.wut.practicum.entity.OaAiSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OaAiSourceMapper {
    int insert(OaAiSource source);
    OaAiSource selectById(@Param("id") Long id);
    List<OaAiSource> selectList(OaAiSource params);
    int updateById(OaAiSource source);

    /** 查询有权访问的文档（用于MySQL二次鉴权） */
    List<OaAiSource> selectAuthorizedSources(
            @Param("ids") List<Long> ids,
            @Param("requestRole") String requestRole,
            @Param("requestRoleLevel") Integer requestRoleLevel,
            @Param("requestDepartmentId") Long requestDepartmentId);

    /** 按文件SHA-256查找 */
    OaAiSource selectByFileHash(@Param("fileHash") String fileHash);

    /** 更新文档访问权限（含清除部门ID） */
    int updateAccessFields(@Param("id") Long id,
                           @Param("accessScope") String accessScope,
                           @Param("accessDepartmentId") String accessDepartmentId,
                           @Param("minRoleLevel") Integer minRoleLevel);
}

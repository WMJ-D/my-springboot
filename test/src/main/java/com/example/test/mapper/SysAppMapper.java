package com.example.test.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 子系统表 sys_app：多子系统架构，前端通过请求头 X-App-Id 标识
 */
@Mapper
public interface SysAppMapper {

    long count(@Param("appId") String appId, @Param("appName") String appName, @Param("status") Integer status);

    List<Map<String, Object>> list(@Param("appId") String appId, @Param("appName") String appName,
                                   @Param("status") Integer status, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * 按 APPID 查询子系统（查名称等场景使用）
     */
    @Select("SELECT id, app_id AS appId, app_name AS appName, base_url AS baseUrl, icon, sort_order AS sort, status, remark FROM sys_app WHERE app_id=#{appId} AND deleted=0 LIMIT 1")
    Map<String, Object> findByAppId(@Param("appId") String appId);

    /**
     * 新增子系统，自增主键回写到 params.id
     */
    int insert(Map<String, Object> params);

    @Update("""
            UPDATE sys_app SET app_id=#{appId}, app_name=#{appName}, base_url=#{baseUrl}, icon=#{icon},
                   sort_order=#{sort}, status=#{status}, remark=#{remark}, updated_by=#{updatedBy}
            WHERE id=#{id} AND deleted=0
            """)
    int update(@Param("appId") String appId, @Param("appName") String appName, @Param("baseUrl") String baseUrl,
               @Param("icon") String icon, @Param("sort") int sort, @Param("status") int status,
               @Param("remark") String remark, @Param("updatedBy") long updatedBy, @Param("id") long id);

    int softDeleteByIds(@Param("updatedBy") long updatedBy, @Param("ids") List<Long> ids);
}

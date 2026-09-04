package com.example.test.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 组织管理，对应 Express 侧 routes/system.js 的组织相关 SQL
 */
@Mapper
public interface SysOrgMapper {

    List<Map<String, Object>> list(@Param("orgName") String orgName);

    @Select("SELECT ancestors FROM sys_org WHERE id=#{id} AND deleted=0")
    String findAncestors(@Param("id") long id);

    /**
     * 新增组织，自增主键回写到 params.id
     */
    int insert(Map<String, Object> params);

    @Update("""
            UPDATE sys_org SET parent_id=#{parentId}, ancestors=#{ancestors}, org_name=#{orgName}, org_code=#{orgCode},
                   leader=#{leader}, phone=#{phone}, email=#{email}, sort_order=#{sort}, status=#{status}, updated_by=#{updatedBy}
            WHERE id=#{id} AND deleted=0
            """)
    int update(@Param("parentId") Long parentId, @Param("ancestors") String ancestors, @Param("orgName") String orgName,
               @Param("orgCode") String orgCode, @Param("leader") String leader, @Param("phone") String phone,
               @Param("email") String email, @Param("sort") int sort, @Param("status") int status,
               @Param("updatedBy") long updatedBy, @Param("id") long id);

    @Select("SELECT id FROM sys_org WHERE parent_id=#{id} AND deleted=0 LIMIT 1")
    Long findFirstChild(@Param("id") long id);

    @Update("UPDATE sys_org SET deleted=1, updated_by=#{updatedBy} WHERE id=#{id}")
    int softDelete(@Param("updatedBy") long updatedBy, @Param("id") long id);
}

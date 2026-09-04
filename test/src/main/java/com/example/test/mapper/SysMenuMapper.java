package com.example.test.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 菜单管理，对应 Express 侧 routes/system.js 的菜单相关 SQL
 */
@Mapper
public interface SysMenuMapper {

    List<Map<String, Object>> list(@Param("menuName") String menuName, @Param("status") Integer status);

    /**
     * 新增菜单，自增主键回写到 params.id
     */
    int insert(Map<String, Object> params);

    @Update("""
            UPDATE sys_menu SET parent_id=#{parentId}, menu_name=#{menuName}, menu_type=#{menuType}, path=#{path},
                   component=#{component}, route_name=#{routeName}, permission=#{permission}, icon=#{icon},
                   sort_order=#{sort}, visible=#{visible}, status=#{status}, keep_alive=#{keepAlive},
                   external_link=#{externalLink}, remark=#{remark}, updated_by=#{updatedBy}
            WHERE id=#{id} AND deleted=0
            """)
    int update(@Param("parentId") Long parentId, @Param("menuName") String menuName, @Param("menuType") String menuType,
               @Param("path") String path, @Param("component") String component, @Param("routeName") String routeName,
               @Param("permission") String permission, @Param("icon") String icon, @Param("sort") int sort,
               @Param("visible") int visible, @Param("status") int status, @Param("keepAlive") int keepAlive,
               @Param("externalLink") int externalLink, @Param("remark") String remark,
               @Param("updatedBy") long updatedBy, @Param("id") long id);

    @Select("SELECT id FROM sys_menu WHERE parent_id=#{id} AND deleted=0 LIMIT 1")
    Long findFirstChild(@Param("id") long id);

    @Update("UPDATE sys_menu SET deleted=1, updated_by=#{updatedBy} WHERE id=#{id}")
    int softDelete(@Param("updatedBy") long updatedBy, @Param("id") long id);
}

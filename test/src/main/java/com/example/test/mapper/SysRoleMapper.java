package com.example.test.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 角色管理，对应 Express 侧 routes/system.js 的角色相关 SQL
 */
@Mapper
public interface SysRoleMapper {

    long count(@Param("roleName") String roleName, @Param("status") Integer status);

    List<Map<String, Object>> list(@Param("roleName") String roleName, @Param("status") Integer status,
                                   @Param("limit") int limit, @Param("offset") int offset);

    /**
     * 新增角色，自增主键回写到 params.id
     */
    int insert(Map<String, Object> params);

    @Update("UPDATE sys_role SET role_name=#{roleName}, role_key=#{roleKey}, data_scope=#{dataScope}, sort_order=#{sort}, status=#{status}, remark=#{remark}, updated_by=#{updatedBy} WHERE id=#{id} AND deleted=0")
    int update(@Param("roleName") String roleName, @Param("roleKey") String roleKey, @Param("dataScope") int dataScope,
               @Param("sort") int sort, @Param("status") int status, @Param("remark") String remark,
               @Param("updatedBy") long updatedBy, @Param("id") long id);

    /**
     * 查询受保护的超管角色（禁止删除）
     */
    List<Long> findAdminRoleIds(@Param("ids") List<Long> ids);

    int softDeleteByIds(@Param("updatedBy") long updatedBy, @Param("ids") List<Long> ids);

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id=#{roleId}")
    List<Long> findMenuIds(@Param("roleId") long roleId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id=#{roleId}")
    int deleteRoleMenus(@Param("roleId") long roleId);

    int insertRoleMenus(@Param("roleId") long roleId, @Param("menuIds") List<Long> menuIds);
}

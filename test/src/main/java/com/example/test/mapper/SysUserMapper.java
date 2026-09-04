package com.example.test.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 用户管理，对应 Express 侧 routes/system.js 的用户相关 SQL
 * <p>insert 使用 Map 参数以支持自增主键回写（keyProperty=id）</p>
 */
@Mapper
public interface SysUserMapper {

    long count(@Param("username") String username, @Param("phone") String phone,
               @Param("status") Integer status, @Param("orgId") Long orgId);

    List<Map<String, Object>> list(@Param("username") String username, @Param("phone") String phone,
                                   @Param("status") Integer status, @Param("orgId") Long orgId,
                                   @Param("limit") int limit, @Param("offset") int offset);

    @Select("""
            SELECT id, username, nickname, phone, email, avatar_url, org_id AS orgId, status,
                   created_at AS createdAt
            FROM sys_user WHERE id=#{id} AND deleted=0
            """)
    Map<String, Object> findById(@Param("id") long id);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id=#{userId}")
    List<Long> findRoleIds(@Param("userId") long userId);

    /**
     * 新增用户，自增主键回写到 params.id
     */
    int insert(Map<String, Object> params);

    int insertUserRoles(@Param("userId") long userId, @Param("roleIds") List<Long> roleIds);

    @Delete("DELETE FROM sys_user_role WHERE user_id=#{userId}")
    int deleteUserRoles(@Param("userId") long userId);

    @Update("UPDATE sys_user SET org_id=#{orgId}, nickname=#{nickname}, phone=#{phone}, email=#{email}, status=#{status}, updated_by=#{updatedBy} WHERE id=#{id} AND deleted=0")
    int update(@Param("orgId") Long orgId, @Param("nickname") String nickname, @Param("phone") String phone,
               @Param("email") String email, @Param("status") int status, @Param("updatedBy") long updatedBy,
               @Param("id") long id);

    @Update("UPDATE sys_user SET status=#{status}, updated_by=#{updatedBy} WHERE id=#{id} AND deleted=0")
    int updateStatus(@Param("status") int status, @Param("updatedBy") long updatedBy, @Param("id") long id);

    @Update("UPDATE sys_user SET password_hash=#{passwordHash}, password_updated_at=CURRENT_TIMESTAMP(3), updated_by=#{updatedBy} WHERE id=#{id} AND deleted=0")
    int resetPassword(@Param("passwordHash") String passwordHash, @Param("updatedBy") long updatedBy, @Param("id") long id);

    int softDeleteByIds(@Param("updatedBy") long updatedBy, @Param("ids") List<Long> ids);
}

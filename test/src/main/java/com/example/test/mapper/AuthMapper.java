package com.example.test.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 认证相关查询，对应 Express 侧 routes/auth.js 与 middleware/auth.js 中的 SQL
 */
@Mapper
public interface AuthMapper {

    /**
     * 登录时按用户名查找用户
     */
    @Select("SELECT id, username, password_hash, nickname, status, deleted FROM sys_user WHERE username=#{username} LIMIT 1")
    Map<String, Object> findLoginUser(@Param("username") String username);

    /**
     * 加载用户身份基础信息
     */
    @Select("""
            SELECT u.id, u.username, u.nickname, u.phone, u.email, u.avatar_url AS avatarUrl,
                   u.status, u.org_id AS orgId, o.org_name AS orgName
            FROM sys_user u LEFT JOIN sys_org o ON o.id=u.org_id AND o.deleted=0
            WHERE u.id=#{userId} AND u.deleted=0
            """)
    Map<String, Object> findIdentityUser(@Param("userId") long userId);

    /**
     * 用户的有效角色
     */
    @Select("""
            SELECT r.id, r.role_name AS roleName, r.role_key AS roleKey
            FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id
            WHERE ur.user_id=#{userId} AND r.status=1 AND r.deleted=0
            ORDER BY r.sort_order, r.id
            """)
    List<Map<String, Object>> findRoles(@Param("userId") long userId);

    /**
     * 用户全部权限标识
     */
    @Select("""
            SELECT DISTINCT m.permission
            FROM sys_user_role ur
            JOIN sys_role r ON r.id=ur.role_id AND r.status=1 AND r.deleted=0
            JOIN sys_role_menu rm ON rm.role_id=r.id
            JOIN sys_menu m ON m.id=rm.menu_id AND m.status=1 AND m.deleted=0
            WHERE ur.user_id=#{userId} AND m.permission IS NOT NULL
            ORDER BY m.permission
            """)
    List<String> findPermissions(@Param("userId") long userId);

    /**
     * 登录成功后更新最后登录信息
     */
    @Update("UPDATE sys_user SET last_login_ip=#{ip}, last_login_at=CURRENT_TIMESTAMP(3) WHERE id=#{userId}")
    int touchLastLogin(@Param("userId") long userId, @Param("ip") String ip);

    /**
     * 管理员可见菜单（全部非按钮菜单）
     */
    @Select("""
            SELECT DISTINCT m.id, m.parent_id AS parentId, m.menu_name AS menuName, m.menu_type AS menuType,
                   m.path, m.component, m.route_name AS routeName, m.permission, m.icon, m.sort_order AS sort,
                   m.visible, m.status, m.keep_alive AS keepAlive, m.external_link AS externalLink,
                   m.remark, m.created_at AS createdAt
            FROM sys_menu m
            WHERE m.deleted=0 AND m.status=1 AND m.visible=1 AND m.menu_type<>'F'
            ORDER BY m.sort_order, m.id
            """)
    List<Map<String, Object>> findAdminMenus();

    /**
     * 普通用户可见菜单（按角色过滤）
     */
    @Select("""
            SELECT DISTINCT m.id, m.parent_id AS parentId, m.menu_name AS menuName, m.menu_type AS menuType,
                   m.path, m.component, m.route_name AS routeName, m.permission, m.icon, m.sort_order AS sort,
                   m.visible, m.status, m.keep_alive AS keepAlive, m.external_link AS externalLink,
                   m.remark, m.created_at AS createdAt
            FROM sys_user_role ur
            JOIN sys_role r ON r.id=ur.role_id AND r.status=1 AND r.deleted=0
            JOIN sys_role_menu rm ON rm.role_id=r.id
            JOIN sys_menu m ON m.id=rm.menu_id
            WHERE ur.user_id=#{userId} AND m.deleted=0 AND m.status=1 AND m.visible=1 AND m.menu_type<>'F'
            ORDER BY m.sort_order, m.id
            """)
    List<Map<String, Object>> findUserMenus(@Param("userId") long userId);
}

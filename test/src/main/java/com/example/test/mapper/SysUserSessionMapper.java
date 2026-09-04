package com.example.test.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户在线会话表 sys_user_session，对应 Express 侧 auth 路由与在线用户管理的 SQL
 */
@Mapper
public interface SysUserSessionMapper {

    /**
     * 认证时查找会话状态
     */
    @Select("SELECT status, expires_at FROM sys_user_session WHERE session_id=#{sessionId} AND user_id=#{userId} LIMIT 1")
    Map<String, Object> findStatusAndExpires(@Param("sessionId") String sessionId, @Param("userId") long userId);

    /**
     * 登录成功后写入会话（记录所属子系统）
     */
    @Insert("""
            INSERT INTO sys_user_session
            (session_id, app_id, user_id, username, ip_address, browser, os, user_agent, expires_at)
            VALUES (#{sessionId}, #{appId}, #{userId}, #{username}, #{ipAddress}, #{browser}, #{os}, #{userAgent}, #{expiresAt})
            """)
    int insertSession(@Param("sessionId") String sessionId, @Param("appId") String appId, @Param("userId") long userId,
                      @Param("username") String username, @Param("ipAddress") String ipAddress,
                      @Param("browser") String browser, @Param("os") String os,
                      @Param("userAgent") String userAgent, @Param("expiresAt") LocalDateTime expiresAt);

    /**
     * 会话过期标记
     */
    @Update("UPDATE sys_user_session SET status='EXPIRED' WHERE session_id=#{sessionId} AND status='ACTIVE'")
    int markExpired(@Param("sessionId") String sessionId);

    /**
     * 心跳：更新最后活跃时间
     */
    @Update("UPDATE sys_user_session SET last_active_at=CURRENT_TIMESTAMP(3) WHERE session_id=#{sessionId} AND status='ACTIVE'")
    int heartbeat(@Param("sessionId") String sessionId);

    /**
     * 登出
     */
    @Update("UPDATE sys_user_session SET status='LOGOUT', logout_at=CURRENT_TIMESTAMP(3) WHERE session_id=#{sessionId} AND status='ACTIVE'")
    int logout(@Param("sessionId") String sessionId);

    /**
     * 强制下线单个会话
     */
    @Update("""
            UPDATE sys_user_session SET status='KICKED', kicked_by=#{kickedBy}, kicked_at=CURRENT_TIMESTAMP(3), kick_reason=#{reason}
            WHERE session_id=#{sessionId} AND status='ACTIVE'
            """)
    int kick(@Param("sessionId") String sessionId, @Param("kickedBy") long kickedBy, @Param("reason") String reason);

    /**
     * 批量强制下线
     */
    int batchKick(@Param("sessionIds") List<String> sessionIds, @Param("kickedBy") long kickedBy, @Param("reason") String reason);

    /**
     * 将过期/闲置的 ACTIVE 会话标记为 EXPIRED
     */
    @Update("""
            UPDATE sys_user_session SET status='EXPIRED'
            WHERE status='ACTIVE' AND (expires_at<=CURRENT_TIMESTAMP(3) OR last_active_at<DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 5 MINUTE))
            """)
    int expireStale();

    /**
     * 清理 30 天前的非活跃会话
     */
    @Delete("""
            DELETE FROM sys_user_session
            WHERE status<>'ACTIVE' AND COALESCE(kicked_at, logout_at, expires_at)<DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 30 DAY)
            """)
    int removeStale();

    /**
     * 在线用户分页统计（支持按所属子系统筛选）
     */
    long countOnline(@Param("appId") String appId, @Param("username") String username, @Param("ipAddress") String ipAddress);

    /**
     * 在线用户分页列表（返回所属系统名称）
     */
    List<Map<String, Object>> listOnline(@Param("appId") String appId, @Param("username") String username,
                                          @Param("ipAddress") String ipAddress,
                                          @Param("pageSize") int pageSize, @Param("offset") int offset);
}

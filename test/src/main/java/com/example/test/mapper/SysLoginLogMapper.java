package com.example.test.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 登录日志表 sys_login_log，对应 Express 侧 routes/auth.js 写日志与 routes/logs.js 查询
 */
@Mapper
public interface SysLoginLogMapper {

    /**
     * 登录日志写入
     */
    @Insert("""
            INSERT INTO sys_login_log (user_id, username, ip_address, browser, os, user_agent, login_type, status, message)
            VALUES (#{userId}, #{username}, #{ipAddress}, #{browser}, #{os}, #{userAgent}, 'PASSWORD', #{status}, #{message})
            """)
    int insertLog(@Param("userId") Long userId, @Param("username") String username, @Param("ipAddress") String ipAddress,
                  @Param("browser") String browser, @Param("os") String os, @Param("userAgent") String userAgent,
                  @Param("status") int status, @Param("message") String message);

    /**
     * 分页统计
     */
    long count(@Param("username") String username, @Param("ipAddress") String ipAddress,
               @Param("status") Integer status, @Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 分页查询
     */
    List<Map<String, Object>> list(@Param("username") String username, @Param("ipAddress") String ipAddress,
                                   @Param("status") Integer status, @Param("startTime") String startTime,
                                   @Param("endTime") String endTime, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * 导出（最多 10000 条）
     */
    List<Map<String, Object>> export(@Param("username") String username, @Param("ipAddress") String ipAddress,
                                     @Param("status") Integer status, @Param("startTime") String startTime,
                                     @Param("endTime") String endTime);

    /**
     * 批量逻辑删除
     */
    int deleteByIds(@Param("ids") List<Long> ids);
}

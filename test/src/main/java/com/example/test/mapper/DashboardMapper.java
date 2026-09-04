package com.example.test.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 仪表盘统计，对应 Express 侧 routes/dashboard.js
 */
@Mapper
public interface DashboardMapper {

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM sys_user WHERE deleted=0) AS user_count,
              (SELECT COUNT(*) FROM sys_role WHERE deleted=0) AS role_count,
              (SELECT COUNT(*) FROM sys_menu WHERE deleted=0 AND menu_type='C') AS menu_count,
              (
                SELECT COUNT(DISTINCT user_id)
                FROM sys_login_log
                WHERE deleted=0 AND status=1 AND user_id IS NOT NULL
                  AND login_at >= CURRENT_DATE() AND login_at < CURRENT_DATE() + INTERVAL 1 DAY
              ) AS today_visit_count
            """)
    Map<String, Object> statistics();
}

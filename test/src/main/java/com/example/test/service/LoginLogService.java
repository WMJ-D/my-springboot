package com.example.test.service;

import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.mapper.SysLoginLogMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录日志服务，对应 Express 侧 routes/logs.js 的登录日志部分
 */
@Service
public class LoginLogService {

    private final SysLoginLogMapper loginLogMapper;

    public LoginLogService(SysLoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    public PageResult<Map<String, Object>> list(PageQuery pageQuery, String username, String ipAddress,
                                                Integer status, String startTime, String endTime) {
        long total = loginLogMapper.count(username, ipAddress, status, startTime, endTime);
        List<Map<String, Object>> rows = loginLogMapper.list(username, ipAddress, status, startTime, endTime,
                pageQuery.getPageSize(), pageQuery.getOffset());
        return new PageResult<>(rows.stream().map(LoginLogService::transform).toList(),
                total, pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public void delete(List<Long> ids) {
        loginLogMapper.deleteByIds(ids);
    }

    public List<Map<String, Object>> export(String username, String ipAddress, Integer status,
                                            String startTime, String endTime) {
        List<Map<String, Object>> rows = loginLogMapper.export(username, ipAddress, status, startTime, endTime);
        return rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>(row);
            item.put("statusText", asInt(row.get("status")) == 1 ? "成功" : "失败");
            item.put("login_at", formatDateTime(row.get("login_at")));
            return item;
        }).toList();
    }

    private static Map<String, Object> transform(Map<String, Object> row) {
        Map<String, Object> item = new LinkedHashMap<>(row);
        item.put("ip", row.get("ipAddress"));
        item.put("status", asInt(row.get("status")) == 1 ? "成功" : "失败");
        item.put("loginTime", row.get("loginAt"));
        return item;
    }

    private static int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }

    /**
     * CSV 中的时间列统一为 yyyy-MM-dd HH:mm:ss（与 Express 侧 dateStrings 输出一致）
     */
    private static String formatDateTime(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof java.time.LocalDateTime time) {
            return time.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return String.valueOf(value);
    }
}

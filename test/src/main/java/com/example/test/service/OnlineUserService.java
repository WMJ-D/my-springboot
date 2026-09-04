package com.example.test.service;

import com.example.test.common.AppException;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.mapper.SysUserSessionMapper;
import com.example.test.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 在线用户服务，对应 Express 侧 routes/logs.js 的在线用户部分
 */
@Service
public class OnlineUserService {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final SysUserSessionMapper sessionMapper;

    public OnlineUserService(SysUserSessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
    }

    public PageResult<Map<String, Object>> list(PageQuery pageQuery, String appId, String username,
                                                String ipAddress, CurrentUser currentUser) {
        long total = sessionMapper.countOnline(appId, username, ipAddress);
        List<Map<String, Object>> rows = sessionMapper.listOnline(appId, username, ipAddress,
                pageQuery.getPageSize(), pageQuery.getOffset());
        List<Map<String, Object>> list = rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>(row);
            item.put("ip", row.get("ipAddress"));
            item.put("isCurrent", currentUser.sessionId().equals(row.get("sessionId")));
            return item;
        }).toList();
        return new PageResult<>(list, total, pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public void kick(String sessionId, String reason, CurrentUser currentUser) {
        if (!UUID_PATTERN.matcher(sessionId).matches()) {
            throw new AppException(400, "请求参数不合法", "VALIDATION_ERROR");
        }
        if (sessionId.equals(currentUser.sessionId())) {
            throw new AppException(400, "不能强制下线当前操作会话", "CANNOT_KICK_SELF");
        }
        int affected = sessionMapper.kick(sessionId, currentUser.userId(),
                reason == null || reason.isBlank() ? "管理员强制下线" : reason.trim());
        if (affected == 0) {
            throw new AppException(404, "在线会话不存在或已离线", "SESSION_NOT_FOUND");
        }
    }

    public Map<String, Object> batchKick(List<String> sessionIds, String reason, CurrentUser currentUser) {
        LinkedHashSet<String> targets = new LinkedHashSet<>();
        for (String sessionId : sessionIds) {
            if (!sessionId.equals(currentUser.sessionId())) {
                targets.add(sessionId);
            }
        }
        if (targets.isEmpty()) {
            throw new AppException(400, "没有可强制下线的会话", "NO_KICKABLE_SESSION");
        }
        int count = sessionMapper.batchKick(List.copyOf(targets), currentUser.userId(),
                reason == null || reason.isBlank() ? "管理员批量强制下线" : reason.trim());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        return result;
    }

    /**
     * 清理过期与闲置会话
     */
    public Map<String, Object> clean() {
        int expired = sessionMapper.expireStale();
        int removed = sessionMapper.removeStale();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expiredCount", expired);
        result.put("removedCount", removed);
        return result;
    }
}

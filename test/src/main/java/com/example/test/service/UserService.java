package com.example.test.service;

import com.example.test.common.AppException;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.mapper.SysUserMapper;
import com.example.test.security.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理服务，对应 Express 侧 routes/system.js 的用户部分
 */
@Service
public class UserService {

    private final SysUserMapper userMapper;

    public UserService(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public PageResult<Map<String, Object>> list(PageQuery pageQuery, String username, String phone,
                                                Integer status, Long orgId) {
        long total = userMapper.count(username, phone, status, orgId);
        List<Map<String, Object>> rows = userMapper.list(username, phone, status, orgId,
                pageQuery.getPageSize(), pageQuery.getOffset());
        List<Map<String, Object>> list = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>(row);
            item.put("roleIds", splitIds(row.get("roleIds")));
            list.add(item);
        }
        return new PageResult<>(list, total, pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public Map<String, Object> detail(long id) {
        Map<String, Object> user = userMapper.findById(id);
        if (user == null) {
            throw new AppException(404, "用户不存在", "NOT_FOUND");
        }
        Map<String, Object> result = new LinkedHashMap<>(user);
        result.put("roleIds", userMapper.findRoleIds(id));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public String create(UserInput input, long actorId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("orgId", input.orgId());
        params.put("username", input.username());
        params.put("passwordHash", PasswordUtil.hash(input.password() == null || input.password().isBlank()
                ? "123456" : input.password()));
        params.put("nickname", input.nickname());
        params.put("phone", input.phone());
        params.put("email", input.email());
        params.put("status", input.status() == null ? 1 : input.status());
        params.put("createdBy", actorId);
        params.put("updatedBy", actorId);
        userMapper.insert(params);
        long id = ((Number) params.get("id")).longValue();
        if (!input.roleIds().isEmpty()) {
            userMapper.insertUserRoles(id, input.roleIds());
        }
        return String.valueOf(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(long id, UserInput input, long actorId) {
        int affected = userMapper.update(input.orgId(), input.nickname(), input.phone(), input.email(),
                input.status() == null ? 1 : input.status(), actorId, id);
        if (affected == 0) {
            throw new AppException(404, "用户不存在", "NOT_FOUND");
        }
        userMapper.deleteUserRoles(id);
        if (!input.roleIds().isEmpty()) {
            userMapper.insertUserRoles(id, input.roleIds());
        }
    }

    public void updateStatus(long id, int status, long actorId) {
        int affected = userMapper.updateStatus(status, actorId, id);
        if (affected == 0) {
            throw new AppException(404, "用户不存在", "NOT_FOUND");
        }
    }

    public void resetPassword(long id, String password, long actorId) {
        String target = password == null || password.isBlank() ? "123456" : password;
        int affected = userMapper.resetPassword(PasswordUtil.hash(target), actorId, id);
        if (affected == 0) {
            throw new AppException(404, "用户不存在", "NOT_FOUND");
        }
    }

    public void delete(String idParam, long currentUserId, long actorId) {
        List<Long> ids = parseIds(idParam);
        if (ids.contains(currentUserId)) {
            throw new AppException(400, "不能删除当前登录用户");
        }
        userMapper.softDeleteByIds(actorId, ids);
    }

    /**
     * 入参对象：新增/修改共用（修改时不使用 password）
     */
    public record UserInput(String username, String nickname, String phone, String email, Long orgId,
                            List<Long> roleIds, String password, Integer status) {
    }

    static List<Long> parseIds(String value) {
        List<Long> ids = new ArrayList<>();
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                long id = Long.parseLong(trimmed);
                if (id > 0 && !ids.contains(id)) {
                    ids.add(id);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (ids.isEmpty()) {
            throw new AppException(400, "请提供有效ID", "INVALID_IDS");
        }
        return ids;
    }

    private static List<Long> splitIds(Object groupConcat) {
        if (groupConcat == null) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : String.valueOf(groupConcat).split(",")) {
            if (!part.isBlank()) {
                ids.add(Long.parseLong(part.trim()));
            }
        }
        return ids;
    }
}

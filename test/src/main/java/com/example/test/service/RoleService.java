package com.example.test.service;

import com.example.test.common.AppException;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.mapper.SysRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色管理服务，对应 Express 侧 routes/system.js 的角色部分
 */
@Service
public class RoleService {

    private final SysRoleMapper roleMapper;

    public RoleService(SysRoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    public PageResult<Map<String, Object>> list(PageQuery pageQuery, String roleName, Integer status) {
        long total = roleMapper.count(roleName, status);
        List<Map<String, Object>> rows = roleMapper.list(roleName, status, pageQuery.getPageSize(), pageQuery.getOffset());
        return new PageResult<>(rows, total, pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public String create(RoleInput input, long actorId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("roleName", input.roleName());
        params.put("roleKey", input.roleKey());
        params.put("dataScope", input.dataScope() == null ? 1 : input.dataScope());
        params.put("sort", input.sort() == null ? 0 : input.sort());
        params.put("status", input.status() == null ? 1 : input.status());
        params.put("remark", input.remark());
        params.put("createdBy", actorId);
        params.put("updatedBy", actorId);
        roleMapper.insert(params);
        return String.valueOf(params.get("id"));
    }

    public void update(long id, RoleInput input, long actorId) {
        int affected = roleMapper.update(input.roleName(), input.roleKey(),
                input.dataScope() == null ? 1 : input.dataScope(),
                input.sort() == null ? 0 : input.sort(),
                input.status() == null ? 1 : input.status(),
                input.remark(), actorId, id);
        if (affected == 0) {
            throw new AppException(404, "角色不存在", "NOT_FOUND");
        }
    }

    public void delete(String idParam, long actorId) {
        List<Long> ids = UserService.parseIds(idParam);
        if (!roleMapper.findAdminRoleIds(ids).isEmpty()) {
            throw new AppException(400, "不能删除超级管理员角色");
        }
        roleMapper.softDeleteByIds(actorId, ids);
    }

    public List<Long> menuIds(long roleId) {
        return roleMapper.findMenuIds(roleId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(long roleId, List<Long> menuIds) {
        roleMapper.deleteRoleMenus(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMapper.insertRoleMenus(roleId, menuIds);
        }
    }

    public record RoleInput(String roleName, String roleKey, Integer dataScope, Integer sort,
                            Integer status, String remark) {
    }
}

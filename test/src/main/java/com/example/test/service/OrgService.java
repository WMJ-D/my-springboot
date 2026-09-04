package com.example.test.service;

import com.example.test.common.AppException;
import com.example.test.common.TreeBuilder;
import com.example.test.mapper.SysOrgMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 组织管理服务，对应 Express 侧 routes/system.js 的组织部分
 */
@Service
public class OrgService {

    private final SysOrgMapper orgMapper;

    public OrgService(SysOrgMapper orgMapper) {
        this.orgMapper = orgMapper;
    }

    public List<Map<String, Object>> list(String orgName) {
        return TreeBuilder.buildTree(orgMapper.list(orgName), "parentId");
    }

    public String create(OrgInput input, long actorId) {
        String ancestors = resolveAncestors(input.parentId());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("parentId", input.parentId());
        params.put("ancestors", ancestors);
        params.put("orgName", input.orgName());
        params.put("orgCode", input.orgCode());
        params.put("leader", input.leader());
        params.put("phone", input.phone());
        params.put("email", input.email());
        params.put("sort", input.sort() == null ? 0 : input.sort());
        params.put("status", input.status() == null ? 1 : input.status());
        params.put("createdBy", actorId);
        params.put("updatedBy", actorId);
        orgMapper.insert(params);
        return String.valueOf(params.get("id"));
    }

    public void update(long id, OrgInput input, long actorId) {
        if (input.parentId() != null && input.parentId() == id) {
            throw new AppException(400, "上级组织不能是自身");
        }
        String ancestors = resolveAncestors(input.parentId());
        int affected = orgMapper.update(input.parentId(), ancestors, input.orgName(), input.orgCode(),
                input.leader(), input.phone(), input.email(),
                input.sort() == null ? 0 : input.sort(),
                input.status() == null ? 1 : input.status(), actorId, id);
        if (affected == 0) {
            throw new AppException(404, "组织不存在", "NOT_FOUND");
        }
    }

    public void delete(long id, long actorId) {
        if (orgMapper.findFirstChild(id) != null) {
            throw new AppException(400, "请先删除下级组织");
        }
        orgMapper.softDelete(actorId, id);
    }

    /**
     * 计算祖级路径：顶级为空串，下级为「父祖级路径,父ID」
     */
    private String resolveAncestors(Long parentId) {
        if (parentId == null) {
            return "";
        }
        String parentAncestors = orgMapper.findAncestors(parentId);
        if (parentAncestors == null) {
            throw new AppException(400, "上级组织不存在");
        }
        return parentAncestors.isEmpty() ? String.valueOf(parentId) : parentAncestors + "," + parentId;
    }

    public record OrgInput(Long parentId, String orgName, String orgCode, String leader, String phone,
                           String email, Integer sort, Integer status) {
    }
}

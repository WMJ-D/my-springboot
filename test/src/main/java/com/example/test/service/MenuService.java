package com.example.test.service;

import com.example.test.common.AppException;
import com.example.test.common.TreeBuilder;
import com.example.test.mapper.SysMenuMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单管理服务，对应 Express 侧 routes/system.js 的菜单部分
 */
@Service
public class MenuService {

    private final SysMenuMapper menuMapper;

    public MenuService(SysMenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    public List<Map<String, Object>> list(String menuName, Integer status) {
        return TreeBuilder.buildTree(menuMapper.list(menuName, status), "parentId");
    }

    public String create(MenuInput input, long actorId) {
        Map<String, Object> params = buildParams(input, actorId);
        menuMapper.insert(params);
        return String.valueOf(params.get("id"));
    }

    public void update(long id, MenuInput input, long actorId) {
        if (input.parentId() != null && input.parentId() == id) {
            throw new AppException(400, "上级菜单不能是自身");
        }
        int affected = menuMapper.update(input.parentId(), input.menuName(), input.menuType(), input.path(),
                input.component(), input.routeName(), input.permission(), input.icon(),
                input.sort() == null ? 0 : input.sort(),
                input.visible() == null ? 1 : input.visible(),
                input.status() == null ? 1 : input.status(),
                input.keepAlive() == null ? 1 : input.keepAlive(),
                input.externalLink() == null ? 0 : input.externalLink(),
                input.remark(), actorId, id);
        if (affected == 0) {
            throw new AppException(404, "菜单不存在", "NOT_FOUND");
        }
    }

    public void delete(long id, long actorId) {
        if (menuMapper.findFirstChild(id) != null) {
            throw new AppException(400, "请先删除子菜单");
        }
        menuMapper.softDelete(actorId, id);
    }

    private Map<String, Object> buildParams(MenuInput input, long actorId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("parentId", input.parentId());
        params.put("menuName", input.menuName());
        params.put("menuType", input.menuType());
        params.put("path", input.path());
        params.put("component", input.component());
        params.put("routeName", input.routeName());
        params.put("permission", input.permission());
        params.put("icon", input.icon());
        params.put("sort", input.sort() == null ? 0 : input.sort());
        params.put("visible", input.visible() == null ? 1 : input.visible());
        params.put("status", input.status() == null ? 1 : input.status());
        params.put("keepAlive", input.keepAlive() == null ? 1 : input.keepAlive());
        params.put("externalLink", input.externalLink() == null ? 0 : input.externalLink());
        params.put("remark", input.remark());
        params.put("createdBy", actorId);
        params.put("updatedBy", actorId);
        return params;
    }

    public record MenuInput(Long parentId, String menuName, String menuType, String path, String component,
                            String routeName, String permission, String icon, Integer sort, Integer visible,
                            Integer status, Integer keepAlive, Integer externalLink, String remark) {
    }
}

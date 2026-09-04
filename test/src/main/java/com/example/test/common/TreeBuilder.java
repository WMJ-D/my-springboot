package com.example.test.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 树构建工具，与 Express 侧 utils/data.js 的 buildTree 保持一致：
 * 按 parentId 组装 children，未匹配到父节点的作为根节点
 */
public final class TreeBuilder {

    private TreeBuilder() {
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> buildTree(List<Map<String, Object>> rows, String parentKey) {
        List<Map<String, Object>> nodes = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Map<String, Object> node = new LinkedHashMap<>(row);
            node.put("children", new ArrayList<Map<String, Object>>());
            nodes.add(node);
        }
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        for (Map<String, Object> node : nodes) {
            map.put(String.valueOf(node.get("id")), node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> node : nodes) {
            Object parentValue = node.get(parentKey);
            Map<String, Object> parent = parentValue == null ? null : map.get(String.valueOf(parentValue));
            if (parent != null) {
                ((List<Map<String, Object>>) parent.get("children")).add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }
}

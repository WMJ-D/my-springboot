package com.example.test.service;

import com.example.test.common.AppException;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.mapper.SysAppMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 子系统管理服务：多子系统架构的子系统 CRUD
 */
@Service
public class SysAppService {

    private final SysAppMapper appMapper;

    public SysAppService(SysAppMapper appMapper) {
        this.appMapper = appMapper;
    }

    public PageResult<Map<String, Object>> list(PageQuery pageQuery, String appId, String appName, Integer status) {
        long total = appMapper.count(appId, appName, status);
        List<Map<String, Object>> rows = appMapper.list(appId, appName, status,
                pageQuery.getPageSize(), pageQuery.getOffset());
        return new PageResult<>(rows, total, pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public String create(AppInput input, long actorId) {
        if (appMapper.findByAppId(input.appId()) != null) {
            throw new AppException(409, "子系统标识已存在", "DUPLICATE_DATA");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("appId", input.appId());
        params.put("appName", input.appName());
        params.put("baseUrl", input.baseUrl());
        params.put("icon", input.icon());
        params.put("sort", input.sort() == null ? 0 : input.sort());
        params.put("status", input.status() == null ? 1 : input.status());
        params.put("remark", input.remark());
        params.put("createdBy", actorId);
        params.put("updatedBy", actorId);
        appMapper.insert(params);
        return String.valueOf(params.get("id"));
    }

    public void update(long id, AppInput input, long actorId) {
        Map<String, Object> existing = appMapper.findByAppId(input.appId());
        if (existing != null && ((Number) existing.get("id")).longValue() != id) {
            throw new AppException(409, "子系统标识已存在", "DUPLICATE_DATA");
        }
        int affected = appMapper.update(input.appId(), input.appName(), input.baseUrl(), input.icon(),
                input.sort() == null ? 0 : input.sort(),
                input.status() == null ? 1 : input.status(),
                input.remark(), actorId, id);
        if (affected == 0) {
            throw new AppException(404, "子系统不存在", "NOT_FOUND");
        }
    }

    public void delete(String idParam, long actorId) {
        List<Long> ids = UserService.parseIds(idParam);
        appMapper.softDeleteByIds(actorId, ids);
    }

    public record AppInput(String appId, String appName, String baseUrl, String icon, Integer sort,
                           Integer status, String remark) {
    }
}

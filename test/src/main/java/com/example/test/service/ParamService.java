package com.example.test.service;

import com.example.test.common.AppException;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.mapper.SysParamMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统参数管理服务，对应 Express 侧 routes/system.js 的参数部分
 */
@Service
public class ParamService {

    private final SysParamMapper paramMapper;

    public ParamService(SysParamMapper paramMapper) {
        this.paramMapper = paramMapper;
    }

    public PageResult<Map<String, Object>> list(PageQuery pageQuery, String paramName, String paramKey, String paramType) {
        long total = paramMapper.count(paramName, paramKey, paramType);
        List<Map<String, Object>> rows = paramMapper.list(paramName, paramKey, paramType,
                pageQuery.getPageSize(), pageQuery.getOffset());
        return new PageResult<>(rows, total, pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public String create(ParamInput input, long actorId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("paramName", input.paramName());
        params.put("paramKey", input.paramKey());
        params.put("paramValue", input.paramValue());
        params.put("paramType", input.paramType() == null ? "N" : input.paramType());
        params.put("valueType", input.valueType() == null ? "string" : input.valueType());
        params.put("remark", input.remark());
        params.put("createdBy", actorId);
        params.put("updatedBy", actorId);
        paramMapper.insert(params);
        return String.valueOf(params.get("id"));
    }

    public void update(long id, ParamInput input, long actorId) {
        int affected = paramMapper.update(input.paramName(), input.paramKey(), input.paramValue(),
                input.paramType() == null ? "N" : input.paramType(),
                input.valueType() == null ? "string" : input.valueType(),
                input.remark(), actorId, id);
        if (affected == 0) {
            throw new AppException(404, "参数不存在", "NOT_FOUND");
        }
    }

    public void delete(String idParam, long actorId) {
        List<Long> ids = UserService.parseIds(idParam);
        paramMapper.softDeleteByIds(actorId, ids);
    }

    public record ParamInput(String paramName, String paramKey, String paramValue, String paramType,
                             String valueType, String remark) {
    }
}

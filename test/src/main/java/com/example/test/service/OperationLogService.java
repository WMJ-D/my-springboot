package com.example.test.service;

import com.example.test.common.JacksonHolder;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.mapper.SysOperationLogMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务，对应 Express 侧 routes/logs.js 的操作日志部分
 */
@Service
public class OperationLogService {

    private static final Map<String, String> DISPLAY_TYPE = Map.of(
            "CREATE", "新增", "UPDATE", "修改", "DELETE", "删除",
            "QUERY", "查询", "EXPORT", "导出", "IMPORT", "导入", "OTHER", "其他");

    private final SysOperationLogMapper operationLogMapper;

    public OperationLogService(SysOperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public PageResult<Map<String, Object>> list(PageQuery pageQuery, String appId, String module, String operator,
                                                String operationType, Integer status, String startTime, String endTime) {
        long total = operationLogMapper.count(appId, module, operator, operationType, status, startTime, endTime);
        List<Map<String, Object>> rows = operationLogMapper.list(appId, module, operator, operationType, status,
                startTime, endTime, pageQuery.getPageSize(), pageQuery.getOffset());
        return new PageResult<>(rows.stream().map(OperationLogService::transform).toList(),
                total, pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public void delete(List<Long> ids) {
        operationLogMapper.deleteByIds(ids);
    }

    public List<Map<String, Object>> export(String appId, String module, String operator, String operationType,
                                            Integer status, String startTime, String endTime) {
        List<Map<String, Object>> rows = operationLogMapper.export(appId, module, operator, operationType,
                status, startTime, endTime);
        return rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>(row);
            item.put("type", displayType(String.valueOf(row.get("operation_type"))));
            item.put("operator", row.get("operator_username"));
            item.put("ip", row.get("ip_address"));
            item.put("statusText", asInt(row.get("status")) == 1 ? "成功" : "失败");
            item.put("request_params", stringify(row.get("request_params")));
            item.put("response_result", stringify(row.get("response_result")));
            item.put("operated_at", formatDateTime(row.get("operated_at")));
            return item;
        }).toList();
    }

    /**
     * 列表行转换：补充 type/operator/ip/status/operTime 展示字段，JSON 字段解析为对象
     */
    private static Map<String, Object> transform(Map<String, Object> row) {
        Map<String, Object> item = new LinkedHashMap<>(row);
        item.put("type", displayType(String.valueOf(row.get("operationType"))));
        item.put("operator", row.get("operatorUsername"));
        item.put("ip", row.get("ipAddress"));
        item.put("status", asInt(row.get("status")) == 1 ? "成功" : "失败");
        item.put("operTime", row.get("operatedAt"));
        item.put("requestParams", parseJson(row.get("requestParams")));
        item.put("responseResult", parseJson(row.get("responseResult")));
        return item;
    }

    private static String displayType(String operationType) {
        return DISPLAY_TYPE.getOrDefault(operationType, operationType);
    }

    private static Object parseJson(Object value) {
        if (value == null) {
            return null;
        }
        return JacksonHolder.parseLenient(String.valueOf(value));
    }

    private static String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
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

    private static int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }
}

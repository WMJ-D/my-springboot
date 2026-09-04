package com.example.test.controller;

import com.example.test.common.ApiResponse;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.common.QueryUtils;
import com.example.test.security.RequirePermission;
import com.example.test.service.OperationLogService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.example.test.common.QueryUtils.CsvColumn;

/**
 * 操作日志接口，与 Express 侧 /api/v1/logs/operation 路由保持一致
 */
@RestController
@RequestMapping("/api/v1/logs/operation")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * GET /api/v1/logs/operation 分页查询
     */
    @GetMapping
    @RequirePermission("log:operation:list")
    public ApiResponse<PageResult<Map<String, Object>>> list(@RequestParam(required = false) Integer pageNum,
                                                             @RequestParam(required = false) Integer pageSize,
                                                             @RequestParam(required = false) String module,
                                                             @RequestParam(required = false) String operator,
                                                             @RequestParam(required = false) String operatorUsername,
                                                             @RequestParam(required = false) String type,
                                                             @RequestParam(required = false) String operationType,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) String dateRange,
                                                             @RequestParam(required = false) String startDate,
                                                             @RequestParam(required = false) String endDate) {
        String[] range = QueryUtils.parseDateRange(dateRange, startDate, endDate);
        return ApiResponse.ok(operationLogService.list(PageQuery.of(pageNum, pageSize), module,
                operator != null && !operator.isBlank() ? operator : operatorUsername,
                QueryUtils.resolveOperationType(type, operationType),
                QueryUtils.parseStatus(status), range[0], range[1]));
    }

    /**
     * DELETE /api/v1/logs/operation 批量删除
     */
    @DeleteMapping
    @RequirePermission("log:operation:delete")
    public ApiResponse<Object> delete(@RequestBody IdsBody body) {
        operationLogService.delete(parseIds(body));
        return ApiResponse.ok(null, "删除成功");
    }

    /**
     * GET /api/v1/logs/operation/export 导出 CSV
     */
    @GetMapping("/export")
    @RequirePermission("log:operation:export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String module,
                       @RequestParam(required = false) String operator,
                       @RequestParam(required = false) String operatorUsername,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) String operationType,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String dateRange,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) throws IOException {
        String[] range = QueryUtils.parseDateRange(dateRange, startDate, endDate);
        List<Map<String, Object>> rows = operationLogService.export(module,
                operator != null && !operator.isBlank() ? operator : operatorUsername,
                QueryUtils.resolveOperationType(type, operationType),
                QueryUtils.parseStatus(status), range[0], range[1]);
        List<CsvColumn> columns = List.of(
                new CsvColumn("module", "操作模块"), new CsvColumn("type", "操作类型"),
                new CsvColumn("description", "操作描述"), new CsvColumn("operator", "操作人"),
                new CsvColumn("request_url", "请求地址"), new CsvColumn("request_params", "请求参数"),
                new CsvColumn("response_result", "响应结果"), new CsvColumn("ip", "操作IP"),
                new CsvColumn("statusText", "状态"), new CsvColumn("error_message", "错误信息"),
                new CsvColumn("duration_ms", "耗时(ms)"), new CsvColumn("operated_at", "操作时间"));
        response.setContentType("text/csv; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"operation-logs.csv\"");
        response.getOutputStream().write(QueryUtils.buildCsv(columns, rows).getBytes(StandardCharsets.UTF_8));
    }

    public record IdsBody(List<Long> ids) {
    }

    static List<Long> parseIds(IdsBody body) {
        List<Long> ids = body == null || body.ids() == null ? List.of() : body.ids();
        List<Long> valid = ids.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (valid.isEmpty()) {
            throw new com.example.test.common.AppException(400, "请选择需要删除的记录");
        }
        return valid;
    }
}

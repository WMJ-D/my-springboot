package com.example.test.controller;

import com.example.test.common.ApiResponse;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.common.QueryUtils;
import com.example.test.security.RequirePermission;
import com.example.test.service.LoginLogService;
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
 * 登录日志接口，与 Express 侧 /api/v1/logs/login 路由保持一致
 */
@RestController
@RequestMapping("/api/v1/logs/login")
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    /**
     * GET /api/v1/logs/login 分页查询
     */
    @GetMapping
    @RequirePermission("log:login:list")
    public ApiResponse<PageResult<Map<String, Object>>> list(@RequestParam(required = false) Integer pageNum,
                                                             @RequestParam(required = false) Integer pageSize,
                                                             @RequestParam(required = false) String username,
                                                             @RequestParam(required = false) String ip,
                                                             @RequestParam(required = false) String ipAddress,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) String dateRange,
                                                             @RequestParam(required = false) String startDate,
                                                             @RequestParam(required = false) String endDate) {
        String[] range = QueryUtils.parseDateRange(dateRange, startDate, endDate);
        return ApiResponse.ok(loginLogService.list(PageQuery.of(pageNum, pageSize), username,
                ip != null && !ip.isBlank() ? ip : ipAddress,
                QueryUtils.parseStatus(status), range[0], range[1]));
    }

    /**
     * DELETE /api/v1/logs/login 批量删除
     */
    @DeleteMapping
    @RequirePermission("log:login:delete")
    public ApiResponse<Object> delete(@RequestBody OperationLogController.IdsBody body) {
        loginLogService.delete(OperationLogController.parseIds(body));
        return ApiResponse.ok(null, "删除成功");
    }

    /**
     * GET /api/v1/logs/login/export 导出 CSV
     */
    @GetMapping("/export")
    @RequirePermission("log:login:export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false) String ip,
                       @RequestParam(required = false) String ipAddress,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String dateRange,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) throws IOException {
        String[] range = QueryUtils.parseDateRange(dateRange, startDate, endDate);
        List<Map<String, Object>> rows = loginLogService.export(username,
                ip != null && !ip.isBlank() ? ip : ipAddress,
                QueryUtils.parseStatus(status), range[0], range[1]);
        List<CsvColumn> columns = List.of(
                new CsvColumn("username", "用户名"), new CsvColumn("ip_address", "登录IP"),
                new CsvColumn("location", "登录地点"), new CsvColumn("browser", "浏览器"),
                new CsvColumn("os", "操作系统"), new CsvColumn("statusText", "状态"),
                new CsvColumn("message", "提示信息"), new CsvColumn("login_at", "登录时间"));
        response.setContentType("text/csv; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"login-logs.csv\"");
        response.getOutputStream().write(QueryUtils.buildCsv(columns, rows).getBytes(StandardCharsets.UTF_8));
    }
}

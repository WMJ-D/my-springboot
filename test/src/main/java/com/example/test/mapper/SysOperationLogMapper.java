package com.example.test.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 操作日志表 sys_operation_log，对应 Express 侧 routes/logs.js 的操作日志查询
 */
@Mapper
public interface SysOperationLogMapper {

    /**
     * 操作日志写入（由操作日志过滤器调用）
     */
    @Insert("""
            INSERT INTO sys_operation_log
            (trace_id, module, operation_type, description, operator_id, operator_username,
             request_method, request_url, request_params, response_result, ip_address, user_agent,
             status, error_message, duration_ms)
            VALUES (#{traceId}, #{module}, #{operationType}, #{description}, #{operatorId}, #{operatorUsername},
                    #{requestMethod}, #{requestUrl}, #{requestParams}, #{responseResult}, #{ipAddress}, #{userAgent},
                    #{status}, #{errorMessage}, #{durationMs})
            """)
    int insertLog(@Param("traceId") String traceId, @Param("module") String module,
                  @Param("operationType") String operationType, @Param("description") String description,
                  @Param("operatorId") Long operatorId, @Param("operatorUsername") String operatorUsername,
                  @Param("requestMethod") String requestMethod, @Param("requestUrl") String requestUrl,
                  @Param("requestParams") String requestParams, @Param("responseResult") String responseResult,
                  @Param("ipAddress") String ipAddress, @Param("userAgent") String userAgent,
                  @Param("status") int status, @Param("errorMessage") String errorMessage,
                  @Param("durationMs") long durationMs);

    /**
     * 分页统计
     */
    long count(@Param("module") String module, @Param("operator") String operator,
               @Param("operationType") String operationType, @Param("status") Integer status,
               @Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 分页查询
     */
    List<Map<String, Object>> list(@Param("module") String module, @Param("operator") String operator,
                                   @Param("operationType") String operationType, @Param("status") Integer status,
                                   @Param("startTime") String startTime, @Param("endTime") String endTime,
                                   @Param("limit") int limit, @Param("offset") int offset);

    /**
     * 导出（最多 10000 条）
     */
    List<Map<String, Object>> export(@Param("module") String module, @Param("operator") String operator,
                                     @Param("operationType") String operationType, @Param("status") Integer status,
                                     @Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 批量逻辑删除
     */
    int deleteByIds(@Param("ids") List<Long> ids);
}

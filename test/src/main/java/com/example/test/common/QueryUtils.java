package com.example.test.common;

import java.util.Map;

/**
 * 日志查询参数工具：与 Express 侧 routes/logs.js 的查询参数约定保持一致
 */
public final class QueryUtils {

    private QueryUtils() {
    }

    /**
     * 状态筛选：'成功' 或 '1' 视为成功，其余非空值视为失败；空则不过滤
     */
    public static Integer parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return "成功".equals(status) || "1".equals(status) ? 1 : 0;
    }

    /**
     * 日期范围：dateRange（数组或逗号分隔）优先，其次 startDate/endDate
     *
     * @return [start, end]，元素可为 null
     */
    public static String[] parseDateRange(String dateRange, String startDate, String endDate) {
        String start = startDate;
        String end = endDate;
        if (dateRange != null && !dateRange.isBlank()) {
            String[] parts = dateRange.split(",");
            if (parts.length >= 2) {
                start = parts[0].isBlank() ? null : parts[0].trim();
                end = parts[1].isBlank() ? null : parts[1].trim();
            } else if (parts.length == 1 && !parts[0].isBlank()) {
                start = parts[0].trim();
            }
        }
        return new String[]{start, end};
    }

    /**
     * 操作日志类型筛选：支持中文（新增/修改/删除/查询/导出）与英文枚举
     */
    public static String resolveOperationType(String type, String operationType) {
        String raw = type != null && !type.isBlank() ? type : operationType;
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw) {
            case "新增" -> "CREATE";
            case "修改" -> "UPDATE";
            case "删除" -> "DELETE";
            case "查询" -> "QUERY";
            case "导出" -> "EXPORT";
            default -> raw;
        };
    }

    /**
     * CSV 单元格转义：含引号/逗号/换行时用双引号包裹，内部引号翻倍
     */
    public static String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return text.matches(".*[\",\n\r].*") ? "\"" + text + "\"" : text;
    }

    /**
     * CSV 表头描述
     */
    public record CsvColumn(String key, String label) {
    }

    /**
     * 生成带 BOM 的 UTF-8 CSV 内容
     */
    public static String buildCsv(java.util.List<CsvColumn> columns, java.util.List<Map<String, Object>> rows) {
        StringBuilder builder = new StringBuilder("\uFEFF");
        builder.append(columns.stream().map(column -> escapeCsv(column.label())).reduce((a, b) -> a + "," + b).orElse(""));
        for (Map<String, Object> row : rows) {
            builder.append("\r\n").append(columns.stream()
                    .map(column -> escapeCsv(row.get(column.key())))
                    .reduce((a, b) -> a + "," + b)
                    .orElse(""));
        }
        return builder.toString();
    }
}

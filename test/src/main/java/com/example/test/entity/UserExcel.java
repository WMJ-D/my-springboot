package com.example.test.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
@ColumnWidth(20)
public class UserExcel {

    @ExcelProperty("ID")
    private Integer id;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("值")
    private String value;
}

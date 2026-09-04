package com.example.test.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 系统参数管理，对应 Express 侧 routes/system.js 的参数相关 SQL
 */
@Mapper
public interface SysParamMapper {

    long count(@Param("paramName") String paramName, @Param("paramKey") String paramKey, @Param("paramType") String paramType);

    List<Map<String, Object>> list(@Param("paramName") String paramName, @Param("paramKey") String paramKey,
                                   @Param("paramType") String paramType, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * 新增参数，自增主键回写到 params.id
     */
    int insert(Map<String, Object> params);

    @Update("""
            UPDATE sys_param SET param_name=#{paramName}, param_key=#{paramKey}, param_value=#{paramValue},
                   param_type=#{paramType}, value_type=#{valueType}, remark=#{remark}, updated_by=#{updatedBy}
            WHERE id=#{id} AND deleted=0
            """)
    int update(@Param("paramName") String paramName, @Param("paramKey") String paramKey, @Param("paramValue") String paramValue,
               @Param("paramType") String paramType, @Param("valueType") String valueType, @Param("remark") String remark,
               @Param("updatedBy") long updatedBy, @Param("id") long id);

    int softDeleteByIds(@Param("updatedBy") long updatedBy, @Param("ids") List<Long> ids);
}

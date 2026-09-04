package com.example.test.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 文件元数据访问层
 */
@Mapper
public interface SysFileMapper {

    long count(@Param("appId") String appId, @Param("fileName") String fileName);

    List<Map<String, Object>> list(@Param("appId") String appId, @Param("fileName") String fileName,
                                   @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT id, app_id AS appId, original_name AS originalName, stored_name AS storedName, storage_path AS storagePath, content_type AS contentType, file_size AS fileSize, file_hash AS fileHash, uploader_id AS uploaderId, uploader_username AS uploaderUsername, created_at AS createdAt FROM sys_file WHERE id=#{id} AND deleted=0 AND (app_id=#{appId} OR app_id IS NULL)")
    Map<String, Object> findById(@Param("id") long id, @Param("appId") String appId);

    int insert(Map<String, Object> params);

    int softDeleteByIds(@Param("appId") String appId, @Param("ids") List<Long> ids);
}

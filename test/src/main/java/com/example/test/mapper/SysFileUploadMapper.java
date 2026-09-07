package com.example.test.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 分片上传会话与分片记录访问层
 */
@Mapper
public interface SysFileUploadMapper {

    int insertSession(Map<String, Object> params);

    @Select("SELECT id, upload_key AS uploadKey, app_id AS appId, file_name AS fileName, file_size AS fileSize, content_type AS contentType, chunk_size AS chunkSize, total_chunks AS totalChunks, status, file_id AS fileId, uploader_id AS uploaderId, uploader_username AS uploaderUsername FROM sys_file_upload WHERE upload_key = #{uploadKey}")
    Map<String, Object> findSessionByKey(@Param("uploadKey") String uploadKey);

    @Select("SELECT id, upload_key AS uploadKey, app_id AS appId, file_name AS fileName, file_size AS fileSize, content_type AS contentType, chunk_size AS chunkSize, total_chunks AS totalChunks, status, file_id AS fileId, uploader_id AS uploaderId, uploader_username AS uploaderUsername FROM sys_file_upload WHERE id = #{id}")
    Map<String, Object> findSessionById(@Param("id") long id);

    List<Integer> listChunkIndexes(@Param("uploadId") long uploadId);

    int insertChunkIgnore(@Param("uploadId") long uploadId, @Param("chunkIndex") int chunkIndex, @Param("chunkSize") long chunkSize);

    long countChunks(@Param("uploadId") long uploadId);

    int finishSession(@Param("id") long id, @Param("fileId") long fileId);

    int deleteChunks(@Param("uploadId") long uploadId);
}

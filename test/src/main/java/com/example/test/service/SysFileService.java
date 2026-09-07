package com.example.test.service;

import com.example.test.common.AppException;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.mapper.SysFileMapper;
import com.example.test.mapper.SysFileUploadMapper;
import com.example.test.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文件系统服务：文件内容落磁盘，数据库保存文件元数据
 */
@Service
public class SysFileService {

    /** 分片大小下限，防止恶意海量小分片请求 */
    private static final long MIN_CHUNK_SIZE = 512L * 1024;
    /** 分片大小上限，需小于 spring.servlet.multipart.max-file-size */
    private static final long MAX_CHUNK_SIZE = 50L * 1024 * 1024;
    /** 分片临时目录名，位于存储根目录下 */
    private static final String CHUNK_DIR_NAME = ".chunks";

    private final SysFileMapper fileMapper;
    private final SysFileUploadMapper uploadMapper;
    private final Path storageRoot;
    private final Path chunkRoot;

    public SysFileService(SysFileMapper fileMapper,
                          SysFileUploadMapper uploadMapper,
                          @Value("${app.file.storage-path:./data/files}") String storagePath) {
        this.fileMapper = fileMapper;
        this.uploadMapper = uploadMapper;
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        this.chunkRoot = storageRoot.resolve(CHUNK_DIR_NAME).normalize();
    }

    public PageResult<Map<String, Object>> list(PageQuery pageQuery, String appId, String fileName) {
        long total = fileMapper.count(appId, fileName);
        List<Map<String, Object>> rows = fileMapper.list(appId, fileName,
                pageQuery.getPageSize(), pageQuery.getOffset());
        for (Map<String, Object> row : rows) {
            Object size = row.get("fileSize");
            row.put("fileSizeText", formatSize(size instanceof Number number ? number.longValue() : 0));
            row.put("downloadUrl", "/api/v1/system/files/" + row.get("id") + "/download");
        }
        return new PageResult<>(rows, total, pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> upload(List<MultipartFile> files, String appId, CurrentUser user) {
        if (files == null || files.isEmpty()) {
            throw new AppException(400, "请选择需要上传的文件", "EMPTY_FILES");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        String safeAppId = normalizeAppId(appId);
        Path appDirectory = storageRoot.resolve(safeAppId == null ? "common" : safeAppId)
                .resolve(LocalDate.now().toString()).normalize();
        try {
            Files.createDirectories(appDirectory);
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String originalName = sanitizeOriginalName(file.getOriginalFilename());
                String storedName = UUID.randomUUID() + getExtension(originalName);
                Path target = appDirectory.resolve(storedName).normalize();
                if (!target.startsWith(appDirectory)) {
                    throw new AppException(400, "文件名不合法", "INVALID_FILE_NAME");
                }
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                String relativePath = storageRoot.relativize(target).toString().replace('\\', '/');
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("appId", appId);
                params.put("originalName", originalName);
                params.put("storedName", storedName);
                params.put("storagePath", relativePath);
                params.put("contentType", file.getContentType());
                params.put("fileSize", file.getSize());
                params.put("fileHash", sha256(target));
                params.put("uploaderId", user.userId());
                params.put("uploaderUsername", user.username());
                try {
                    fileMapper.insert(params);
                } catch (RuntimeException error) {
                    Files.deleteIfExists(target);
                    throw error;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", params.get("id"));
                item.put("originalName", originalName);
                item.put("fileSize", file.getSize());
                item.put("appId", appId);
                result.add(item);
            }
        } catch (IOException error) {
            throw new AppException(500, "文件上传失败", "FILE_UPLOAD_FAILED");
        }
        if (result.isEmpty()) {
            throw new AppException(400, "请选择有效文件", "EMPTY_FILES");
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids, String appId) {
        for (Long id : ids) {
            Map<String, Object> file = fileMapper.findById(id, appId);
            if (file != null) {
                deletePhysical(file.get("storagePath"));
            }
        }
        fileMapper.softDeleteByIds(appId, ids);
    }

    public DownloadFile download(long id, String appId) {
        Map<String, Object> file = fileMapper.findById(id, appId);
        if (file == null) {
            throw new AppException(404, "文件不存在", "FILE_NOT_FOUND");
        }
        String relativePath = String.valueOf(file.get("storagePath"));
        Path target = storageRoot.resolve(relativePath).normalize();
        if (!target.startsWith(storageRoot) || !Files.isRegularFile(target)) {
            throw new AppException(404, "文件内容不存在", "FILE_CONTENT_NOT_FOUND");
        }
        try {
            return new DownloadFile(target, String.valueOf(file.get("originalName")),
                    String.valueOf(file.getOrDefault("contentType", "application/octet-stream")), Files.size(target));
        } catch (IOException error) {
            throw new AppException(500, "读取文件失败", "FILE_READ_FAILED");
        }
    }

    // ==================== 大文件分片上传 ====================

    /**
     * 初始化分片上传：按 uploadKey 幂等。
     * 已完成的任务直接返回文件信息（秒传）；未完成的任务返回已上传分片序号（断点续传）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> initChunkUpload(InitChunkCommand command, String appId, CurrentUser user) {
        String uploadKey = command.uploadKey() == null ? "" : command.uploadKey().trim();
        if (uploadKey.isEmpty() || uploadKey.length() > 64) {
            throw new AppException(400, "上传标识不合法", "INVALID_UPLOAD_KEY");
        }
        long fileSize = command.fileSize();
        long chunkSize = command.chunkSize();
        int totalChunks = command.totalChunks();
        if (fileSize <= 0 || chunkSize < MIN_CHUNK_SIZE || chunkSize > MAX_CHUNK_SIZE) {
            throw new AppException(400, "分片参数不合法", "INVALID_CHUNK_PARAMS");
        }
        long expectedChunks = (fileSize + chunkSize - 1) / chunkSize;
        if (totalChunks != expectedChunks) {
            throw new AppException(400, "分片数量与文件大小不匹配", "CHUNK_COUNT_MISMATCH");
        }
        String safeAppId = normalizeAppId(appId);
        Map<String, Object> existing = uploadMapper.findSessionByKey(uploadKey);
        if (existing != null) {
            String existingAppId = existing.get("appId") == null ? null : String.valueOf(existing.get("appId"));
            boolean appMatched = safeAppId == null ? existingAppId == null : safeAppId.equals(existingAppId);
            if (!appMatched) {
                throw new AppException(409, "上传标识冲突，请重新选择文件", "UPLOAD_KEY_CONFLICT");
            }
            long uploadId = ((Number) existing.get("id")).longValue();
            Map<String, Object> result = new LinkedHashMap<>();
            if (((Number) existing.get("status")).intValue() == 1) {
                // 同一文件此前已上传完成，直接秒传
                result.put("finished", true);
                result.put("fileId", existing.get("fileId"));
                result.put("originalName", existing.get("fileName"));
                return result;
            }
            // 断点续传：返回已上传分片，前端跳过这些分片
            result.put("finished", false);
            result.put("uploadId", uploadId);
            result.put("uploadedChunks", uploadMapper.listChunkIndexes(uploadId));
            return result;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("uploadKey", uploadKey);
        params.put("appId", appId);
        params.put("fileName", sanitizeOriginalName(command.fileName()));
        params.put("fileSize", fileSize);
        params.put("contentType", command.contentType());
        params.put("chunkSize", chunkSize);
        params.put("totalChunks", totalChunks);
        params.put("uploaderId", user.userId());
        params.put("uploaderUsername", user.username());
        uploadMapper.insertSession(params);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("finished", false);
        result.put("uploadId", params.get("id"));
        result.put("uploadedChunks", List.of());
        return result;
    }

    /**
     * 上传单个分片：内容写入 .chunks/{uploadId}/{chunkIndex}.part 并记录到分片表（INSERT IGNORE 幂等）
     */
    public Map<String, Object> uploadChunk(long uploadId, int chunkIndex, MultipartFile file) {
        Map<String, Object> session = requireSession(uploadId);
        if (((Number) session.get("status")).intValue() != 0) {
            throw new AppException(400, "上传任务已完成", "UPLOAD_FINISHED");
        }
        int totalChunks = ((Number) session.get("totalChunks")).intValue();
        long fileSize = ((Number) session.get("fileSize")).longValue();
        long chunkSize = ((Number) session.get("chunkSize")).longValue();
        if (chunkIndex < 0 || chunkIndex >= totalChunks) {
            throw new AppException(400, "分片序号不合法", "INVALID_CHUNK_INDEX");
        }
        if (file == null || file.isEmpty()) {
            throw new AppException(400, "分片内容为空", "EMPTY_CHUNK");
        }
        // 最后一片为剩余字节数，其余分片必须等于标准分片大小
        long expectedSize = chunkIndex == totalChunks - 1
                ? fileSize - chunkSize * (totalChunks - 1)
                : chunkSize;
        if (file.getSize() != expectedSize) {
            throw new AppException(400, "分片大小不正确", "INVALID_CHUNK_SIZE");
        }
        Path chunkDir = chunkRoot.resolve(String.valueOf(uploadId)).normalize();
        if (!chunkDir.startsWith(chunkRoot)) {
            throw new AppException(400, "分片路径不合法", "INVALID_CHUNK_PATH");
        }
        Path target = chunkDir.resolve(chunkIndex + ".part").normalize();
        try {
            Files.createDirectories(chunkDir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            throw new AppException(500, "分片保存失败", "CHUNK_SAVE_FAILED");
        }
        uploadMapper.insertChunkIgnore(uploadId, chunkIndex, file.getSize());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("chunkIndex", chunkIndex);
        result.put("uploadedChunks", uploadMapper.countChunks(uploadId));
        result.put("totalChunks", totalChunks);
        return result;
    }

    /**
     * 合并分片：校验分片完整性后按序合并落盘，生成 sys_file 记录，标记会话完成并清理分片
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> mergeChunkUpload(long uploadId) {
        Map<String, Object> session = requireSession(uploadId);
        if (((Number) session.get("status")).intValue() == 1) {
            // 幂等：重复调用合并直接返回已生成的文件信息
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", session.get("fileId"));
            result.put("originalName", session.get("fileName"));
            result.put("fileSize", session.get("fileSize"));
            return result;
        }
        int totalChunks = ((Number) session.get("totalChunks")).intValue();
        long fileSize = ((Number) session.get("fileSize")).longValue();
        List<Integer> indexes = uploadMapper.listChunkIndexes(uploadId);
        if (indexes.size() != totalChunks) {
            throw new AppException(400, "分片不完整，还缺少 " + (totalChunks - indexes.size()) + " 个分片", "CHUNKS_INCOMPLETE");
        }
        String appId = session.get("appId") == null ? null : String.valueOf(session.get("appId"));
        String safeAppId = normalizeAppId(appId);
        String originalName = sanitizeOriginalName(String.valueOf(session.get("fileName")));
        Path chunkDir = chunkRoot.resolve(String.valueOf(uploadId)).normalize();
        Path appDirectory = storageRoot.resolve(safeAppId == null ? "common" : safeAppId)
                .resolve(LocalDate.now().toString()).normalize();
        String storedName = UUID.randomUUID() + getExtension(originalName);
        Path target = appDirectory.resolve(storedName).normalize();
        if (!target.startsWith(appDirectory) || !chunkDir.startsWith(chunkRoot)) {
            throw new AppException(400, "文件名不合法", "INVALID_FILE_NAME");
        }
        long fileId;
        try {
            Files.createDirectories(appDirectory);
            try (OutputStream output = Files.newOutputStream(target)) {
                for (int index = 0; index < totalChunks; index++) {
                    Path chunk = chunkDir.resolve(index + ".part");
                    if (!Files.isRegularFile(chunk)) {
                        throw new AppException(400, "分片内容缺失：" + index, "CHUNK_FILE_MISSING");
                    }
                    Files.copy(chunk, output);
                }
            }
            if (Files.size(target) != fileSize) {
                throw new AppException(500, "合并后文件大小不一致", "MERGE_SIZE_MISMATCH");
            }
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("appId", appId);
            params.put("originalName", originalName);
            params.put("storedName", storedName);
            params.put("storagePath", storageRoot.relativize(target).toString().replace('\\', '/'));
            params.put("contentType", session.get("contentType"));
            params.put("fileSize", fileSize);
            params.put("fileHash", sha256(target));
            params.put("uploaderId", session.get("uploaderId"));
            params.put("uploaderUsername", session.get("uploaderUsername"));
            fileMapper.insert(params);
            fileId = ((Number) params.get("id")).longValue();
        } catch (IOException error) {
            deleteQuietly(target);
            throw new AppException(500, "文件合并失败", "MERGE_FAILED");
        } catch (RuntimeException error) {
            deleteQuietly(target);
            throw error;
        }
        uploadMapper.finishSession(uploadId, fileId);
        uploadMapper.deleteChunks(uploadId);
        deleteDirectoryRecursively(chunkDir);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", fileId);
        result.put("originalName", originalName);
        result.put("fileSize", fileSize);
        return result;
    }

    private Map<String, Object> requireSession(long uploadId) {
        Map<String, Object> session = uploadMapper.findSessionById(uploadId);
        if (session == null) {
            throw new AppException(404, "上传任务不存在", "UPLOAD_NOT_FOUND");
        }
        return session;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private void deleteDirectoryRecursively(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
        } catch (IOException ignored) {
        }
    }

    /** 分片上传初始化命令 */
    public record InitChunkCommand(String uploadKey, String fileName, long fileSize,
                                   String contentType, long chunkSize, int totalChunks) {
    }

    private String normalizeAppId(String appId) {
        if (appId == null || appId.isBlank()) {
            return null;
        }
        String value = appId.trim();
        if (!value.matches("[A-Za-z0-9_-]+")) {
            throw new AppException(400, "子系统标识不合法", "INVALID_APP_ID");
        }
        return value;
    }

    private String sanitizeOriginalName(String name) {
        String value = name == null || name.isBlank() ? "unnamed" : name;
        value = Paths.get(value).getFileName().toString().replaceAll("[\\r\\n]", "");
        if (value.length() > 255) {
            value = value.substring(value.length() - 255);
        }
        return value;
    }

    private String getExtension(String name) {
        int index = name.lastIndexOf('.');
        return index > 0 && index < name.length() - 1 ? name.substring(index).toLowerCase() : "";
    }

    private String sha256(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 不可用", error);
        }
    }

    private void deletePhysical(Object pathValue) {
        if (pathValue == null) {
            return;
        }
        Path target = storageRoot.resolve(String.valueOf(pathValue)).normalize();
        if (!target.startsWith(storageRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
        return String.format("%.1f GB", bytes / 1024.0 / 1024.0 / 1024.0);
    }

    public record DownloadFile(Path path, String originalName, String contentType, long size) {
    }
}

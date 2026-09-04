package com.example.test.service;

import com.example.test.common.AppException;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.mapper.SysFileMapper;
import com.example.test.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
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

    private final SysFileMapper fileMapper;
    private final Path storageRoot;

    public SysFileService(SysFileMapper fileMapper,
                          @Value("${app.file.storage-path:./data/files}") String storagePath) {
        this.fileMapper = fileMapper;
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
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

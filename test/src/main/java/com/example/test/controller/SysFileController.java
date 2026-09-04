package com.example.test.controller;

import com.example.test.common.ApiResponse;
import com.example.test.common.PageQuery;
import com.example.test.common.PageResult;
import com.example.test.security.AuthContext;
import com.example.test.security.RequirePermission;
import com.example.test.security.SecurityUtils;
import com.example.test.service.SysFileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 文件系统接口：列表、批量上传、批量删除、下载
 */
@RestController
@RequestMapping("/api/v1/system/files")
public class SysFileController {

    private final SysFileService fileService;

    public SysFileController(SysFileService fileService) {
        this.fileService = fileService;
    }

    /**
     * GET /api/v1/system/files 查询当前 X-App-Id 所属系统的文件
     */
    @GetMapping
    @RequirePermission("system:file:list")
    public ApiResponse<PageResult<Map<String, Object>>> list(@RequestParam(required = false) Integer pageNum,
                                                             @RequestParam(required = false) Integer pageSize,
                                                             @RequestParam(required = false) String fileName,
                                                             HttpServletRequest request) {
        String appId = SecurityUtils.getAppId(request);
        return ApiResponse.ok(fileService.list(PageQuery.of(pageNum, pageSize), appId, fileName));
    }

    /**
     * POST /api/v1/system/files/upload 批量上传文件
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("system:file:upload")
    public ApiResponse<List<Map<String, Object>>> upload(@RequestParam("files") List<MultipartFile> files,
                                                         HttpServletRequest request) {
        return ApiResponse.ok(fileService.upload(files, SecurityUtils.getAppId(request), AuthContext.require()), "上传成功");
    }

    /**
     * DELETE /api/v1/system/files 批量删除文件
     */
    @DeleteMapping
    @RequirePermission("system:file:delete")
    public ApiResponse<Object> delete(@Valid @RequestBody IdsBody body, HttpServletRequest request) {
        fileService.delete(body.ids(), SecurityUtils.getAppId(request));
        return ApiResponse.ok(null, "删除成功");
    }

    /**
     * GET /api/v1/system/files/{id}/download 下载文件
     */
    @GetMapping("/{id}/download")
    @RequirePermission("system:file:download")
    public ResponseEntity<Resource> download(@PathVariable long id, HttpServletRequest request) {
        SysFileService.DownloadFile file = fileService.download(id, SecurityUtils.getAppId(request));
        FileSystemResource resource = new FileSystemResource(file.path());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.originalName(), StandardCharsets.UTF_8)
                .build();
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(file.contentType());
        } catch (Exception error) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    public record IdsBody(@NotEmpty(message = "请选择需要删除的文件") List<Long> ids) {
    }
}

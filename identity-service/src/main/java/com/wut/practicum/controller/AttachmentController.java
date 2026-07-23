package com.wut.practicum.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.wut.practicum.common.ApiResult;
import com.wut.practicum.entity.OaLeaveAttachment;
import com.wut.practicum.mapper.LeaveMapper;
import com.wut.practicum.service.OssService;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class AttachmentController {

    private final OssService ossService;
    private final OSS ossClient;
    private final LeaveMapper leaveMapper;

    @PostMapping("/{leaveId}/attachments")
    public ApiResult<AttachmentVo> upload(@PathVariable Long leaveId,
                                          @RequestParam("file") MultipartFile file) {
        String url = ossService.uploadFile(file);
        OaLeaveAttachment att = new OaLeaveAttachment();
        att.setLeaveId(leaveId);
        att.setFileName(file.getOriginalFilename());
        att.setFileUrl(url);
        att.setFileSize(file.getSize());
        att.setMimeType(file.getContentType());
        leaveMapper.insertAttachment(att);
        return ApiResult.success(new AttachmentVo(att.getId(), att.getFileName(), att.getFileUrl(),
                att.getFileSize(), att.getMimeType()), "上传成功");
    }

    @GetMapping("/{leaveId}/attachments")
    public ApiResult<List<AttachmentVo>> list(@PathVariable Long leaveId) {
        List<AttachmentVo> list = leaveMapper.selectAttachmentsByLeaveId(leaveId).stream()
                .map(a -> new AttachmentVo(a.getId(), a.getFileName(), a.getFileUrl(),
                        a.getFileSize(), a.getMimeType()))
                .toList();
        return ApiResult.success(list);
    }

    /** 下载附件（通过 OSS SDK 流式传输） */
    @GetMapping("/attachments/{attachId}/download")
    public void download(@PathVariable Long attachId, HttpServletResponse response) {
        var att = leaveMapper.selectAttachmentById(attachId);
        if (att == null) { sendError(response, 404, "附件不存在"); return; }

        try {
            // 从 OSS URL 中提取 object key
            String fileUrl = att.getFileUrl();
            String prefix = "https://" + ossService.getBucketName() + "." + ossService.getEndpoint() + "/";
            String key = fileUrl.startsWith(prefix) ? fileUrl.substring(prefix.length()) : fileUrl;

            OSSObject ossObject = ossClient.getObject(ossService.getBucketName(), key);

            response.setContentType(att.getMimeType() != null ? att.getMimeType() : "application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                    + URLEncoder.encode(att.getFileName(), StandardCharsets.UTF_8).replace("+", "%20"));

            try (InputStream is = ossObject.getObjectContent(); var os = response.getOutputStream()) {
                byte[] buf = new byte[8192]; int len;
                while ((len = is.read(buf)) != -1) os.write(buf, 0, len);
            }
        } catch (Exception e) {
            log.error("下载附件失败 id={}: {}", attachId, e.getMessage());
            sendError(response, 500, "下载失败");
        }
    }

    private void sendError(HttpServletResponse response, int code, String msg) {
        try {
            response.setStatus(code);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + msg + "\",\"data\":null}");
        } catch (Exception ignored) {}
    }

    public record AttachmentVo(Long id, String fileName, String fileUrl,
                               Long fileSize, String mimeType) {}
}

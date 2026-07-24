package com.wut.practicum.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {

    private final OSS ossClient;

    @Value("${oss.bucket-name:${OSS_BUCKET_NAME:${ALIYUN_OSS_BUCKET_NAME:oa-service720}}}")
    private String bucketName;

    @Value("${oss.endpoint:${OSS_ENDPOINT:${ALIYUN_OSS_ENDPOINT:oss-cn-beijing.aliyuncs.com}}}")
    private String endpoint;

    public String getBucketName() { return bucketName; }
    public String getEndpoint() { return endpoint; }

    public String uploadFile(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String key = "leave/" + UUID.randomUUID().toString().replace("-", "") + ext;

        try (InputStream is = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            if (file.getContentType() != null) {
                metadata.setContentType(file.getContentType());
            }
            // 显式设置上传文件的 Object ACL 为公共读 (PublicRead)，确保生成的图片 URL 在浏览器中可公开预览
            metadata.setObjectAcl(com.aliyun.oss.model.CannedAccessControlList.PublicRead);

            ossClient.putObject(bucketName, key, is, metadata);
            String url = "https://" + bucketName + "." + endpoint + "/" + key;
            log.info("OSS upload OK: {}", key);
            return url;
        } catch (OSSException oe) {
            log.error("OSS upload FAILED [OSSException]: ErrorCode={}, RequestId={}, HostId={}, Msg={}",
                    oe.getErrorCode(), oe.getRequestId(), oe.getHostId(), oe.getErrorMessage());
            throw new RuntimeException("文件上传失败（阿里云OSS权限拒绝: " + oe.getErrorMessage() + "）", oe);
        } catch (Exception e) {
            log.error("OSS upload FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        }
    }
}

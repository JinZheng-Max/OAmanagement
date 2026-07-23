package com.wut.practicum.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OssConfig {

    @Value("${oss.endpoint:${OSS_ENDPOINT:${ALIYUN_OSS_ENDPOINT:oss-cn-beijing.aliyuncs.com}}}")
    private String endpoint;

    @Value("${oss.access-key-id:${OSS_ACCESS_KEY_ID:${ALIYUN_OSS_ACCESS_KEY_ID:}}}")
    private String accessKeyId;

    @Value("${oss.access-key-secret:${OSS_ACCESS_KEY_SECRET:${ALIYUN_OSS_ACCESS_KEY_SECRET:}}}")
    private String accessKeySecret;

    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}

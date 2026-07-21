package com.wut.practicum.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

/**
 * Elasticsearch 客户端与连接池优化配置 (支持 HTTPS 自签名证书与账号密码认证)
 */
@Configuration
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:https://localhost:9200}")
    private String uris;

    @Value("${spring.elasticsearch.username:elastic}")
    private String username;

    @Value("${spring.elasticsearch.password:64526691aA}")
    private String password;

    @Bean
    public RestClientBuilder customRestClientBuilder() {
        String host = "localhost";
        int port = 9200;
        String scheme = "https";

        try {
            if (uris.startsWith("https://")) {
                scheme = "https";
                String clean = uris.replace("https://", "");
                if (clean.contains(":")) {
                    String[] parts = clean.split(":");
                    host = parts[0];
                    port = Integer.parseInt(parts[1]);
                } else {
                    host = clean;
                }
            } else if (uris.startsWith("http://")) {
                scheme = "http";
                String clean = uris.replace("http://", "");
                if (clean.contains(":")) {
                    String[] parts = clean.split(":");
                    host = parts[0];
                    port = Integer.parseInt(parts[1]);
                } else {
                    host = clean;
                }
            }
        } catch (Exception ignored) {
        }

        final String finalScheme = scheme;
        final String finalHost = host;
        final int finalPort = port;

        return RestClient.builder(new HttpHost(finalHost, finalPort, finalScheme))
                .setRequestConfigCallback(requestConfigBuilder ->
                        requestConfigBuilder
                                .setConnectTimeout(3000)
                                .setSocketTimeout(5000)
                                .setConnectionRequestTimeout(3000))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    // 1. 如果配置了账号密码，设置 Basic Auth 身份凭证
                    if (username != null && !username.trim().isEmpty()) {
                        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }

                    // 2. 如果是 HTTPS 协议，忽略自签名证书校验
                    if ("https".equalsIgnoreCase(finalScheme)) {
                        try {
                            SSLContext sslContext = SSLContextBuilder.create()
                                    .loadTrustMaterial(null, (chain, authType) -> true)
                                    .build();
                            httpClientBuilder.setSSLContext(sslContext);
                            httpClientBuilder.setSSLHostnameVerifier((hostname, session) -> true);
                        } catch (Exception e) {
                            throw new RuntimeException("配置 ES HTTPS SSL 信任凭证失败", e);
                        }
                    }

                    return httpClientBuilder
                            .setKeepAliveStrategy((response, context) -> 30000)
                            .setMaxConnTotal(100)
                            .setMaxConnPerRoute(30);
                });
    }
}

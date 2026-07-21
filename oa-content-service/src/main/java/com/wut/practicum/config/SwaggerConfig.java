package com.wut.practicum.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智办AI OA - 公告制度与全文检索微服务 API")
                        .version("1.0.0")
                        .description("提供公告制度草稿维护、发布/下架、Redis缓存与ES全文检索能力"));
    }
}

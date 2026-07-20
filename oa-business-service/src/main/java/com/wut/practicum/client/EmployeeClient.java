package com.wut.practicum.client;

import com.wut.practicum.common.ApiResult;
import com.wut.practicum.dto.EmployeeResponse;
import com.wut.practicum.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "identity-service", configuration = FeignConfig.class)
public interface EmployeeClient {
    @GetMapping("/api/employees/{id}")
    ApiResult<EmployeeResponse> getById(@PathVariable("id") Long id);
}

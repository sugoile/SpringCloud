package com.xsg.springcloud.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @auther zzyy
 * @create 2020-02-25 15:04
 */
@RestController
public class RateLimitController
{
    @GetMapping("/byResource")
    @SentinelResource(value = "byResource",blockHandler = "handleException")
    public String byResource()
    {
        return "按资源名称限流测试";
    }
    public String handleException(BlockException exception)
    {
        return exception.getClass().getCanonicalName()+"\t 服务不可用";
    }

    @GetMapping("/byUrl")
    @SentinelResource(value = "byUrl",blockHandler = "handleException")
    public String byUrl()
    {
        return "按url限流测试";
    }


    @GetMapping("/rateLimit/customerBlockHandler")
    @SentinelResource(value = "customerBlockHandler",
            blockHandlerClass = CustomerBlockHandler.class,
            blockHandler = "handlerException2")
    public String customerBlockHandler()
    {
        return "按客戶自定义测试";
    }
}
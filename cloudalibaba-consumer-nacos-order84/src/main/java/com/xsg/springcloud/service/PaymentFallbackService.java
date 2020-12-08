package com.xsg.springcloud.service;

import org.springframework.stereotype.Component;

/**
 * @auther zzyy
 * @create 2020-02-25 18:30
 */
@Component
public class PaymentFallbackService implements PaymentService
{
    @Override
    public String paymentSQL(Long id)
    {
        return "服务降级返回";
    }
}

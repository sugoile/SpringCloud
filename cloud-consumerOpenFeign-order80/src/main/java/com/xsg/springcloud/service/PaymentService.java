package com.xsg.springcloud.service;

import com.xsg.springcloud.entities.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @des:
 * @package: com.xsg.springcloud.service
 * @author: xsg
 * @date: 2020/11/23
 **/
@Component
@FeignClient(value = "CLOUD-PAYMENT-SERVICE")   //指向哪一个微服务地址
public interface PaymentService {
    @GetMapping(value = "/payment/get/{id}")
    CommonResult getPaymentById(@PathVariable("id") Long id);

    @GetMapping(value = "/payment/feign/timeout")
    String paymentFeignTimeout();
}

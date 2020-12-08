package com.xsg.springcloud.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @des:
 * @package: com.xsg.springcloud.controller
 * @author: xsg
 * @date: 2020/12/3
 **/
@RestController
public class PaymentController
{
    @Value("${server.port}")
    private String serverPort;

    @GetMapping(value = "/payment/nacos")
    public String getPayment()
    {
        return "nacos start,serverPort: "+ serverPort;
    }

    @GetMapping(value = "/paymentSQL/{id}")
    public String paymentSQL(@PathVariable("id") Long id)
    {
        if(id == 1){
            return serverPort + "调用成功";
        }else{
            return null;
        }
    }
}

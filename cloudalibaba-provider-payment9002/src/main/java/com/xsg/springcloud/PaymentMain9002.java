package com.xsg.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @des:
 * @package: com.xsg.springcloud
 * @author: xsg
 * @date: 2020/12/3
 **/
@EnableDiscoveryClient
@SpringBootApplication
public class PaymentMain9002
{
    public static void main(String[] args) {
        SpringApplication.run(PaymentMain9002.class, args);
    }
}

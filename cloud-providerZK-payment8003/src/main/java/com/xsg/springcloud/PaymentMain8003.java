package com.xsg.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @des:
 * @package: com.xsg.springcloud
 * @author: xsg
 * @date: 2020/11/18
 **/
@SpringBootApplication
@EnableDiscoveryClient  //该注解用于向使用consul或者zookeeper作为注册中心时注册服务
public class PaymentMain8003 {
    public static void main(String[] args) {
        SpringApplication.run(PaymentMain8003.class, args);
    }
}

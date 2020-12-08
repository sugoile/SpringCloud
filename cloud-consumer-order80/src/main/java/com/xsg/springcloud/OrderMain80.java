package com.xsg.springcloud;

import com.xsg.ribbon.RibbonRule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;

/**
 * @des:
 * @package: com.xsg.springcloud
 * @author: xsg
 * @date: 2020/11/13
 **/
@SpringBootApplication
@EnableEurekaClient
@RibbonClient(name = "CLOUD-PAYMENT-SERVICE", configuration= RibbonRule.class)
//name是服务提供者名称（地址），表明负载均衡用在服务提供者上
//configuration = RibbonRule.class是调用自定义的负载均衡配置类（使用自定义的随机算法）
public class OrderMain80 {
    public static void main(String[] args) {
        SpringApplication.run(OrderMain80.class, args);
    }
}

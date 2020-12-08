package com.xsg.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @des:
 * @package: com.xsg.springcloud
 * @author: xsg
 * @date: 2020/12/1
 **/
@SpringBootApplication
@EnableConfigServer
@EnableEurekaClient
public class ConfigServer3344 {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServer3344.class, args);
    }
}

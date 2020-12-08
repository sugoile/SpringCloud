package com.xsg.springcloud.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @des:
 * @package: com.xsg.springcloud.config
 * @author: xsg
 * @date: 2020/12/3
 **/
@Configuration
public class ApplicationConfig {
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}

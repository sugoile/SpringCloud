package com.xsg.ribbon;

import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.RandomRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @des:
 * @package: com.xsg.ribbon
 * @author: xsg
 * @date: 2020/11/22
 **/
@Configuration
public class RibbonRule {
    @Bean
    public IRule MyRule(){
        return new RandomRule();
    }
}

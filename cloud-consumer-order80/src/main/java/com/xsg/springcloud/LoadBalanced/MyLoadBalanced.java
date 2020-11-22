package com.xsg.springcloud.LoadBalanced;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 * @des:
 * @package: com.xsg.springcloud.LoadBalanced
 * @author: xsg
 * @date: 2020/11/22
 **/
public interface MyLoadBalanced {
    ServiceInstance instances(List<ServiceInstance> serviceInstances);
}

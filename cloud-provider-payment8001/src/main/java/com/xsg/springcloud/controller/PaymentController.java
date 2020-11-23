package com.xsg.springcloud.controller;

import com.xsg.springcloud.entities.CommonResult;
import com.xsg.springcloud.entities.Payment;
import com.xsg.springcloud.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @des:
 * @package: com.xsg.springcloud.controller
 * @author: xsg
 * @date: 2020/11/10
 **/
@RestController
@Slf4j
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Value("${server.port}")
    private String Server;

    @PostMapping(value = "/payment/create")
    public CommonResult create(@RequestBody Payment payment)
    {
        Long result = paymentService.create(payment);
        log.info("*****插入结果："+result);
        if(result > 0)
        {
            return new CommonResult(200,"插入数据库成功, Server: " + Server, result);
        }else{
            return new CommonResult(444,"插入数据库失败",null);
        }
    }

    @GetMapping(value = "/payment/get/{id}")
    public CommonResult getPaymentById(@PathVariable("id") Long id)
    {
        Payment payment = paymentService.getPaymentById(id);

        if(payment != null)
        {
            return new CommonResult(200,"查询成功, Server: " + Server, payment);
        }else{
            return new CommonResult(444,"没有对应记录,查询ID: "+id,null);
        }
    }


    /**
     * 服务发现，用于简介该controller的一些简介信息
     * services 返回所有的服务类型 ("CLOUD-ORDER-SERVICE", "CLOUD-PAYMENT-SERVICE")
     * instances 通过传入某一service的id来返回 接口、主机等信息
     */
    @GetMapping(value = "/payment/discoveryClient")
    public Object discoveryClient(){
        List<String> services = discoveryClient.getServices();
        for (String service : services){
                log.info("\"" + service + "\" ");
        }
        List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PAYMENT-SERVICE");
        for (ServiceInstance instance : instances){
            log.info(instance.getServiceId() + "\t" + instance.getPort() + "\t" + instance.getUri());
        }
        return this.discoveryClient;
    }

    @GetMapping(value = "/payment/serverport")
    public String serverport(){
        return Server;
    }

    @GetMapping(value = "/payment/feign/timeout")
    public String paymentFeignTimeout()
    {
        // 业务逻辑处理正确，但是需要耗费3秒钟
        try { TimeUnit.SECONDS.sleep(3); } catch (InterruptedException e) { e.printStackTrace(); }
        return Server;
    }
}

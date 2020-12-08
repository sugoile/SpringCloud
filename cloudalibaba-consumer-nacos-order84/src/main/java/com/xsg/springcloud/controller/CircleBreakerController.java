package com.xsg.springcloud.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.xsg.springcloud.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;


/**
 * @des:
 * @package: com.xsg.springcloud.controller
 * @author: xsg
 * @date: 2020/12/8
 **/
@RestController
public class CircleBreakerController {
    public static final String SERVICE_URL = "http://nacos-payment-provider";

    @Autowired
    private RestTemplate restTemplate;


    @RequestMapping("/consumer/fallback/{id}")
    //@SentinelResource(value = "fallback") //没有配置
    //@SentinelResource(value = "fallback",fallback = "handlerFallback") //fallback只负责业务异常
    //@SentinelResource(value = "fallback",blockHandler = "blockHandler") //blockHandler只负责sentinel控制台配置违规
    @SentinelResource(value = "fallback",fallback = "handlerFallback",blockHandler = "blockHandler",
            exceptionsToIgnore = {IllegalArgumentException.class})
    public String fallback(@PathVariable Long id)
    {
        String result = restTemplate.getForObject(SERVICE_URL + "/paymentSQL/"+id,String.class,id);

        if (id == 2) {
            throw new IllegalArgumentException ("IllegalArgumentException,非法参数异常....");
        }else if (result == null) {
            throw new NullPointerException ("NullPointerException,该ID没有对应记录,空指针异常");
        }

        return result;
    }
    //本例是fallback
    public String handlerFallback(@PathVariable  Long id,Throwable e) {
        return "服务降级fallback,exception内容  "+e.getMessage();
    }
    //本例是blockHandler
    public String blockHandler(@PathVariable  Long id, BlockException blockException) {
        return "服务降级blockHandler,exception内容  "+blockException.getMessage();
    }

    //==================OpenFeign
    @Autowired
    private PaymentService paymentService;

    @GetMapping(value = "/consumer/paymentSQL/{id}")
    public String paymentSQL(@PathVariable("id") Long id)
    {
        return paymentService.paymentSQL(id);
    }
}

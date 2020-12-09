package com.xsg.springcloud.controller;

import com.xsg.springcloud.domain.CommonResult;
import com.xsg.springcloud.domain.Order;
import com.xsg.springcloud.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @auther zzyy
 * @create 2020-02-26 15:24
 */
@RestController
public class OrderController
{
    @Resource
    private OrderService orderService;


    @GetMapping("/order/create")
    public CommonResult create(Order order)
    {
        System.out.println(order);
        orderService.create(order);
        return new CommonResult(200,"订单创建成功");
    }
}

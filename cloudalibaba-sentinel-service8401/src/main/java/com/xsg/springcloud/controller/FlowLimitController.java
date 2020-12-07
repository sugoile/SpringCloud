package com.xsg.springcloud.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * @des:
 * @package: com.xsg.springcloud.controller
 * @author: xsg
 * @date: 2020/12/7
 **/
@RestController
@Slf4j
public class FlowLimitController {

    @GetMapping("/test1")
    public String test1()
    {
        return "test1";
    }

    @GetMapping("/test2")
    public String test2()
    {
        //试验在流量控制的线程数达到阈值
//        try {
//            TimeUnit.MILLISECONDS.sleep(3000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        log.info(Thread.currentThread().getName() + "-----/test2");
        return "test2";
    }
}

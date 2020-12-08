package com.xsg.springcloud.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/test3")
    public String test3()
    {
        int age = 10 / 0;
        return "test3";
    }

    @GetMapping("/test4")
    public String test4()
    {
        try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); }
        return "test4";
    }

    @GetMapping("/test5")
    @SentinelResource(value = "test", blockHandler = "deal_testHotKey")
    public String test5(@RequestParam(value = "p1",required = false) String p1,
                        @RequestParam(value = "p2",required = false) String p2){
       return "/test5";
    }
    public String deal_testHotKey (String p1, String p2, BlockException exception)
    {
        return "------deal_testHotKey";  //sentinel系统默认的提示：Blocked by Sentinel (flow limiting)
    }

    @GetMapping("/hello")
    public String hello() {
        return "hello sentinel";
    }

}

package com.xsg.springcloud.controller;

import com.alibaba.csp.sentinel.slots.block.BlockException;

/**
 * @auther zzyy
 * @create 2020-02-25 15:32
 */
public class CustomerBlockHandler
{
    public static String handlerException(BlockException exception)
    {
        return "按客戶自定义,global handlerException----1";
    }
    public static String handlerException2(BlockException exception)
    {
        return "按客戶自定义,global handlerException----2";
    }
}

package com.xsg.springcloud.serive;

import org.springframework.stereotype.Service;

/**
 * @des:
 * @package: com.xsg.springcloud.serive
 * @author: xsg
 * @date: 2020/11/29
 **/
@Service
public class PaymentFallbackService implements PaymentHystrixService {
    @Override
    public String paymentInfo_OK(Integer id) {
        return "PaymentFallbackService fall back-paymentInfo_OK";
    }

    @Override
    public String paymentInfo_TimeOut(Integer id) {
        return "PaymentFallbackService fall back-paymentInfo_TimeOut";
    }
}

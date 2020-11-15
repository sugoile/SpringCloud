package com.xsg.springcloud.service;

import com.xsg.springcloud.entities.Payment;
import org.apache.ibatis.annotations.Param;

/**
 * @des:
 * @package: com.xsg.springcloud.service
 * @author: xsg
 * @date: 2020/11/10
 **/
public interface PaymentService {
    public Long create(Payment payment);

    public Payment getPaymentById(@Param("id") Long id);
}

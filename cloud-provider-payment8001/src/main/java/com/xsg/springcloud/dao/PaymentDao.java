package com.xsg.springcloud.dao;

import com.xsg.springcloud.entities.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @des:
 * @package: com.xsg.springcloud.dao
 * @author: xsg
 * @date: 2020/11/10
 **/
@Mapper
public interface PaymentDao {
    public int create(Payment payment);

    public Payment getPaymentById(@Param("id") Long id);
}

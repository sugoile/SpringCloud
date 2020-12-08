package com.xsg.springcloud.service.Imp;

import com.xsg.springcloud.dao.PaymentDao;
import com.xsg.springcloud.entities.Payment;
import com.xsg.springcloud.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @auther zzyy
 * @create 2020-02-18 10:40
 */
@Service
public class PaymentServiceImp implements PaymentService {
    @Autowired
    private PaymentDao paymentDao;

    public Long create(Payment payment) {
         paymentDao.create(payment);
         return payment.getId();
    }

    public Payment getPaymentById(Long id) {
        return paymentDao.getPaymentById(id);
    }
}

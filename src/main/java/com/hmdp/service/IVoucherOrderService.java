package com.hmdp.service;

import com.hmdp.constants.MQConstants;
import com.hmdp.dto.VoucherOrderIdDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.result.Result;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutionException;

import static com.hmdp.constants.MQConstants.CREATE_ORDER_EXCHANGE;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result buySeckillVoucher (Long voucherId) throws ExecutionException, InterruptedException;

    void createVoucherOrder (VoucherOrderIdDTO voucherOrderIdDTO);
}

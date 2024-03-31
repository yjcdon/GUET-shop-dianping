package com.hmdp.service;

import com.hmdp.dto.VoucherOrderIdDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.result.Result;

import java.util.concurrent.ExecutionException;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result buySeckillVoucher (Long voucherId) throws ExecutionException, InterruptedException;

    void createVoucherOrder (Long orderId,Long voucherId);

}

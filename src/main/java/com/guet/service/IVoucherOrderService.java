package com.guet.service;

import com.guet.dto.VoucherOrderIdDTO;
import com.guet.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guet.result.Result;

import java.util.concurrent.ExecutionException;

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

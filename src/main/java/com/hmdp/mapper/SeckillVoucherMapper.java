package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;

/**
 * @author lyj
 * @description 针对表【tb_seckill_voucher(秒杀优惠券表，与优惠券是一对一关系)】的数据库操作Mapper
 * @createDate 2024-02-15 18:09:23
 * @Entity com.hmdp.entity.SeckillVoucher
 */
public interface SeckillVoucherMapper extends BaseMapper<SeckillVoucher> {

    boolean updateWithLock (SeckillVoucher seckillVoucher);
}





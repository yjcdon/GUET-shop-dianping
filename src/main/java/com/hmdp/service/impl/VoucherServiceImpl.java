package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.annotation.JudgeIdExist;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.result.Result;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.MyRedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.hmdp.constants.RedisConstants.SECKILL_STOCK_KEY;

@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Autowired
    private SeckillVoucherMapper seckillVoucherMapper;

    @Autowired
    private StringRedisTemplate srt;

    @Override
    @JudgeIdExist(key = "shopIds")
    public Result queryVoucherOfShop (Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);

        if (!vouchers.isEmpty()) {
            // 写入Redis
            for (Voucher voucher : vouchers) {
                String key = SECKILL_STOCK_KEY + voucher.getId();
                if (voucher.getStock() != null) {
                    srt.opsForValue().set(key, voucher.getStock().toString());
                }
            }
        }

        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher (Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherMapper.insert(seckillVoucher);

        // 保存到Redis
        // 保存库存到Redis，key是voucherid，value是库存数
        srt.opsForValue().set(SECKILL_STOCK_KEY + voucher.getId(), seckillVoucher.getStock().toString());

    }
}

package com.guet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guet.annotation.JudgeIdExist;
import com.guet.entity.SeckillVoucher;
import com.guet.entity.Voucher;
import com.guet.mapper.SeckillVoucherMapper;
import com.guet.mapper.VoucherMapper;
import com.guet.result.Result;
import com.guet.service.IVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.guet.constants.RedisConstants.SECKILL_STOCK_KEY;

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

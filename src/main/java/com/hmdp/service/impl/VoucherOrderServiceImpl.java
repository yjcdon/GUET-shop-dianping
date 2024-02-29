package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.result.Result;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.MyRedisUtils;
import com.hmdp.utils.UserHolderUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.hmdp.constants.RedisConstants.CACHE_ORDER_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private SeckillVoucherMapper seckillVoucherMapper;

    @Autowired
    private VoucherMapper voucherMapper;

    @Autowired
    private VoucherOrderMapper voucherOrderMapper;

    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private MyRedisUtils redisUtils;

    @Autowired
    private StringRedisTemplate srt;

    @Autowired
    private RedissonClient redissonClient;

    // 判断用户是否有下单资格的脚本
    private static final DefaultRedisScript<Long> JUDGE_SCRIPT;

    // 初始化脚本
    static {
        JUDGE_SCRIPT = new DefaultRedisScript<>();
        JUDGE_SCRIPT.setLocation(new ClassPathResource("./Lua/seckill.lua"));
        JUDGE_SCRIPT.setResultType(Long.class);
    }

    // 阻塞队列，执行订单的数据库写入操作
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    // 创建线程池
    private static final ExecutorService seckillOrderExecutor = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init () {
        seckillOrderExecutor.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run () {
            while (true) {
                // 获取队列中的订单信息
                try {
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 创建订单
                    proxy.createVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("订单处理异常" + e);
                }
            }
        }
    }

    /*private void handlerVoucherOrder (VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("lock:" + CACHE_ORDER_KEY + userId);

        boolean isLock = lock.tryLock();
        if (!isLock) {
            // 获取锁失败，返回失败信息或重试
            log.error("不允许重复下单");
        }

        try {
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }
    }*/

    IVoucherOrderService proxy;

    @Override
    public Result buySeckillVoucher (Long voucherId) {
        Long userId = UserHolderUtil.getUser().getId();

        //     执行Lua脚本，判断返回值是否为0
        Long result = srt.execute(JUDGE_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString()
        );
        int r = result.intValue();
        // 返回值不为0，不能下单
        if (r != 0) {
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }

        // 返回值为0，可以下单，
        // todo 把下单信息保存到队列中
        long orderId = redisUtils.idGenerator(CACHE_ORDER_KEY + voucherId);

        // 封装订单信息
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(redisUtils.idGenerator(CACHE_ORDER_KEY));
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);

        // 创建队列
        // 创建代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        // 返回订单ID
        return Result.ok(orderId);
    }


    /*@Override
    public Result buySeckillVoucher (Long voucherId) {
        // 查询优惠券id
        SeckillVoucher seckillVoucher = seckillVoucherMapper.selectById(voucherId);
        Voucher voucher = voucherMapper.selectById(voucherId);

        if (seckillVoucher == null) {
            return Result.fail("优惠券不存在");
        }

        // 对时间做判断
        // 没到开始时间，返回错误信息
        if (LocalDateTime.now().isBefore(seckillVoucher.getBeginTime())) {
            return Result.fail("秒杀未开始！");
        }

        // 已过期
        if (LocalDateTime.now().isAfter(seckillVoucher.getEndTime())) {
            voucher.setStatus(3);
            return Result.fail("秒杀已结束！");
        }

        // 在规定时间内才能继续

        // 查询库存
        // 库存不足则不给下单
        if (seckillVoucher.getStock() < 1) {
            return Result.fail("库存不足！");
        }

        // 判断该用户是否购买过这张优惠券，如果买过了就不能继续买
        Long userId = UserHolderUtil.getUser().getId();
        // RedisLock lock = new RedisLock(srt, CACHE_ORDER_KEY + userId);
        RLock lock = redissonClient.getLock("lock:" + CACHE_ORDER_KEY + userId);

        boolean isLock = lock.tryLock();
        if (!isLock) {
            // 获取锁失败，返回失败信息或重试
            return Result.fail("不允许重复下单！");
        }

        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(seckillVoucher);
        } finally {
            lock.unlock();
        }
    }*/

    @Transactional
    public Result createVoucherOrder (VoucherOrder voucherOrder) {
        // 库存充足，修改库存数据
        // 如果并发量稍微高一些就会导致超卖问题，解决方法是乐观锁
        boolean success = seckillVoucherMapper.updateWithLock(voucherOrder);

        // 要多进行一次判断，更新成功才能插入数据，即购买成功
        if (!success) {
            return Result.fail("库存不足");
        }

        // 给voucher_order表加一条数据，返回订单id
        voucherOrderMapper.insert(voucherOrder);

        return Result.ok(voucherOrder.getId());
    }
}



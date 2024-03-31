package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.constants.MQConstants;
import com.hmdp.dto.VoucherOrderIdDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.result.Result;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.MyRedisUtils;
import com.hmdp.utils.UserHolderUtil;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.hmdp.constants.MQConstants.CREATE_ORDER_EXCHANGE;
import static com.hmdp.constants.RedisConstants.CACHE_ORDER_KEY;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private SeckillVoucherMapper seckillVoucherMapper;

    @Autowired
    private VoucherOrderMapper voucherOrderMapper;

    @Autowired
    private MyRedisUtils redisUtils;

    @Autowired
    private StringRedisTemplate srt;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    ExecutorService pool = Executors.newFixedThreadPool(12);

    // 判断用户是否有下单资格的脚本
    private static final DefaultRedisScript<Long> JUDGE_SCRIPT;

    // 加载并初始化脚本
    static {
        JUDGE_SCRIPT = new DefaultRedisScript<>();
        JUDGE_SCRIPT.setLocation(new ClassPathResource("./Lua/seckill.lua"));
        JUDGE_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result buySeckillVoucher (Long voucherId) {
        Long userId = UserHolderUtil.getUser().getId();

        // 执行Lua脚本，获得脚本的返回值；但是在执行前它会去Redis中查库存，你要提前将优惠券信息写入Redis
        // 每当有请求进入shop，就会查询该店铺的优惠券信息，查完就把对应优惠券的库存写入Redis
        int result = srt.execute(JUDGE_SCRIPT,
                        Collections.emptyList(),
                        voucherId.toString(), userId.toString())
                .intValue();

        // 如果返回值不为0，则不能下单
        if (result != 0) {
            return Result.fail(result == 1 ? "库存不足" : "不能重复下单");
        }

        // 可以下单，这里应该是加分布式锁的地方，不是分布式，通过Lua脚本可以实现一人一单了
        Long orderId = redisUtils.idGenerator(CACHE_ORDER_KEY + voucherId);
        VoucherOrderIdDTO voucherOrderIdDTO = new VoucherOrderIdDTO();
        voucherOrderIdDTO.setOrderId(orderId);
        voucherOrderIdDTO.setVoucherId(voucherId);
        voucherOrderIdDTO.setUserId(userId); // 测试发现不加这个会报空指针异常

        rabbitTemplate.convertAndSend(CREATE_ORDER_EXCHANGE, "order", voucherOrderIdDTO);

        // 返回订单ID
        return Result.ok(orderId);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.CREATE_ORDER_QUEUE, durable = "true"),
            exchange = @Exchange(name = CREATE_ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = {"order"}
    ))
    @Transactional
    @Override
    public void createVoucherOrder (VoucherOrderIdDTO voucherOrderIdDTO) {
        Long orderId = voucherOrderIdDTO.getOrderId();
        Long voucherId = voucherOrderIdDTO.getVoucherId();
        Long userId = voucherOrderIdDTO.getUserId();

        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId)
                .setUserId(userId)
                .setVoucherId(voucherId);

        int success = voucherOrderMapper.insert(voucherOrder);

        // 插入成功，更新库存
        if (success > 0) {
            updateStock(voucherId);
        }
    }

    private void updateStock (Long voucherId) {
        LambdaQueryWrapper<SeckillVoucher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SeckillVoucher::getVoucherId, voucherId);
        SeckillVoucher seckillVoucher = seckillVoucherMapper.selectOne(queryWrapper);
        if (seckillVoucher != null) {
            // 更新库存，通过乐观锁防止超卖
            seckillVoucherMapper.updateWithLock(seckillVoucher);
        }
    }

}




package com.GUET;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.GUET.constants.MQConstants;
import com.GUET.dto.VoucherOrderIdDTO;
import com.GUET.entity.SeckillVoucher;
import com.GUET.entity.Shop;
import com.GUET.entity.VoucherOrder;
import com.GUET.mapper.SeckillVoucherMapper;
import com.GUET.mapper.ShopMapper;
import com.GUET.mapper.VoucherMapper;
import com.GUET.mapper.VoucherOrderMapper;
import com.GUET.service.IShopService;
import com.GUET.utils.MyRedisUtils;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.GUET.constants.MQConstants.CREATE_ORDER_EXCHANGE;
import static com.GUET.constants.RedisConstants.CACHE_SHOP_KEY;
import static com.GUET.constants.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
class GUETShopDianPingApplicationTests {

    @Autowired
    private StringRedisTemplate srt;

    @Autowired
    private MyRedisUtils redisUtils;

    @Autowired
    private IShopService shopService;

    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Autowired
    private VoucherMapper voucherMapper;

    @Autowired
    private VoucherOrderMapper voucherOrderMapper;

    @Autowired
    private SeckillVoucherMapper seckillVoucherMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Test
    public void testSaveShopData () {
        Long id = 1L;
        Shop shop = shopService.getById(1L);
        redisUtils.setWithLogicalExpire(CACHE_SHOP_KEY + id, shop, 20L, TimeUnit.MINUTES);
    }

    @Test
    public void testIdGenerator () throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisUtils.idGenerator("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };

        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }

    @Test
    void testHyperLogLog () {
        String[] values = new String[1000];
        int j;
        for (int i = 0; i < 1000000; i++) {
            j = i % 1000;
            values[j] = "user_" + i;
            if (j == 999) {
                // 发送到Redis
                srt.opsForHyperLogLog().add("hl2", values);
            }
        }
        // 统计数量
        Long count = srt.opsForHyperLogLog().size("hl2");
        System.out.println("count = " + count);
    }

    @Test
    public void testBitSet () {
        int[] array = {3, 8, 5, 7, 1};
        BitSet bitSet = new BitSet(5);

        for (int i = 0; i < array.length; i++) {
            bitSet.set(array[i], true);
        }

        bitSet.stream().forEach(e -> System.out.println(e));
        System.out.println(bitSet.get(10));
    }

    @Test
    public void testBitmap () {
        // 把所有的店铺ID写入Redis的BitMap
        List<Shop> shops = shopMapper.selectList(null);
        for (Shop shop : shops) {
            srt.opsForValue().setBit("shopIds", shop.getId() % 100000, true);
        }
    }

}

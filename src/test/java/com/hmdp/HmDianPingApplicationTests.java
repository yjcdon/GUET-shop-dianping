package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.MyRedisUtils;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.constants.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private StringRedisTemplate srt;

    @Autowired
    private MyRedisUtils redisUtils;

    @Autowired
    private IShopService shopService;

    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Resource
    private RedissonClient redissonClient;

    @Test
    public void testSaveShopData () {
        Long id = 1L;
        Shop shop = shopService.getById(1L);
        redisUtils.setWithLogicalExpire(CACHE_SHOP_KEY + id, shop, 20L, TimeUnit.SECONDS);
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

}

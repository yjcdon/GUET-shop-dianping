package com.guet;

import com.guet.entity.Shop;
import com.guet.mapper.SeckillVoucherMapper;
import com.guet.mapper.ShopMapper;
import com.guet.mapper.VoucherMapper;
import com.guet.mapper.VoucherOrderMapper;
import com.guet.service.IShopService;
import com.guet.utils.MyRedisUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.guet.constants.RedisConstants.CACHE_SHOP_KEY;

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

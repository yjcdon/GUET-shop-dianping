package com.hmdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.constants.MQConstants;
import com.hmdp.dto.VoucherOrderIdDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Shop;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.MyRedisUtils;
import com.hmdp.utils.UserHolderUtil;
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

import static com.hmdp.constants.MQConstants.CREATE_ORDER_EXCHANGE;
import static com.hmdp.constants.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.constants.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
class HmDianPingApplicationTests {

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
    public void saveShopGeo () {
        // 查询店铺信息
        List<Shop> shops = shopMapper.selectList(null);

        // 把店铺按照typeId分组，利用Stream进行分组收集成map
        Map<Long, List<Shop>> map = shops.stream()
                .collect(Collectors.groupingBy(shop -> shop.getTypeId()));

        // 分批写入Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            // typeId
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            // 该类型的Shop
            List<Shop> value = entry.getValue();

            // 写入Redis的geo,GEOADD key 经度 维度 member

            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            for (Shop shop : value) {
                // 这个方法效率不高，会频繁与Redis进行交互
                // srt.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());

                // 这个好
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())));
            }
            srt.opsForGeo().add(key, locations);
        }
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

    @Test
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.CREATE_ORDER_QUEUE, durable = "true"),
            exchange = @Exchange(name = CREATE_ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = {"order"}
    ))
    @Transactional
    public void createVoucherOrder (@RequestBody VoucherOrderIdDTO voucherOrderIdDTO) {
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

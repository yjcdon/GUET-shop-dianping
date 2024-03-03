package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.MyRedisUtils;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Resource
    private RedissonClient redissonClient;

    @Autowired
    private ShopMapper shopMapper;

    @Test
    public void testSaveShopData () {
        Long id = 1L;
        Shop shop = shopService.getById(1L);
        redisUtils.setWithLogicalExpire(CACHE_SHOP_KEY + id, shop, 20L, TimeUnit.SECONDS);
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

}

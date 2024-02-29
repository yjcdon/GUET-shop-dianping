package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    @Bean
    public RedissonClient redissonClient () {
        Config config = new Config();
        // 多节点使用useClusterServer
        config.useSingleServer().setAddress("redis://8.138.88.67:6379");
        return Redisson.create(config);
    }
}

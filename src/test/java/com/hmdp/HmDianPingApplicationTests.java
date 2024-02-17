package com.hmdp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private
    StringRedisTemplate srt = new StringRedisTemplate();

    @Test
    public void test () {
        srt.delete("hello");
    }
}

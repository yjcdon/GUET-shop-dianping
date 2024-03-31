package com.guet.lock;

import cn.hutool.core.lang.UUID;
import com.guet.constants.RedisConstants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class RedisLock implements ILock {

    private StringRedisTemplate srt;

    private String name;

    public RedisLock (StringRedisTemplate srt, String name) {
        this.srt = srt;
        this.name = name;
    }

    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    // 这个静态代码块只会加载一次，减少了IO次数
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("../resources/Lua/unLock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock (long expireTime) {
        String key = RedisConstants.LOCK_KEY + name;
        // value要加线程id
        String threadIdValue = ID_PREFIX + Thread.currentThread().getId();

        Boolean success = srt.opsForValue().setIfAbsent(key, threadIdValue, expireTime, TimeUnit.SECONDS);

        // 防止拆箱时发生空指针
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock () {
        // 调用lua脚本，提前加载脚本
        srt.execute(UNLOCK_SCRIPT,
                Collections.singletonList(RedisConstants.LOCK_KEY + name),
                ID_PREFIX + Thread.currentThread().getId());
    }
    // @Override
    // public void unlock () {
    //     // 获取线程标识，也就是存入Redis的互斥锁的value
    //     String threadIdValue = ID_PREFIX + Thread.currentThread().getId();
    //     // 获取锁的标识，根据key获取锁的value
    //     String redisLockValue = srt.opsForValue().get(RedisConstants.LOCK_KEY + name);
    //
    //     // 锁一样才能删除
    //     if (threadIdValue.equals(redisLockValue)) {
    //         srt.delete(RedisConstants.LOCK_KEY + name);
    //     }
    // }
}

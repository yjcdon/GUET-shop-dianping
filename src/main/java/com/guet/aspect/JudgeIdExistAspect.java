package com.guet.aspect;

import com.guet.annotation.JudgeIdExist;
import com.guet.result.Result;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class JudgeIdExistAspect {

    @Autowired
    private StringRedisTemplate srt;

    @Pointcut("execution(* com.guet.service.impl.*.*(..)) && @annotation(com.guet.annotation.JudgeIdExist)")
    public void judge () {
    }

    @Around("judge()")
    public Object judgeExist (ProceedingJoinPoint joinPoint) throws Throwable {
        // 方法参数
        Object[] ids = joinPoint.getArgs();
        if (ids == null || ids.length == 0) {
            return null;
        }

        // 获取方法的注解对象
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        JudgeIdExist annotation = signature.getMethod().getAnnotation(JudgeIdExist.class);
        // 获取注解中传入的key
        String key = annotation.key();
        Long id = (Long) ids[0];

        if (id < 0) {
            return Result.fail("id不存在");
        }

        // 判断BitMap中是否存在ID
        int hashVal = Math.abs(id.hashCode());
        Boolean isExist = srt.opsForValue().getBit(key, hashVal % 100000);
        if (!Boolean.TRUE.equals(isExist)) {
            return Result.fail("id不存在");
        }

        // 执行原本的方法
        return joinPoint.proceed();
    }
}

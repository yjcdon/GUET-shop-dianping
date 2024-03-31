package com.guet.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 判断id是否存在于BitMap中，
// 但是有挺多不足的，需要提前将指定的ID存入BitMap中才能实现，否则会报错
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JudgeIdExist {
    // 相关业务的key
    String key ();
}

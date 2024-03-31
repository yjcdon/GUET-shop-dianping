package com.guet.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @Author: 梁雨佳
 * @Date: 2024/2/17 16:17:21
 * @Description: 各种转换方法
 */
public class MyConvertUtils {

    /**
     * @Author: 梁雨佳
     * @Date: 2024/2/18 10:26:14
     * @Description: 将对象转换为map
     */
    public static Map<String, Object> objectToMap (Object obj) throws Exception {

        Map<String, Object> map = new HashMap<>();

        Class<?> clazz = obj.getClass();

        // 获取所有属性和值，并put到map中
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            map.put(field.getName(), field.get(obj).toString());
        }
        return map;
    }

    /**
     * @Author: 梁雨佳
     * @Date: 2024/2/18 10:26:27
     * @Description: 将对象List转换为StringList，主要用于Redis的pushAll方法
     * 传入对象List，传入转换为什么样的String方法，最后返回
     */
    public static <T> List<String> objectListToStringList (List<T> objectList, Function<T, String> toJsonString) {
        List<String> result = new ArrayList<>(objectList.size());
        for (T object : objectList) {
            // 应用传入的转换方法并返回
            result.add(toJsonString.apply(object));
        }
        return result;
    }
}

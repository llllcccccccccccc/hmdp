package com.hmdp.utils;

import com.alibaba.druid.sql.visitor.functions.Now;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    /**
     * 开始时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    private static final int CONUT_BITS = 32;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public long nextId(String keyPrefix) {
        //1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        //2.生成序列化号
        //2.1获取当前日期，精确到天
        String date = DateTimeFormatter.ofPattern("yyyy:MM:dd").format(LocalDateTime.now());
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        //3.拼接并返回
        return timestamp << CONUT_BITS | count;
    }

    public static void main(String[] args) {
        long a = 111;

        System.out.println(a << 32);
    }


}

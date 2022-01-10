package com.atguigu.gmall.index;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

//@SpringBootTest
class GmallIndexApplicationTests {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Test
    void contextLoads() {
        this.redisTemplate.opsForValue().set("name","柳岩");
        System.out.println(this.redisTemplate.opsForValue().get("name"));
    }

    BloomFilter<String> bloomFilter;

    @BeforeEach
    void init(){

    }

    @Test
    void contextLoad(){
        // 1-准备填充的数据总量  2-失败率
        BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8),10,0.3);
        bloomFilter.put("1");
        bloomFilter.put("2");
        bloomFilter.put("3");
        bloomFilter.put("4");
        bloomFilter.put("5");
        System.out.println(bloomFilter.mightContain("1"));
        System.out.println(bloomFilter.mightContain("2"));
        System.out.println(bloomFilter.mightContain("3"));
        System.out.println(bloomFilter.mightContain("4"));
        System.out.println(bloomFilter.mightContain("5"));
        System.out.println(bloomFilter.mightContain("6"));
        System.out.println(bloomFilter.mightContain("7"));
        System.out.println(bloomFilter.mightContain("8"));
        System.out.println(bloomFilter.mightContain("9"));
        System.out.println(bloomFilter.mightContain("10"));
        System.out.println(bloomFilter.mightContain("11"));
        System.out.println(bloomFilter.mightContain("12"));
        System.out.println(bloomFilter.mightContain("13"));
        System.out.println(bloomFilter.mightContain("14"));
        System.out.println(bloomFilter.mightContain("15"));
        System.out.println(bloomFilter.mightContain("16"));
    }

    @Test
    void testRedissonBloom(){
        RBloomFilter<String> bloom = this.redissonClient.getBloomFilter("bloom");
        bloom.tryInit(16l, 0.3);
        bloom.add("1");
        bloom.add("2");
        bloom.add("3");
        bloom.add("4");
        bloom.add("5");
        System.out.println(bloom.contains("1"));
        System.out.println(bloom.contains("3"));
        System.out.println(bloom.contains("5"));
        System.out.println(bloom.contains("6"));
        System.out.println(bloom.contains("7"));
        System.out.println(bloom.contains("8"));
        System.out.println(bloom.contains("9"));
        System.out.println(bloom.contains("10"));
        System.out.println(bloom.contains("11"));
        System.out.println(bloom.contains("12"));
        System.out.println(bloom.contains("13"));
        System.out.println(bloom.contains("14"));
        System.out.println(bloom.contains("15"));
        System.out.println(bloom.contains("16"));
    }


    @Test
    public void test1(){
        String a = "aaa";
        String b = a;
        System.out.println(b);
        b = "bbb";
        System.out.println(b);
        System.out.println(a);
    }
}

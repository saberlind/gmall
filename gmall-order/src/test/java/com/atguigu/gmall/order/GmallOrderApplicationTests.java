package com.atguigu.gmall.order;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.junit.jupiter.api.Test;

//@SpringBootTest
class GmallOrderApplicationTests {

    @Test
    void contextLoads() {
        String timeId = IdWorker.getTimeId();
        System.out.println(timeId);
    }

}

package com.mann.cvreview;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CvreviewApplicationTests {

    @MockitoBean
    private RedisClient redisClient;

    @MockitoBean
    private LettuceBasedProxyManager<byte[]> proxyManager;

    @Test
    void contextLoads() {
    }
}

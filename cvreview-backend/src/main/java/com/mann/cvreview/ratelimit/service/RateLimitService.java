package com.mann.cvreview.ratelimit.service;

import com.mann.cvreview.ratelimit.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    // Redis proxy manager for distributed rate limit buckets
    private final LettuceBasedProxyManager<byte[]> proxyManager;

    //constructor
    public RateLimitService(LettuceBasedProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
    }

    public void checkLimit(String clientKey) {

        // Configure bucket: max 3 tokens - give 3 tokens a day
        BucketConfiguration config = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder().capacity(3).refillIntervally(3, Duration.ofDays(1)).build())
                .build();

        //check user tokens in redis
        var bucket = proxyManager.builder().build(clientKey.getBytes(), () -> config);

        //delete 1 token
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Daily analysis limit reached, please try again tomorrow");
        }
    }
}
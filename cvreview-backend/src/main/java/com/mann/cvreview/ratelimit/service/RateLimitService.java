package com.mann.cvreview.ratelimit.service;

import com.mann.cvreview.ratelimit.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    // Redis proxy manager for distributed rate limit buckets
    private final LettuceBasedProxyManager<byte[]> proxyManager;

    // Inject Lettuce-based proxy manager via constructor
    public RateLimitService(LettuceBasedProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
    }

    public void checkLimit(String clientKey) {

        // Configure bucket: allow 3 requests per day
        BucketConfiguration config = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(3, Refill.intervally(3, Duration.ofDays(1))))
                .build();

        // Resolve or create bucket for this client key in Redis
        var bucket = proxyManager.builder().build(clientKey.getBytes(), config);

        // Consume 1 token; throw exception if daily limit is exceeded
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Daily analysis limit reached, please try again tomorrow");
        }
    }
}
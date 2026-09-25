package com.mann.cvreview.ratelimit;

import com.mann.cvreview.ratelimit.exception.RateLimitExceededException;
import com.mann.cvreview.ratelimit.service.RateLimitService;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private LettuceBasedProxyManager<byte[]> proxyManager;

    @Mock
    private RemoteBucketBuilder<byte[]> remoteBucketBuilder;

    @Mock
    private BucketProxy bucketProxy;

    @InjectMocks
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("checkLimit allows request when token is available")
    @SuppressWarnings("unchecked")
    void checkLimit_tokenAvailable_doesNotThrow() {
        given(proxyManager.builder()).willReturn(remoteBucketBuilder);
        given(remoteBucketBuilder.build(any(byte[].class), any(Supplier.class))).willReturn(bucketProxy);
        given(bucketProxy.tryConsume(1)).willReturn(true);

        assertDoesNotThrow(() -> rateLimitService.checkLimit("127.0.0.1"));
    }

    @Test
    @DisplayName("checkLimit throws RateLimitExceededException when tokens exhausted")
    @SuppressWarnings("unchecked")
    void checkLimit_tokensExhausted_throwsRateLimitExceededException() {
        given(proxyManager.builder()).willReturn(remoteBucketBuilder);
        given(remoteBucketBuilder.build(any(byte[].class), any(Supplier.class))).willReturn(bucketProxy);
        given(bucketProxy.tryConsume(1)).willReturn(false);

        assertThrows(RateLimitExceededException.class, () -> rateLimitService.checkLimit("127.0.0.1"));
    }
}

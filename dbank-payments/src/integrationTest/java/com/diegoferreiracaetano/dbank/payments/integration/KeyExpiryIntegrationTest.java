package com.diegoferreiracaetano.dbank.payments.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "dbank.idempotency.cache.ttl=2s")
class KeyExpiryIntegrationTest extends TransferApiSupport {

    @Autowired
    StringRedisTemplate redis;

    @Test
    void givenExpiredCacheEntry_whenReplaying_thenShouldAnswerFromTheDatabaseAndCacheAgain() throws Exception {
        var key = newKey();
        var first = idOf(postTransfer(key, 100).andReturn());
        postTransfer(key, 100).andExpect(status().isAccepted());
        var cacheKey = "idempotency:" + key;
        assertThat(redis.hasKey(cacheKey)).isTrue();

        await().atMost(10, TimeUnit.SECONDS).until(() -> !Boolean.TRUE.equals(redis.hasKey(cacheKey)));

        postTransfer(key, 100)
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(first));
        assertThat(redis.hasKey(cacheKey)).isTrue();
    }
}

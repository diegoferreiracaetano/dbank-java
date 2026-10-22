package com.diegoferreiracaetano.dbank.payments.integration;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class RedisUnavailableIntegrationTest extends TransferApiSupport {

    @MockitoBean
    StringRedisTemplate redis;

    @BeforeEach
    void redisIsDown() {
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("redis is down"));
    }

    @Test
    void givenRedisDown_whenPosting_thenShouldStillAcceptTheTransfer() throws Exception {
        postTransfer(newKey(), 100)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void givenRedisDown_whenReplaying_thenShouldStillReplayFromTheDatabase() throws Exception {
        var key = newKey();
        var first = idOf(postTransfer(key, 100).andReturn());

        postTransfer(key, 100)
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(first));
    }

    @Test
    void givenRedisDown_whenReusingTheKeyWithADifferentBody_thenShouldStillRejectIt() throws Exception {
        var key = newKey();
        postTransfer(key, 100).andExpect(status().isAccepted());

        postTransfer(key, 200).andExpect(status().isUnprocessableEntity());
    }
}

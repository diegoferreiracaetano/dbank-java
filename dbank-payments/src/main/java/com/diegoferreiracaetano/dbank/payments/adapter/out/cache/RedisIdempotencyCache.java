package com.diegoferreiracaetano.dbank.payments.adapter.out.cache;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyRecord;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.RequestFingerprint;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
class RedisIdempotencyCache implements IdempotencyCache {

    private static final String PREFIX = "idempotency:";

    private final StringRedisTemplate redis;
    private final Duration ttl;

    RedisIdempotencyCache(StringRedisTemplate redis, @Value("${dbank.idempotency.cache.ttl:24h}") Duration ttl) {
        this.redis = redis;
        this.ttl = ttl;
    }

    @Override
    public Optional<IdempotencyRecord> find(IdempotencyKey key) {
        var value = redis.opsForValue().get(PREFIX + key.value());
        return Optional.ofNullable(value).map(stored -> decode(key, stored));
    }

    @Override
    public void put(IdempotencyRecord record) {
        redis.opsForValue().setIfAbsent(PREFIX + record.key().value(), encode(record), ttl);
    }

    private static String encode(IdempotencyRecord record) {
        return record.fingerprint().value() + "|" + record.transferId().value();
    }

    private static IdempotencyRecord decode(IdempotencyKey key, String stored) {
        var parts = stored.split("\\|", 2);
        return new IdempotencyRecord(key, new RequestFingerprint(parts[0]), new TransferId(UUID.fromString(parts[1])));
    }
}

package com.diegoferreiracaetano.dbank.payments.adapter.out.persistence.idempotency;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataIdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, String> {

    @Modifying(flushAutomatically = true)
    @Query(value = """
                    INSERT INTO idempotency_key (key, fingerprint, transfer_id)
                    VALUES (:key, :fingerprint, :transferId)
                    ON CONFLICT (key) DO NOTHING
                    """, nativeQuery = true)
    int insertIfAbsent(
            @Param("key") String key, @Param("fingerprint") String fingerprint, @Param("transferId") UUID transferId);
}

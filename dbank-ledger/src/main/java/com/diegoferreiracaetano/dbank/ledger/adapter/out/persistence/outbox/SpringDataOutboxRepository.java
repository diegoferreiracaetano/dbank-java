package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataOutboxRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(
            value = "SELECT * FROM outbox_event WHERE sent_at IS NULL ORDER BY created_at LIMIT :limit"
                    + " FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEventEntity> lockPending(@Param("limit") int limit);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE OutboxEventEntity e SET e.sentAt = :sentAt WHERE e.id IN :ids")
    void markSent(@Param("ids") List<UUID> ids, @Param("sentAt") Instant sentAt);
}

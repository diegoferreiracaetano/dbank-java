package com.diegoferreiracaetano.dbank.notifications.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    @Modifying(flushAutomatically = true)
    @Query(
            value = "INSERT INTO notification (event_id, transfer_id, account_id, message)"
                    + " VALUES (:eventId, :transferId, :accountId, :message) ON CONFLICT (event_id) DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") UUID eventId,
            @Param("transferId") UUID transferId,
            @Param("accountId") UUID accountId,
            @Param("message") String message);
}

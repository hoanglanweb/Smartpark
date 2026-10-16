package com.example.SmartPark.repository;

import com.example.SmartPark.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @EntityGraph(attributePaths = {"slot", "user", "vehicle"})
    List<Reservation> findByUserIdOrderByStartTimeDesc(Long userId);
    List<Reservation> findByStatus(Reservation.Status status);

    @EntityGraph(attributePaths = {"slot", "user", "vehicle"})
    List<Reservation> findTop20ByOrderByCreatedAtDesc();

    @Query("""
            select distinct reservation.slot.id
            from Reservation reservation
            where reservation.slot.id in :slotIds
              and reservation.status in :statuses
              and reservation.startTime < :newEnd
              and reservation.endTime > :newStart
            """)
    List<Long> findReservedSlotIds(
            @Param("slotIds") Collection<Long> slotIds,
            @Param("statuses") Collection<Reservation.Status> statuses,
            @Param("newEnd") LocalDateTime newEnd,
            @Param("newStart") LocalDateTime newStart);

    //?: Trùng giờ khi: bắt đầu cũ < kết thúc mới VÀ kết thúc cũ > bắt đầu mới
    boolean existsBySlotIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            Long slotId,
            Collection<Reservation.Status> statuses,
            LocalDateTime newEnd,
            LocalDateTime newStart);
}
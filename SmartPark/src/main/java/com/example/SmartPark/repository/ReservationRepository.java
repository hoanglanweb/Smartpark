package com.example.SmartPark.repository;

import com.example.SmartPark.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserIdOrderByStartTimeDesc(Long userId);
    List<Reservation> findByStatus(Reservation.Status status);

    //?: Trùng giờ khi: bắt đầu cũ < kết thúc mới VÀ kết thúc cũ > bắt đầu mới
    boolean existsBySlotIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            Long slotId,
            Collection<Reservation.Status> statuses,
            LocalDateTime newEnd,
            LocalDateTime newStart);
}
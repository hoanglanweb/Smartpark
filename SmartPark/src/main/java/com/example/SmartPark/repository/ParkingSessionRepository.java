package com.example.SmartPark.repository;

import com.example.SmartPark.model.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
    Optional<ParkingSession> findByVehicleIdAndStatus(Long vehicleId, ParkingSession.Status status);
    Optional<ParkingSession> findBySlotIdAndStatus(Long slotId, ParkingSession.Status status);
    List<ParkingSession> findByStatus(ParkingSession.Status status);
    List<ParkingSession> findByVehicleIdOrderByCheckInTimeDesc(Long vehicleId);
}
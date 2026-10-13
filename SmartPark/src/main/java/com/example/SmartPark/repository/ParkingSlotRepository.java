package com.example.SmartPark.repository;

import com.example.SmartPark.model.ParkingSlot;
import com.example.SmartPark.model.ParkingSlot.SlotStatus;
import com.example.SmartPark.model.ParkingSlot.SlotType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSlotRepository
        extends JpaRepository<ParkingSlot, Long> {

    // Tìm ô theo mã: A01, B01...
    Optional<ParkingSlot> findByCode(String code);

    // Lấy toàn bộ ô theo khu A hoặc B
    List<ParkingSlot> findByZone(String zone);

    // Lấy ô theo trạng thái
    List<ParkingSlot> findByStatus(SlotStatus status);

    // Lấy ô theo loại: STANDARD, VIP, PRIORITY, CAR
    List<ParkingSlot> findBySlotType(SlotType slotType);

    // Lọc theo khu và trạng thái
    List<ParkingSlot> findByZoneAndStatus(
            String zone,
            SlotStatus status);

    // Lọc theo khu và dãy, ví dụ B_MIDDLE hoặc B_LOWER
    List<ParkingSlot> findByZoneAndRowName(
            String zone,
            String rowName);

    // Lọc theo khu, dãy và trạng thái
    List<ParkingSlot> findByZoneAndRowNameAndStatus(
            String zone,
            String rowName,
            SlotStatus status);

    // Đếm số ô theo trạng thái
    long countByStatus(SlotStatus status);

    // Đếm tổng ô theo khu
    long countByZone(String zone);

    // Đếm ô theo khu và loại
    long countByZoneAndSlotType(String zone, SlotType slotType);
}
package com.example.SmartPark.service;

import com.example.SmartPark.model.ParkingSession;
import com.example.SmartPark.model.ParkingSlot;
import com.example.SmartPark.model.Vehicle;
import com.example.SmartPark.repository.ParkingSessionRepository;
import com.example.SmartPark.repository.ParkingSlotRepository;
import com.example.SmartPark.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class ParkingSessionService {

    private final ParkingSessionRepository sessionRepository;
    private final ParkingSlotRepository slotRepository;
    private final VehicleRepository vehicleRepository;

    public ParkingSessionService(
            ParkingSessionRepository sessionRepository,
            ParkingSlotRepository slotRepository,
            VehicleRepository vehicleRepository) {
        this.sessionRepository = sessionRepository;
        this.slotRepository = slotRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public ParkingSession checkIn(Long slotId, String plateNumber, Vehicle.VehicleType vehicleType) {
        String normalizedPlate = plateNumber == null
                ? ""
                : plateNumber.trim().toUpperCase(Locale.ROOT);
        if (normalizedPlate.isBlank() || normalizedPlate.length() > 20) {
            throw new IllegalArgumentException("Biển số xe là bắt buộc và không vượt quá 20 ký tự");
        }
        if (vehicleType == null) {
            throw new IllegalArgumentException("Cần chọn loại phương tiện");
        }
        if (slotId == null || slotId <= 0) {
            throw new IllegalArgumentException("Cần chọn một vị trí đỗ hợp lệ");
        }

        ParkingSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vị trí đỗ"));
        if (slot.getStatus() != ParkingSlot.SlotStatus.AVAILABLE) {
            throw new IllegalStateException("Vị trí " + slot.getCode() + " hiện không còn trống");
        }

        Vehicle vehicle = vehicleRepository.findByPlateNumber(normalizedPlate)
                .orElseGet(() -> {
                    Vehicle newVehicle = new Vehicle();
                    newVehicle.setPlateNumber(normalizedPlate);
                    newVehicle.setVehicleType(vehicleType);
                    return vehicleRepository.save(newVehicle);
                });
        if (vehicle.getVehicleType() != vehicleType) {
            throw new IllegalArgumentException("Loại phương tiện không khớp với biển số xe đã lưu");
        }
        if (sessionRepository.findByVehicleIdAndStatus(vehicle.getId(), ParkingSession.Status.ACTIVE).isPresent()) {
            throw new IllegalStateException("Xe " + normalizedPlate + " đã có phiên đỗ đang hoạt động");
        }

        ParkingSession session = new ParkingSession();
        session.setVehicle(vehicle);
        session.setSlot(slot);
        session.setCheckInTime(LocalDateTime.now());
        session.setStatus(ParkingSession.Status.ACTIVE);
        slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
        slotRepository.save(slot);
        return sessionRepository.save(session);
    }

    @Transactional
    public ParkingSession checkOut(Long slotId) {
        if (slotId == null || slotId <= 0) {
            throw new IllegalArgumentException("Cần chọn một vị trí đỗ hợp lệ");
        }

        ParkingSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vị trí đỗ"));
        if (slot.getStatus() != ParkingSlot.SlotStatus.OCCUPIED) {
            throw new IllegalStateException("Vị trí " + slot.getCode() + " hiện không có xe");
        }

        ParkingSession session = sessionRepository
                .findBySlotIdAndStatus(slotId, ParkingSession.Status.ACTIVE)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy phiên đỗ đang hoạt động tại vị trí " + slot.getCode()));
        session.setCheckOutTime(LocalDateTime.now());
        session.setStatus(ParkingSession.Status.FINISHED);
        slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
        slotRepository.save(slot);
        return sessionRepository.save(session);
    }
}

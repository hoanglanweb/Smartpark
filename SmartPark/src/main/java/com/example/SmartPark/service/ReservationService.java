package com.example.SmartPark.service;

import com.example.SmartPark.model.*;
import com.example.SmartPark.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepo;
    private final ParkingSlotRepository slotRepo;
    private final UserRepository userRepo;
    private final VehicleRepository vehicleRepo;
    private final ParkingSlotService slotService;

    //?: Khởi tạo ReservationService với các repository cần thiết. */
    public ReservationService(ReservationRepository reservationRepo,
            ParkingSlotRepository slotRepo,
            UserRepository userRepo,
            VehicleRepository vehicleRepo,
            ParkingSlotService slotService) {
        this.reservationRepo = reservationRepo;
        this.slotRepo = slotRepo;
        this.userRepo = userRepo;
        this.vehicleRepo = vehicleRepo;
        this.slotService = slotService;
    }

    //?: Tạo mới một đơn đặt chỗ ô đỗ xe.
    @Transactional
    public Reservation create(Long userId, Long vehicleId, Long slotId,
            LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (date == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Ngày và thời gian đặt chỗ là bắt buộc");
        }
        return create(userId, vehicleId, slotId, date.atTime(startTime), date.atTime(endTime));
    }

    @Transactional
    public Reservation createGuest(String fullName, String phone, String plateNumber,
            Vehicle.VehicleType vehicleType, Long slotId,
            LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (date == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Ngày và thời gian đặt chỗ là bắt buộc");
        }
        return createGuest(fullName, phone, plateNumber, vehicleType, slotId,
                date.atTime(startTime), date.atTime(endTime));
    }

    @Transactional
    public Reservation createGuest(String fullName, String phone, String plateNumber,
            Vehicle.VehicleType vehicleType, Long slotId,
            LocalDateTime start, LocalDateTime end) {
        String normalizedName = fullName == null ? "" : fullName.trim();
        String normalizedPhone = phone == null ? "" : phone.trim();
        String normalizedPlate = plateNumber == null ? "" : plateNumber.trim().toUpperCase(Locale.ROOT);
        if (normalizedName.isBlank() || normalizedName.length() > 100) {
            throw new IllegalArgumentException("Họ tên là bắt buộc và không vượt quá 100 ký tự");
        }
        if (!normalizedPhone.matches("[+0-9() .-]{7,20}")
                || normalizedPhone.replaceAll("[^0-9]", "").length() < 7) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }
        if (normalizedPlate.isBlank() || normalizedPlate.length() > 20) {
            throw new IllegalArgumentException("Biển số xe là bắt buộc và không vượt quá 20 ký tự");
        }
        if (vehicleType == null) {
            throw new IllegalArgumentException("Cần chọn loại phương tiện");
        }
        slotService.validateTimeWindow(start, end);
        if (slotId == null || slotId <= 0) {
            throw new IllegalArgumentException("Cần chọn một vị trí đỗ hợp lệ");
        }

        ParkingSlot slot = slotRepo.findByIdForUpdate(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vị trí đỗ"));
        if (slot.getStatus() != ParkingSlot.SlotStatus.AVAILABLE) {
            throw new IllegalStateException("Ô " + slot.getCode() + " hiện không còn trống");
        }
        if (slotService.isReserved(slotId, start, end)) {
            throw new IllegalStateException("Ô " + slot.getCode() + " đã được giữ trong khoảng thời gian này");
        }

        Vehicle vehicle = vehicleRepo.findByPlateNumber(normalizedPlate)
                .orElseGet(() -> {
                    Vehicle newVehicle = new Vehicle();
                    newVehicle.setPlateNumber(normalizedPlate);
                    newVehicle.setVehicleType(vehicleType);
                    return vehicleRepo.save(newVehicle);
                });
        if (vehicle.getVehicleType() != vehicleType) {
            throw new IllegalArgumentException("Loại phương tiện không khớp với biển số xe đã lưu");
        }

        Reservation reservation = new Reservation();
        reservation.setGuestName(normalizedName);
        reservation.setGuestPhone(normalizedPhone);
        reservation.setVehicle(vehicle);
        reservation.setSlot(slot);
        reservation.setStartTime(start);
        reservation.setEndTime(end);
        reservation.setStatus(Reservation.Status.PENDING);
        return reservationRepo.save(reservation);
    }

    @Transactional
    public Reservation create(Long userId, Long vehicleId, Long slotId,
            LocalDateTime start, LocalDateTime end) {
        slotService.validateTimeWindow(start, end);
        if (userId == null || vehicleId == null || slotId == null
                || userId <= 0 || vehicleId <= 0 || slotId <= 0) {
            throw new IllegalArgumentException("Người dùng, xe và vị trí đỗ là bắt buộc");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        Vehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe"));
        if (vehicle.getOwner() != null && !vehicle.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Xe đã chọn không thuộc người dùng này");
        }
        ParkingSlot slot = slotRepo.findByIdForUpdate(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ô đỗ"));

        if (slot.getStatus() != ParkingSlot.SlotStatus.AVAILABLE) {
            throw new IllegalStateException("Ô " + slot.getCode() + " hiện không còn trống");
        }

        if (slotService.isReserved(slotId, start, end))
            throw new IllegalStateException("Ô " + slot.getCode() + " đã được giữ trong khoảng thời gian này");

        Reservation r = new Reservation();
        r.setUser(user);
        r.setVehicle(vehicle);
        r.setSlot(slot);
        r.setStartTime(start);
        r.setEndTime(end);
        r.setStatus(Reservation.Status.PENDING);

        return reservationRepo.save(r);
    }

    @Transactional
    public Reservation confirm(Long id) {
        Reservation r = find(id);
        if (r.getStatus() != Reservation.Status.PENDING)
            throw new IllegalStateException("Chỉ xác nhận được đặt chỗ đang chờ");
        r.setStatus(Reservation.Status.CONFIRMED);
        return reservationRepo.save(r);
    }

    @Transactional
    public Reservation cancel(Long id) {
        Reservation r = find(id);
        if (r.getStatus() == Reservation.Status.COMPLETED
                || r.getStatus() == Reservation.Status.CANCELLED)
            throw new IllegalStateException("Đặt chỗ này không thể hủy");
        r.setStatus(Reservation.Status.CANCELLED);
        return reservationRepo.save(r);
    }

    @Transactional(readOnly = true)
    public List<Reservation> getByUser(Long userId) {
        return reservationRepo.findByUserIdOrderByStartTimeDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Reservation> getRecent() {
        return reservationRepo.findTop20ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<User> getUsers() {
        return userRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getVehicles() {
        return vehicleRepo.findAll();
    }

    private Reservation find(Long id) {
        return reservationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt chỗ id=" + id));
    }
}
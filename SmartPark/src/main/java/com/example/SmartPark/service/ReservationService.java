
package com.example.SmartPark.service;

import com.example.SmartPark.model.*;
import com.example.SmartPark.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepo;
    private final ParkingSlotRepository slotRepo;
    private final UserRepository userRepo;
    private final VehicleRepository vehicleRepo;
    private final ParkingSlotService slotService;

    public ReservationService(
            ReservationRepository reservationRepo,
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

    /**
     * Tạo đặt chỗ bằng ngày và giờ.
     * Chỉ gọi sau khi controller xác định được người dùng đã đăng nhập.
     */
    @Transactional
    public Reservation create(
            Long userId,
            Long vehicleId,
            Long slotId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime) {

        if (date == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                    "Ngày và thời gian đặt chỗ là bắt buộc");
        }

        return create(
                userId,
                vehicleId,
                slotId,
                date.atTime(startTime),
                date.atTime(endTime));
    }

    @Transactional
    public Reservation createGuest(
            String fullName,
            String phone,
            String plateNumber,
            Vehicle.VehicleType vehicleType,
            Long slotId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime) {

        if (date == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Ngày và thời gian đặt chỗ là bắt buộc");
        }

        return createGuest(
                fullName,
                phone,
                plateNumber,
                vehicleType,
                slotId,
                date.atTime(startTime),
                date.atTime(endTime));
    }

    @Transactional
    public Reservation createGuest(
            String fullName,
            String phone,
            String plateNumber,
            Vehicle.VehicleType vehicleType,
            Long slotId,
            LocalDateTime start,
            LocalDateTime end) {

        String normalizedName = fullName == null ? "" : fullName.trim();
        String normalizedPhone = phone == null ? "" : phone.trim();
        String normalizedPlate = plateNumber == null
                ? ""
                : plateNumber.trim().toUpperCase(Locale.ROOT);
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
        if (slotId == null || slotId <= 0) {
            throw new IllegalArgumentException("Cần chọn một vị trí đỗ hợp lệ");
        }

        slotService.validateTimeWindow(start, end);

        ParkingSlot slot = slotRepo.findByIdForUpdate(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vị trí đỗ"));
        if (slot.getStatus() != ParkingSlot.SlotStatus.AVAILABLE) {
            throw new IllegalStateException("Ô " + slot.getCode() + " hiện không còn trống");
        }
        if (slotService.isReserved(slotId, start, end)) {
            throw new IllegalStateException("Ô " + slot.getCode()
                    + " đã được giữ trong khoảng thời gian này");
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

    /**
     * Tạo đặt chỗ bằng thời gian đầy đủ.
     */
    @Transactional
    public Reservation create(
            Long userId,
            Long vehicleId,
            Long slotId,
            LocalDateTime start,
            LocalDateTime end) {

        if (userId == null || userId <= 0
                || vehicleId == null || vehicleId <= 0
                || slotId == null || slotId <= 0) {
            throw new IllegalArgumentException(
                    "Người dùng, xe và vị trí đỗ là bắt buộc");
        }

        slotService.validateTimeWindow(start, end);

        // Tìm người dùng
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy người dùng"));

        // Tìm xe
        Vehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy xe"));

        // Chỉ cho phép dùng xe thuộc chính tài khoản này
        if (vehicle.getOwner() == null
                || vehicle.getOwner().getId() == null
                || !vehicle.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "Xe đã chọn không thuộc tài khoản của bạn");
        }

        // Khóa và tìm vị trí đỗ
        ParkingSlot slot = slotRepo.findByIdForUpdate(slotId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy vị trí đỗ"));

        // Kiểm tra trạng thái vị trí
        if (slot.getStatus() != ParkingSlot.SlotStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Ô " + slot.getCode() + " hiện không còn trống");
        }

        // Kiểm tra đặt chỗ bị trùng thời gian
        if (slotService.isReserved(slotId, start, end)) {
            throw new IllegalStateException(
                    "Ô " + slot.getCode()
                            + " đã được giữ trong khoảng thời gian này");
        }

        // Tạo đặt chỗ
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setVehicle(vehicle);
        reservation.setSlot(slot);
        reservation.setStartTime(start);
        reservation.setEndTime(end);
        reservation.setStatus(Reservation.Status.PENDING);

        return reservationRepo.save(reservation);
    }

    /**
     * Xác nhận một đặt chỗ đang chờ.
     */
    @Transactional
    public Reservation confirm(Long id) {
        Reservation reservation = find(id);

        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new IllegalStateException(
                    "Chỉ xác nhận được đặt chỗ đang chờ");
        }

        reservation.setStatus(Reservation.Status.CONFIRMED);
        return reservationRepo.save(reservation);
    }

    /**
     * Hủy đặt chỗ chưa hoàn tất.
     */
    @Transactional
    public Reservation cancel(Long id) {
        Reservation reservation = find(id);

        if (reservation.getStatus() == Reservation.Status.COMPLETED
                || reservation.getStatus() == Reservation.Status.CANCELLED) {
            throw new IllegalStateException(
                    "Đặt chỗ này không thể hủy");
        }

        reservation.setStatus(Reservation.Status.CANCELLED);
        return reservationRepo.save(reservation);
    }

    /**
     * Lấy danh sách đặt chỗ của người dùng.
     */
    @Transactional(readOnly = true)
    public List<Reservation> getByUser(Long userId) {
        return reservationRepo.findByUserIdOrderByStartTimeDesc(userId);
    }

    /**
     * Lấy 20 đặt chỗ gần đây nhất.
     */
    @Transactional(readOnly = true)
    public List<Reservation> getRecent() {
        return reservationRepo.findTop20ByOrderByCreatedAtDesc();
    }

    /**
     * Lấy danh sách người dùng.
     */
    @Transactional(readOnly = true)
    public List<User> getUsers() {
        return userRepo.findAll();
    }

    /**
     * Lấy danh sách xe.
     */
    @Transactional(readOnly = true)
    public List<Vehicle> getVehicles() {
        return vehicleRepo.findAll();
    }

    /**
     * Tìm đặt chỗ theo ID.
     */
    private Reservation find(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "ID đặt chỗ không hợp lệ");
        }

        return reservationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy đặt chỗ id=" + id));
    }
}

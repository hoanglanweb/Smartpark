package com.example.SmartPark.service;

import com.example.SmartPark.model.*;
import com.example.SmartPark.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
            LocalDateTime start, LocalDateTime end) {
        //!: kiểm tra tính hợp lệ của thời gian đặt chỗ
        //!: isAfter(): kiểm tra mốc thời này có sau mốc thời gian kia ko  
        if (start == null || end == null || !end.isAfter(start))
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");

        //!: isBefore(): Kiểm tra mốc thời gian này có trước mốc thời gian kia không
        //!: minusMinutes(1): Trừ đi 1 phút (bù trừ độ trễ mạng)
        if (start.isBefore(LocalDateTime.now().minusMinutes(1)))
            throw new IllegalArgumentException("Không thể đặt chỗ trong quá khứ");

        //!: Tìm và kiếm tra các thực thể trong database (Ném lỗi nếu ko thấy)
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        Vehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe"));
        ParkingSlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ô đỗ"));

        if (slot.getStatus() == ParkingSlot.SlotStatus.MAINTENANCE)
            throw new IllegalStateException("Ô " + slot.getCode() + " đang bảo trì");

        //?: Chặn đặt trùng vị trí trong cùng khoảng thời gian
        if (slotService.isReserved(slotId, start, end))
            throw new IllegalStateException("Ô " + slot.getCode() + " đã được giữ trong khoảng thời gian này");
        
        //?; Khởi tạo đơn đặt chỗ mới với trạng thái PENDING (Chờ xử lý)
        Reservation r = new Reservation();
        r.setUser(user);
        r.setVehicle(vehicle);
        r.setSlot(slot);
        r.setStartTime(start);
        r.setEndTime(end);
        r.setStatus(Reservation.Status.PENDING);

        //!: lưu trữ và trả về dối tượng 
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

    public List<Reservation> getByUser(Long userId) {
        return reservationRepo.findByUserIdOrderByStartTimeDesc(userId);
    }

    private Reservation find(Long id) {
        return reservationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt chỗ id=" + id));
    }
}
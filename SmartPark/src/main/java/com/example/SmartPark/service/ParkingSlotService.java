package com.example.SmartPark.service;

import com.example.SmartPark.model.ParkingSlot;
import com.example.SmartPark.model.ParkingSlot.SlotStatus;
import com.example.SmartPark.model.ParkingSlot.SlotType;
import com.example.SmartPark.model.Reservation;
import com.example.SmartPark.repository.ParkingSlotRepository;
import com.example.SmartPark.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParkingSlotService {

    // ?:** Thống kê hiển thị ở khung "THỐNG KÊ BÃI ĐỖ" */
    public record Stats(long total, long available, long occupied) {
    }

    //?: Các trạng thái đặt chỗ còn hiệu lực
    static final List<Reservation.Status> ACTIVE_RESERVATIONS = List.of(Reservation.Status.PENDING,
            Reservation.Status.CONFIRMED);

    private final ParkingSlotRepository slotRepo;
    private final ReservationRepository reservationRepo;

    //?: Khởi tạo ParkingSlotService với các repository cần thiết.
    public ParkingSlotService(ParkingSlotRepository slotRepo,
            ReservationRepository reservationRepo) {
        this.slotRepo = slotRepo;
        this.reservationRepo = reservationRepo;
    }

    //?: Lấy danh sách tất cả đỗ xe. 
    public List<ParkingSlot> getAll() {
        return slotRepo.findAll();
    }

    //?:Tìm kiếm thông tin đỗ xe dựa vào mã code.
    public ParkingSlot getByCode(String code) {
        return slotRepo.findByCode(code)
                                   //!: nếu không tìm thấy ô -> ném ngoại lệ để dừng chương trình.
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ô " + code));
    }

    //?:** Bộ lọc giao diện: all | A | B | VIP | free */
    public List<ParkingSlot> filter(String filter) {
        if (filter == null)
            return getAll();
        //!: Chuyển đổi từ khóa về chữ thường để so sanh và đưa ra các trường hợp 
        return switch (filter.toLowerCase()) {
            case "a" -> slotRepo.findByZone("A");
            case "b" -> slotRepo.findByZone("B");
            case "vip" -> slotRepo.findBySlotType(SlotType.VIP);
                                    //!: Lọc trạng thái ô trống sẵn sàng.
            case "free", "trong" -> slotRepo.findByStatus(SlotStatus.AVAILABLE);
            default -> getAll();
        };
    }
    //?: Thống kê số lượng ô đỗ xe trong hệ thống.
    public Stats getStats() {
        return new Stats(
                //!: tổng ô đỗ xe 
                slotRepo.count(),
                slotRepo.countByStatus(SlotStatus.AVAILABLE),
                slotRepo.countByStatus(SlotStatus.OCCUPIED));
    }

    //?:** Tìm kiếm danh sách các ô đỗ xe thời trống thực tế
    public List<ParkingSlot> findFreeSlots(LocalDateTime start, LocalDateTime end) {
        return slotRepo.findAll().stream()
                .filter(s -> s.getStatus() != SlotStatus.MAINTENANCE)
                .filter(s -> !isReserved(s.getId(), start, end))
                .toList();
    }

    //?:** true nếu ô đã có đặt chỗ trùng khoảng thời gian */
    public boolean isReserved(Long slotId, LocalDateTime start, LocalDateTime end) {
        return reservationRepo
                //!: Gọi hàm truy vấn nhanh từ Repository để kiểm tra sự tồn tại của bản ghi trùng lịch
                //!: Giải thích logic so sánh thời gian trùng: (Thời gian bắt đầu < end) VÀ (Thời gian kết thúc > start)
                .existsBySlotIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        slotId, ACTIVE_RESERVATIONS, end, start);
    }

    //?:Cập nhật trạng thái mới cho một ô đỗ xe dựa trên ID. */
    public void updateStatus(Long slotId, SlotStatus status) {
        ParkingSlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ô id=" + slotId));
        slot.setStatus(status);
        slotRepo.save(slot);
    }
}

/*  
    ?: stream: một chuỗi các phần tử có thể được xử lý tuần tự hoặc song song, thường được sử dụng để thao tác với các tập hợp dữ liệu.
    ?: IllegalArgumentException: ngoại lệ được ném ra khi một phương thức nhận được đối số không hợp lệ.
    ?: countByStatus: (đếm theo trạng thái) .
    ?: findByStatus: tìm theo trạng thái .
    ?: findByZone: Tìm kiếm các đối tượng .
    ?: findBySlotType: tìm kiếm theo kiểu -> ( vị trí đỗ xe ) .
    ?: existsBySlotIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan: tên một Query Method (Phương thức truy vấn tự động) cực kỳ chi tiết và mạnh mẽ trong Spring Data JPA (Java) -> kiểm tra sự tồn tại (True/False)
*/

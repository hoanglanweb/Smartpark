package com.example.SmartPark.controller;

import com.example.SmartPark.model.GuestReservationForm;
import com.example.SmartPark.model.ParkingAvailabilityForm;
import com.example.SmartPark.model.ParkingSession;
import com.example.SmartPark.model.ParkingSlot;
import com.example.SmartPark.model.ParkingSlot.SlotType;
import com.example.SmartPark.model.User;
import com.example.SmartPark.repository.VehicleRepository;
import com.example.SmartPark.repository.UserRepository;
import com.example.SmartPark.model.Reservation;
import com.example.SmartPark.model.Vehicle;
import com.example.SmartPark.service.ParkingSessionService;
import com.example.SmartPark.service.ParkingSlotService;
import com.example.SmartPark.service.ParkingSlotService.Stats;
import com.example.SmartPark.service.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
public class HomeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);
    private static final Set<String> FILTERS = Set.of("all", "a", "b", "vip", "free");

    private final ParkingSlotService parkingSlotService;
    private final ParkingSessionService parkingSessionService;
    private final ReservationService reservationService;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    // ?: quản lý
    public HomeController(
            ParkingSlotService parkingSlotService,
            ParkingSessionService parkingSessionService,
            ReservationService reservationService,
            UserRepository userRepository,
            VehicleRepository vehicleRepository) {
        this.parkingSlotService = parkingSlotService;
        this.parkingSessionService = parkingSessionService;
        this.reservationService = reservationService;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
    }

    // ?: xử lý yêu cầu trang đỗ xe người dùng.
    @GetMapping("/user/parking")
    public String home(
            // !: bộ lọc vị trí: tất cả , khu A , khu B , trống , VIP
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(required = false, defaultValue = "false") boolean checkAvailability,
            @ModelAttribute("availabilityForm") ParkingAvailabilityForm availabilityForm,
            BindingResult bindingResult,
            Authentication authentication,
            Model model) {

        Authentication contextAuthentication = SecurityContextHolder.getContext().getAuthentication();
        Authentication currentAuthentication = contextAuthentication != null
                ? contextAuthentication
                : authentication;
        String currentFilter = normalizeFilter(filter); // !: -> chuẩn hóa dữ liệu đầu vào của bộ lọc.

        List<ParkingSlot> filteredSlots = parkingSlotService.filter(currentFilter);

        // ?: danh sách dùng đề giao diện có thể đặt chỗ.
        Set<Long> availableSlotIds = parkingSlotService.filter("free")
                .stream()
                .map(ParkingSlot::getId)
                .collect(Collectors.toSet());

        // ?: Chỉ kiểm tra thời gian khi người dùng chủ động submit form.
        boolean availabilitySearch = checkAvailability;
        String availabilityError = null;

        if (checkAvailability) {
            if (bindingResult.hasErrors()
                    || !availabilityForm.isComplete()) {

                availabilityError = "Vui lòng nhập đầy đủ ngày, giờ bắt đầu và giờ kết thúc.";

            } else {
                try {
                    // !: ghép ngày và giờ để tạo thời gian tìm kiếm.
                    LocalDateTime start = availabilityForm.getDate()
                            .atTime(availabilityForm.getStartTime());
                    // !: ghép ngày và giờ kết thúc để tạo thời gian kết thúc tìm.
                    LocalDateTime end = availabilityForm.getDate()
                            .atTime(availabilityForm.getEndTime());

                    // ?: tìm kiếm vị trí trống toàn bộ thời gian yêu cầu.
                    availableSlotIds = parkingSlotService
                            .findFreeSlots(start, end)
                            .stream()
                            .map(ParkingSlot::getId)
                            .collect(Collectors.toSet());

                } catch (IllegalArgumentException exception) {
                    availabilityError = exception.getMessage();
                }
            }
        }

        // ?: sắp xếp theo danh sách -> tên khu vực , số thứ tự , ID tiêu chí.
        List<ParkingSlot> ordered = filteredSlots.stream()
                .sorted(Comparator
                        // !: comparing : so sánh , thước đo căn cứ các vị trí.
                        .comparing((ParkingSlot slot) -> slot.getZone() == null ? "" : slot.getZone())
                        .thenComparingInt(slot -> getNumber(slot.getCode()))
                        .thenComparing(ParkingSlot::getId))
                .toList();
        // ?: lấy tất cả vị trí A
        List<ParkingSlot> zoneA = ordered.stream()
                .filter(slot -> "A".equalsIgnoreCase(slot.getZone()))
                .toList();
        // ?: lấy tất cả vị trí A (VIP) -> giới hạn 10 slot.
        List<ParkingSlot> zoneAVip = zoneA.stream()
                .filter(slot -> slot.getSlotType() == SlotType.VIP)
                .limit(10)
                .toList();
        // ?: tạo ID của các vị trí VIP ở trên.
        Set<Long> vipIds = zoneAVip.stream().map(ParkingSlot::getId).collect(Collectors.toSet());

        // ?: lấy vị trí còn lại của A.
        List<ParkingSlot> zoneAStandard = zoneA.stream()
                .filter(slot -> !vipIds.contains(slot.getId()))
                .toList();

        // ?: lấy tất cả vị trí khu B.
        List<ParkingSlot> zoneB = ordered.stream()
                .filter(slot -> "B".equalsIgnoreCase(slot.getZone()))
                .toList();

        List<ParkingSlot> zoneBMiddle = new ArrayList<>();
        List<ParkingSlot> zoneBLower = new ArrayList<>();
        // !: phân chia vị trí khu B.
        for (ParkingSlot slot : zoneB) {
            if (isMiddleRow(slot)) {
                zoneBMiddle.add(slot);
            } else if (isLowerRow(slot)) {
                zoneBLower.add(slot);
            } else if (getNumber(slot.getCode()) <= 30) {
                zoneBMiddle.add(slot);
            } else {
                zoneBLower.add(slot);
            }
        }
        // ?: lấy ID tất cả vị trí khu A và B đã sắp xếp.
        Set<Long> mappedZoneIds = ordered.stream()
                // !: equalsIgnoreCase -> so sanh chưỡi ký tự có giống nhau không !!
                .filter(slot -> "A".equalsIgnoreCase(slot.getZone()) || "B".equalsIgnoreCase(slot.getZone()))
                .map(ParkingSlot::getId)
                .collect(Collectors.toSet());
        // ?: các vị trí có thể hiển thị trong khu vực riêng
        List<ParkingSlot> otherZones = ordered.stream()
                .filter(slot -> !mappedZoneIds.contains(slot.getId()))
                .toList();
        // ?: truyền dữ liệu , danh sách -> giao diện VIEW
        Stats stats = parkingSlotService.getStats();
        model.addAttribute("khuA", zoneAStandard);
        model.addAttribute("khuAVip", zoneAVip);
        model.addAttribute("khuBGiua", zoneBMiddle);
        model.addAttribute("khuBDuoi", zoneBLower);
        model.addAttribute("khuKhac", otherZones);
        model.addAttribute("tongO", stats.total());
        model.addAttribute("conTrong", stats.available());
        model.addAttribute("daDo", stats.occupied());
        // !: tính tổng vị trí bằng = 0 thì trả về 0
        model.addAttribute("occupancyPercent",
                stats.total() == 0 ? 0 : Math.round((double) stats.occupied() * 100 / stats.total()));
        model.addAttribute("availableSlotIds", availableSlotIds);
        model.addAttribute("availabilitySearch", availabilitySearch && availabilityError == null);
        model.addAttribute("availabilityError", availabilityError);
        // !: kiểm tra người dùng có phải ADMIN không.
        model.addAttribute("currentFilter", currentFilter);
        boolean isAdmin = currentAuthentication != null && currentAuthentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        // Lấy họ tên của người dùng đang đăng nhập.
        model.addAttribute("vehicles", List.<Vehicle>of());
        if (currentAuthentication != null
                && currentAuthentication.isAuthenticated()
                && !"anonymousUser".equals(currentAuthentication.getName())) {

            userRepository.findByEmail(currentAuthentication.getName())
                    .ifPresent(user -> {
                        model.addAttribute("fullname", user.getFullName());
                        model.addAttribute("vehicles", vehicleRepository.findByOwnerId(user.getId()));
                    });
        }
        // !: chỉ tải danh sách đặt chỗ gần đây nếu là ADMin.
        model.addAttribute("reservations", isAdmin ? reservationService.getRecent() : List.of());
        model.addAttribute("guestReservationForm", new GuestReservationForm());
        return "Home";
    }

    // ?: xử lý yêu cầu ghi nhận xe.
    /*
     * @PostMapping("/parking-sessions/check-in")
     * public String checkInVehicle(
     * 
     * @RequestParam Long slotId,
     * 
     * @RequestParam String plateNumber,
     * 
     * @RequestParam String vehicleType,
     * RedirectAttributes redirectAttributes) {
     * try {
     * //!: Chuyển về chữ hoa và sử dụng Locale.ROOT để tránh phụ thuộc ngôn ngữ hệ
     * thống.
     * Vehicle.VehicleType type =
     * Vehicle.VehicleType.valueOf(vehicleType.toUpperCase(Locale.ROOT));
     * ParkingSession session = parkingSessionService.checkIn(slotId, plateNumber,
     * type);
     * redirectAttributes.addFlashAttribute( //!: ghi vào bộ nhớ tạm thời -> sẽ mất
     * nếu reload
     * "parkingOperationSuccess",
     * "Đã ghi nhận xe " + session.getVehicle().getPlateNumber()
     * + " vào vị trí " + session.getSlot().getCode() + ".");
     * } catch (IllegalArgumentException | IllegalStateException exception) {
     * //!: xử lý lỗi đầu vào
     * redirectAttributes.addFlashAttribute("parkingOperationError",
     * exception.getMessage());
     * } catch (RuntimeException exception) {
     * //!: ghi chi tiết lỗi
     * LOGGER.error("Failed to check in vehicle at slot {}", slotId, exception);
     * redirectAttributes.addFlashAttribute(
     * "parkingOperationError",
     * "Không thể ghi nhận xe vào lúc này. Vui lòng thử lại.");
     * }
     * return "redirect:/admin/dashboard";
     * }
     */

    // ?: xử lý yêu cầu ra xe.
    /*
     * @PostMapping("/parking-sessions/check-out")
     * public String checkOutVehicle(
     * 
     * @RequestParam Long slotId,
     * RedirectAttributes redirectAttributes) {
     * try {
     * //!: truy cập vào dữ liệu tìm kiếm xe đã đặt chỗ theo Id
     * ParkingSession session = parkingSessionService.checkOut(slotId);
     * redirectAttributes.addFlashAttribute(
     * "parkingOperationSuccess",
     * "Đã ghi nhận xe " + session.getVehicle().getPlateNumber()
     * + " rời vị trí " + session.getSlot().getCode() + ".");
     * } catch (IllegalArgumentException | IllegalStateException exception) {
     * redirectAttributes.addFlashAttribute("parkingOperationError",
     * exception.getMessage());
     * } catch (RuntimeException exception) {
     * LOGGER.error("Failed to check out vehicle from slot {}", slotId, exception);
     * redirectAttributes.addFlashAttribute(
     * "parkingOperationError",
     * "Không thể ghi nhận xe ra lúc này. Vui lòng thử lại.");
     * }
     * return "redirect:/admin/dashboard";
     * }
     */
    // ?: hiển thị biễu mẫu đặt chỗ.
    @GetMapping("/reservations/new")
    public String guestReservationForm(
            @RequestParam(required = false) Long slotId,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        Authentication contextAuthentication = SecurityContextHolder.getContext().getAuthentication();
        Authentication currentAuthentication = contextAuthentication != null
                ? contextAuthentication
                : authentication;
        try {
            ParkingSlot slot = parkingSlotService.getById(slotId);

            if (slot.getStatus() != ParkingSlot.SlotStatus.AVAILABLE) {
                throw new IllegalStateException(
                        "Vị trí này hiện không còn trống");
            }

            model.addAttribute("slot", slot);
            GuestReservationForm form = new GuestReservationForm();
            form.setSlotId(slot.getId());
            model.addAttribute("guestReservationForm", form);
            model.addAttribute("vehicles", List.<Vehicle>of());
            if (currentAuthentication != null
                    && currentAuthentication.isAuthenticated()
                    && !"anonymousUser".equals(currentAuthentication.getName())) {
                userRepository.findByEmail(currentAuthentication.getName())
                        .ifPresent(user -> model.addAttribute(
                                "vehicles", vehicleRepository.findByOwnerId(user.getId())));
            }

            return "Reservation";

        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "reservationError", exception.getMessage());

            return "redirect:/user/parking";
        }
    }

    @PostMapping("/reservations/guest")
    public Object createGuestReservation(
            @ModelAttribute("guestReservationForm") GuestReservationForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            String message = "Thông tin đặt chỗ không hợp lệ.";
            if (bindingResult.getFieldError() != null) {
                message = "Giá trị " + bindingResult.getFieldError().getField() + " không hợp lệ.";
            }
            if (acceptsJson(request)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", message));
            }
            redirectAttributes.addFlashAttribute("reservationError", message);
            if (form.getSlotId() != null) {
                redirectAttributes.addAttribute("slotId", form.getSlotId());
                return "redirect:/reservations/new";
            }
            return "redirect:/user/parking";
        }

        try {
            Reservation reservation;
            if (form.getVehicleId() == null) {
                throw new IllegalArgumentException(
                        "Vui lòng chọn xe đã đăng ký trước khi đặt chỗ.");
            }
            Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
            if (currentAuthentication == null
                    || !currentAuthentication.isAuthenticated()
                    || "anonymousUser".equals(currentAuthentication.getName())) {
                if (acceptsJson(request)) {
                    return ResponseEntity.status(401)
                            .body(Map.of("success", false, "message", "Vui lòng đăng nhập để đặt chỗ."));
                }
                return "redirect:/login";
            }
            User user = userRepository.findByEmail(currentAuthentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Không tìm thấy tài khoản đăng nhập"));
            reservation = reservationService.create(
                    user.getId(),
                    form.getVehicleId(),
                    form.getSlotId(),
                    form.getDate(),
                    form.getStartTime(),
                    form.getEndTime());
            String message = "Yêu cầu đặt chỗ " + reservation.getId()
                    + " đã được gửi và đang chờ xác nhận.";
            if (acceptsJson(request)) {
                return ResponseEntity.ok(Map.of("success", true, "message", message));
            }
            redirectAttributes.addFlashAttribute("reservationSuccess", message);
            return "redirect:/user/parking";

        } catch (IllegalArgumentException | IllegalStateException exception) {
            if (acceptsJson(request)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", exception.getMessage()));
            }
            redirectAttributes.addFlashAttribute(
                    "reservationError", exception.getMessage());

            if (form.getSlotId() != null) {
                redirectAttributes.addAttribute("slotId", form.getSlotId());
                return "redirect:/reservations/new";
            }

            return "redirect:/user/parking";
        }
    }

    // ?: kiểm tra request có nhận JSON ko.
    private boolean acceptsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    // ?: chuẩn hóa giá trị lọc vị trí đỗ xe.
    private String normalizeFilter(String filter) {
        if (filter == null) {
            return "all";
        }
        String normalized = filter.toLowerCase(Locale.ROOT); // !: chuyển giá trị chữ thương thành hoa.
        return FILTERS.contains(normalized) ? normalized : "all";
    }

    // ?: lây phần số từ mã vị trí đỗ xe.
    private int getNumber(String code) {
        if (code == null) {
            return Integer.MAX_VALUE;
        }
        String number = code.replaceAll("[^0-9]", "");
        if (number.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    // ?: kiểm tra vị trí đỗ xe hàng giữa.
    private boolean isMiddleRow(ParkingSlot slot) {
        String rowName = slot.getRowName();
        if (rowName == null) {
            return false;
        }
        String normalized = rowName.toLowerCase(Locale.ROOT);
        return normalized.contains("middle") || normalized.contains("mid") || normalized.contains("giua");
    }

    // ?: kiểm tra vị trí đỗ xe hàng dưới.
    private boolean isLowerRow(ParkingSlot slot) {
        String rowName = slot.getRowName();
        if (rowName == null) {
            return false;
        }
        String normalized = rowName.toLowerCase(Locale.ROOT);
        return normalized.contains("lower") || normalized.contains("bottom") || normalized.contains("duoi");
    }
}

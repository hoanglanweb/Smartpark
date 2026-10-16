package com.example.SmartPark.controller;

import com.example.SmartPark.model.GuestReservationForm;
import com.example.SmartPark.model.ParkingAvailabilityForm;
import com.example.SmartPark.model.ParkingSession;
import com.example.SmartPark.model.ParkingSlot;
import com.example.SmartPark.model.ParkingSlot.SlotType;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;

@Controller
public class HomeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);
    private static final Set<String> FILTERS = Set.of("all", "a", "b", "vip", "free");

    private final ParkingSlotService parkingSlotService;
    private final ParkingSessionService parkingSessionService;
    private final ReservationService reservationService;

    //?: quản lý 
    public HomeController(
            ParkingSlotService parkingSlotService, 
            ParkingSessionService parkingSessionService,
            ReservationService reservationService) {
        this.parkingSlotService = parkingSlotService;
        this.parkingSessionService = parkingSessionService;
        this.reservationService = reservationService;
    }
    //?: xử lý yêu cầu trang đỗ xe người dùng.
    @GetMapping("/user/parking")
    public String home(
            //!: bộ lọc vị trí: tất cả , khu A , khu B , trống , VIP
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(required = false, defaultValue = "false") boolean checkAvailability,
            @ModelAttribute("availabilityForm") ParkingAvailabilityForm availabilityForm,
            BindingResult bindingResult,
            Authentication authentication,
            Model model) {

        String currentFilter = normalizeFilter(filter); //!: -> chuẩn hóa dữ liệu đầu vào của bộ lọc.

        List<ParkingSlot> filteredSlots = parkingSlotService.filter(currentFilter);
        
        //?: danh sách dùng đề giao diện có thể đặt chỗ.
        Set<Long> availableSlotIds = parkingSlotService.filter("free")
                .stream()
                .map(ParkingSlot::getId)
                .collect(Collectors.toSet());

        //?: Chỉ kiểm tra thời gian khi người dùng chủ động submit form.
        boolean availabilitySearch = checkAvailability;
        String availabilityError = null;
 
        if (checkAvailability) {
            if (bindingResult.hasErrors()
                    || !availabilityForm.isComplete()) {

                availabilityError = "Vui lòng nhập đầy đủ ngày, giờ bắt đầu và giờ kết thúc.";

            } else {
                try {
                    //!: ghép ngày và giờ để tạo thời gian tìm kiếm.
                    LocalDateTime start = availabilityForm.getDate()
                            .atTime(availabilityForm.getStartTime());
                    //!: ghép ngày và giờ kết thúc để tạo thời gian kết thúc tìm.
                    LocalDateTime end = availabilityForm.getDate()
                            .atTime(availabilityForm.getEndTime());

                    //?: tìm kiếm vị trí trống toàn bộ thời gian yêu cầu.
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

        //?: sắp xếp theo danh sách -> tên khu vực , số thứ tự , ID tiêu chí. 
        List<ParkingSlot> ordered = filteredSlots.stream()
                .sorted(Comparator
                        //!: comparing : so sánh , thước đo căn cứ các vị trí.
                        .comparing((ParkingSlot slot) -> slot.getZone() == null ? "" : slot.getZone())
                        .thenComparingInt(slot -> getNumber(slot.getCode()))
                        .thenComparing(ParkingSlot::getId))
                .toList();
        //?: lấy tất cả vị trí A
        List<ParkingSlot> zoneA = ordered.stream()
                .filter(slot -> "A".equalsIgnoreCase(slot.getZone()))
                .toList();
        //?: lấy tất cả vị trí A (VIP) -> giới hạn 10 slot.
        List<ParkingSlot> zoneAVip = zoneA.stream()
                .filter(slot -> slot.getSlotType() == SlotType.VIP)
                .limit(10)
                .toList();
        //?: tạo ID của các vị trí VIP ở trên.
        Set<Long> vipIds = zoneAVip.stream().map(ParkingSlot::getId).collect(Collectors.toSet());

        //?: lấy vị trí còn lại của A.
        List<ParkingSlot> zoneAStandard = zoneA.stream()
                .filter(slot -> !vipIds.contains(slot.getId()))
                .toList();

        //?: lấy tất cả vị trí khu B.
        List<ParkingSlot> zoneB = ordered.stream()
                .filter(slot -> "B".equalsIgnoreCase(slot.getZone()))
                .toList();
        
        List<ParkingSlot> zoneBMiddle = new ArrayList<>();
        List<ParkingSlot> zoneBLower = new ArrayList<>();
        //!: phân chia vị trí khu B.
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
        //?: lấy ID tất cả vị trí khu A và B đã sắp xếp.
        Set<Long> mappedZoneIds = ordered.stream()
                //!: equalsIgnoreCase -> so sanh chưỡi ký tự có giống nhau không !!
                .filter(slot -> "A".equalsIgnoreCase(slot.getZone()) || "B".equalsIgnoreCase(slot.getZone()))
                .map(ParkingSlot::getId)
                .collect(Collectors.toSet());
        //?: các vị trí có thể hiển thị trong khu vực riêng 
        List<ParkingSlot> otherZones = ordered.stream()
                .filter(slot -> !mappedZoneIds.contains(slot.getId()))
                .toList();
        //?: truyền dữ liệu , danh sách -> giao diện VIEW 
        Stats stats = parkingSlotService.getStats();
        model.addAttribute("khuA", zoneAStandard);
        model.addAttribute("khuAVip", zoneAVip);
        model.addAttribute("khuBGiua", zoneBMiddle);
        model.addAttribute("khuBDuoi", zoneBLower);
        model.addAttribute("khuKhac", otherZones);
        model.addAttribute("tongO", stats.total());
        model.addAttribute("conTrong", stats.available());
        model.addAttribute("daDo", stats.occupied());
        //!: tính tổng vị trí bằng = 0 thì trả về 0 
        model.addAttribute("occupancyPercent",
                stats.total() == 0 ? 0 : Math.round((double) stats.occupied() * 100 / stats.total()));
        model.addAttribute("availableSlotIds", availableSlotIds);
        model.addAttribute("availabilitySearch", availabilitySearch && availabilityError == null);
        model.addAttribute("availabilityError", availabilityError);
        //!: kiểm tra người dùng có phải ADMIN không. 
        model.addAttribute("currentFilter", currentFilter);
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        //!: chỉ tải danh sách đặt chỗ gần đây nếu là ADMin.
        model.addAttribute("reservations", isAdmin ? reservationService.getRecent() : List.of());
        model.addAttribute("guestReservationForm", new GuestReservationForm());
        return "Home";
    }

    //?: xử lý yêu cầu ghi nhận xe.
    @PostMapping("/parking-sessions/check-in")
    public String checkInVehicle(
            @RequestParam Long slotId,
            @RequestParam String plateNumber,
            @RequestParam String vehicleType,
            RedirectAttributes redirectAttributes) {
        try {
            //!: Chuyển về chữ hoa và sử dụng Locale.ROOT để tránh phụ thuộc ngôn ngữ hệ thống.
            Vehicle.VehicleType type = Vehicle.VehicleType.valueOf(vehicleType.toUpperCase(Locale.ROOT));
            ParkingSession session = parkingSessionService.checkIn(slotId, plateNumber, type);
            redirectAttributes.addFlashAttribute( //!: ghi vào bộ nhớ tạm thời -> sẽ mất nếu reload  
                    "parkingOperationSuccess",
                    "Đã ghi nhận xe " + session.getVehicle().getPlateNumber()
                            + " vào vị trí " + session.getSlot().getCode() + ".");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            //!: xử lý lỗi đầu vào 
            redirectAttributes.addFlashAttribute("parkingOperationError", exception.getMessage());
        } catch (RuntimeException exception) {
            //!: ghi chi tiết lỗi
            LOGGER.error("Failed to check in vehicle at slot {}", slotId, exception);
            redirectAttributes.addFlashAttribute(
                    "parkingOperationError", "Không thể ghi nhận xe vào lúc này. Vui lòng thử lại.");
        }
        return "redirect:/admin/dashboard";
    }

    //?: xử lý yêu cầu ra xe.
    @PostMapping("/parking-sessions/check-out")
    public String checkOutVehicle(
            @RequestParam Long slotId,
            RedirectAttributes redirectAttributes) {
        try {
            //!: truy cập vào dữ liệu tìm kiếm xe đã đặt chỗ theo Id
            ParkingSession session = parkingSessionService.checkOut(slotId);
            redirectAttributes.addFlashAttribute(
                    "parkingOperationSuccess",
                    "Đã ghi nhận xe " + session.getVehicle().getPlateNumber()
                            + " rời vị trí " + session.getSlot().getCode() + ".");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("parkingOperationError", exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to check out vehicle from slot {}", slotId, exception);
            redirectAttributes.addFlashAttribute(
                    "parkingOperationError", "Không thể ghi nhận xe ra lúc này. Vui lòng thử lại.");
        }
        return "redirect:/admin/dashboard";
    }

    //?: hiển thị biễu mẫu đặt chỗ.
    @GetMapping("/reservations/new")
    public String guestReservationForm(
            @RequestParam(required = false) Long slotId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            //!: lấy thông tin vị trí đỗ xe từ service.
            ParkingSlot slot = parkingSlotService.getById(slotId);
            if (slot.getStatus() != ParkingSlot.SlotStatus.AVAILABLE) {
                //!: nếu vị trí không còn trống -> thông báo.
                throw new IllegalStateException("Vị trí này hiện không còn trống");
            }
            model.addAttribute("slot", slot);
            GuestReservationForm form = new GuestReservationForm();
            form.setSlotId(slot.getId());
            model.addAttribute("guestReservationForm", form);
            return "Reservation";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            //!: xử lý lỗi 
            redirectAttributes.addFlashAttribute("reservationError", exception.getMessage());
            return "redirect:/user/parking";
        }
    }

    //?: xử lý yêu cầu tạo đặt chỗ mới.
    @PostMapping("/reservations/guest")
    public Object createGuestReservation(
            //!: nhận thông tin vị trí muốn đặt.
            @ModelAttribute("guestReservationForm") GuestReservationForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        //!: kiểm tra client có nhận yêu cầu JSON ko.
        boolean jsonRequest = acceptsJson(request);
        if (bindingResult.hasErrors()) {
            if (jsonRequest) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Kiểm tra lại ngày, giờ và loại phương tiện."));
            }
            //!: gửi biểu mẫu HTML , lưu thông báo lỗi.
            redirectAttributes.addFlashAttribute("reservationError", "Kiểm tra lại thông tin đặt chỗ.");

            //!: nếu có vị trí Id , quay lại trang biểu mẫu của vị trí. 
            if (form.getSlotId() != null) {
                redirectAttributes.addAttribute("slotId", form.getSlotId());
                return "redirect:/reservations/new";
            }
            return "redirect:/user/parking";
        }
        try {
            //?: gọi tầng service xử lý , truyền thông tin USER.
            Reservation reservation = reservationService.createGuest(
                    form.getFullName(), form.getPhone(), form.getPlateNumber(), form.getVehicleType(),
                    form.getSlotId(), form.getDate(), form.getStartTime(), form.getEndTime());
            String successMessage = "Yêu cầu đặt chỗ " + reservation.getId() + " đã được gửi và đang chờ xác nhận.";
            //?: nếu client trả về JSON , trả HTTP 500 
            if (jsonRequest) {
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                        "success", true,
                        "message", successMessage,
                        "reservationId", reservation.getId()));
            }
            redirectAttributes.addFlashAttribute("reservationSuccess", successMessage);
            return "redirect:/user/parking";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            //!: xử lý dữ liệu đầu vào hoặc lỗi nghiệp vụ. 
            if (jsonRequest) {
                HttpStatus status = exception instanceof IllegalStateException
                        ? HttpStatus.CONFLICT
                        : HttpStatus.BAD_REQUEST; //!: lỗi 400 Bad Request.
                return ResponseEntity.status(status)
                        //!: trả về nội dung lỗi.
                        .body(Map.of("success", false, "message", exception.getMessage()));
            }
            redirectAttributes.addFlashAttribute("reservationError", exception.getMessage());
            if (form.getSlotId() != null) {
                redirectAttributes.addAttribute("slotId", form.getSlotId());
                return "redirect:/reservations/new";
            }
            return "redirect:/user/parking";
        } catch (RuntimeException exception) { 
            //!: ghi nhận lỗi vào hệ thống log() -> Id, vị trí , thông tin
            LOGGER.error("Failed to create guest reservation for slot {}", form.getSlotId(), exception);
            if (jsonRequest) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false,
                                "message", "Không thể gửi yêu cầu lúc này. Vui lòng thử lại sau."));
            }
            redirectAttributes.addFlashAttribute("reservationError",
                    "Không thể gửi yêu cầu lúc này. Vui lòng thử lại sau.");
            if (form.getSlotId() != null) {
                redirectAttributes.addAttribute("slotId", form.getSlotId());
                return "redirect:/reservations/new";
            }
            return "redirect:/user/parking";
        }
    }

    //?: kiểm tra request có nhận JSON ko.
    private boolean acceptsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    //?: chuẩn hóa giá trị lọc vị trí đỗ xe.
    private String normalizeFilter(String filter) {
        if (filter == null) {
            return "all";
        }
        String normalized = filter.toLowerCase(Locale.ROOT); //!: chuyển giá trị chữ thương thành hoa.
        return FILTERS.contains(normalized) ? normalized : "all";
    }

    //?: lây phần số từ mã vị trí đỗ xe.
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

    //?: kiểm tra vị trí đỗ xe hàng giữa.
    private boolean isMiddleRow(ParkingSlot slot) {
        String rowName = slot.getRowName();
        if (rowName == null) {
            return false;
        }
        String normalized = rowName.toLowerCase(Locale.ROOT);
        return normalized.contains("middle") || normalized.contains("mid") || normalized.contains("giua");
    }

    //?: kiểm tra vị trí đỗ xe hàng dưới.
    private boolean isLowerRow(ParkingSlot slot) {
        String rowName = slot.getRowName();
        if (rowName == null) {
            return false;
        }
        String normalized = rowName.toLowerCase(Locale.ROOT);
        return normalized.contains("lower") || normalized.contains("bottom") || normalized.contains("duoi");
    }
}

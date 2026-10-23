package com.example.SmartPark.controller;

import com.example.SmartPark.model.ParkingSlot;
import com.example.SmartPark.model.ParkingSession;
import com.example.SmartPark.model.Reservation;
import com.example.SmartPark.model.User;
import com.example.SmartPark.model.Vehicle;
import com.example.SmartPark.repository.UserRepository;
import com.example.SmartPark.repository.VehicleRepository;
import com.example.SmartPark.service.ParkingSessionService;
import com.example.SmartPark.service.ParkingSlotService;
import com.example.SmartPark.service.ParkingSlotService.Stats;
import com.example.SmartPark.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@WebMvcTest(controllers = HomeController.class)
@WithMockUser(username = "test-user", roles = "USER")
class HomeTemplateRenderingTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParkingSlotService parkingSlotService;

    @MockitoBean
    private ParkingSessionService parkingSessionService;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private VehicleRepository vehicleRepository;

    @Test
    void rendersBackendSlotsAndStatsWithoutTemplateBindings() throws Exception {
        List<ParkingSlot> allSlots = List.of(
                slot("A01", "A", ParkingSlot.SlotType.STANDARD, ParkingSlot.SlotStatus.AVAILABLE),
                slot("A02", "A", ParkingSlot.SlotType.VIP, ParkingSlot.SlotStatus.RESERVED),
                slot("A03", "A", ParkingSlot.SlotType.PRIORITY, ParkingSlot.SlotStatus.OCCUPIED),
                slot("B01", "B", ParkingSlot.SlotType.STANDARD, ParkingSlot.SlotStatus.AVAILABLE),
                slot("B29", "B", ParkingSlot.SlotType.CAR, ParkingSlot.SlotStatus.MAINTENANCE));
        when(parkingSlotService.filter("all")).thenReturn(allSlots);
        when(parkingSlotService.filter("free")).thenReturn(List.of(allSlots.get(0), allSlots.get(3)));
        when(parkingSlotService.getStats()).thenReturn(new Stats(5, 2, 1));
        when(reservationService.getRecent()).thenReturn(List.of());
        User owner = new User();
        owner.setId(7L);
        when(userRepository.findByEmail("test-user")).thenReturn(Optional.of(owner));
        Vehicle vehicle = new Vehicle();
        vehicle.setId(5L);
        vehicle.setPlateNumber("51A-12345");
        when(vehicleRepository.findByOwnerId(7L)).thenReturn(List.of(vehicle));

        mockMvc.perform(get("/user/parking")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("Home"))
                .andExpect(content().string(containsString("id=\"stat-total\">5</strong>")))
                .andExpect(content().string(containsString("id=\"stat-available\">2</strong>")))
                .andExpect(content().string(containsString("id=\"stat-occupied\">1</strong>")))
                .andExpect(content().string(containsString("A02")))
                .andExpect(content().string(containsString("B29")))
                .andExpect(content().string(containsString("name=\"slotId\"")))
                .andExpect(content().string(containsString("id=\"guest-reservation-dialog\"")))
                .andExpect(content().string(containsString("name=\"vehicleId\"")))
                .andExpect(content().string(containsString("value=\"5\"")))
                .andExpect(content().string(containsString("51A-12345 (CAR)")))
                .andExpect(content().string(containsString("id=\"open-booking\" type=\"button\" disabled")))
                .andExpect(content().string(containsString("id=\"booking-mode-fixed\" type=\"checkbox\" checked disabled />")))
                .andExpect(content().string(containsString("Đặt theo khung giờ cố định")))
                .andExpect(content().string(containsString("id=\"booking-mode-duration\" type=\"checkbox\" disabled />")))
                .andExpect(content().string(containsString("Đặt theo giờ đỗ")))
                .andExpect(content().string(containsString("name=\"checkAvailability\" value=\"true\"")))
                .andExpect(content().string(containsString("data-slot-id=")))
                .andExpect(content().string(containsString("disabled=\"disabled\"")))
                .andExpect(content().string(containsString("aria-label=\"A03, Đang có xe\"")))
                .andExpect(content().string(not(containsString("class=\"slot-status\""))))
                .andExpect(content().string(not(containsString("class=\"slot-type\""))))
                .andExpect(content().string(not(containsString("type=\"radio\""))))
                .andExpect(content().string(not(containsString("guest-form-panel"))))
                .andExpect(content().string(containsString("Đã đặt")))
                .andExpect(content().string(containsString("Bảo trì")))
                .andExpect(content().string(not(containsString("th:each"))));
        verify(vehicleRepository).findByOwnerId(7L);
    }

    @Test
    void showsRegistrationPromptWhenCurrentUserHasNoVehicles() throws Exception {
        List<ParkingSlot> slots = List.of(
                slot("A01", "A", ParkingSlot.SlotType.STANDARD, ParkingSlot.SlotStatus.AVAILABLE));
        when(parkingSlotService.filter("all")).thenReturn(slots);
        when(parkingSlotService.filter("free")).thenReturn(slots);
        when(parkingSlotService.getStats()).thenReturn(new Stats(1, 1, 0));
        when(reservationService.getRecent()).thenReturn(List.of());
        User owner = new User();
        owner.setId(7L);
        when(userRepository.findByEmail("test-user")).thenReturn(Optional.of(owner));
        when(vehicleRepository.findByOwnerId(7L)).thenReturn(List.of());

        mockMvc.perform(get("/user/parking")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Bạn chưa đăng ký xe. Vui lòng đăng ký xe trước khi đặt chỗ.")))
                .andExpect(content().string(containsString("href=\"/vehicles/new\"")))
                .andExpect(content().string(not(containsString("id=\"vehicleId\""))));
    }

    @Test
    void filtersTheMapByRequestedZoneAndKeepsRealStats() throws Exception {
        List<ParkingSlot> zoneB = List.of(
                slot("B01", "B", ParkingSlot.SlotType.STANDARD, ParkingSlot.SlotStatus.AVAILABLE));
        when(parkingSlotService.filter("b")).thenReturn(zoneB);
        when(parkingSlotService.filter("free")).thenReturn(zoneB);
        when(parkingSlotService.getStats()).thenReturn(new Stats(1, 1, 0));
        when(reservationService.getRecent()).thenReturn(List.of());

        mockMvc.perform(get("/user/parking")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user").roles("USER"))
                        .param("filter", "b"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("B01")))
                .andExpect(content().string(not(containsString("A01"))))
                .andExpect(content().string(containsString("Khu B")));
    }

    @Test
    void availabilitySearchFiltersAvailableSlotsByRequestedTimeWindow() throws Exception {
        ParkingSlot unavailable = slot(
                "A01", "A", ParkingSlot.SlotType.STANDARD, ParkingSlot.SlotStatus.AVAILABLE);
        ParkingSlot available = slot(
                "A02", "A", ParkingSlot.SlotType.STANDARD, ParkingSlot.SlotStatus.AVAILABLE);
        when(parkingSlotService.filter("all")).thenReturn(List.of(unavailable, available));
        when(parkingSlotService.filter("free")).thenReturn(List.of(unavailable, available));
        when(parkingSlotService.findFreeSlots(
                java.time.LocalDateTime.of(2030, 1, 1, 8, 0),
                java.time.LocalDateTime.of(2030, 1, 1, 9, 0)))
                .thenReturn(List.of(available));
        when(parkingSlotService.getStats()).thenReturn(new Stats(2, 2, 0));
        when(reservationService.getRecent()).thenReturn(List.of());

        mockMvc.perform(get("/user/parking")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user").roles("USER"))
                        .param("checkAvailability", "true")
                        .param("date", "2030-01-01")
                        .param("startTime", "08:00")
                        .param("endTime", "09:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sơ đồ đã lọc theo khoảng thời gian.")))
                .andExpect(content().string(containsString(
                        "aria-label=\"A01, Không khả dụng trong thời gian đã chọn\"")))
                .andExpect(content().string(containsString(
                        "aria-label=\"A02, Còn trống\"")));

        verify(parkingSlotService).findFreeSlots(
                java.time.LocalDateTime.of(2030, 1, 1, 8, 0),
                java.time.LocalDateTime.of(2030, 1, 1, 9, 0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void checkInRouteReportsVehicleEntry() throws Exception {
        ParkingSlot slot = slot("A01", "A", ParkingSlot.SlotType.STANDARD, ParkingSlot.SlotStatus.OCCUPIED);
        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber("51A12345");
        ParkingSession session = new ParkingSession();
        session.setSlot(slot);
        session.setVehicle(vehicle);
        when(parkingSessionService.checkIn(1L, "51A12345", Vehicle.VehicleType.CAR)).thenReturn(session);

        mockMvc.perform(post("/parking-sessions/check-in")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-admin").roles("ADMIN"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("slotId", "1")
                        .param("plateNumber", "51A12345")
                        .param("vehicleType", "CAR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(
                        "parkingOperationSuccess", "Đã ghi nhận xe 51A12345 vào vị trí A01."));
    }

    @Test
    void selectedSlotOpensServerRenderedGuestBookingForm() throws Exception {
        ParkingSlot selected = slot("A05", "A", ParkingSlot.SlotType.VIP, ParkingSlot.SlotStatus.AVAILABLE);
        when(parkingSlotService.getById(selected.getId())).thenReturn(selected);

        mockMvc.perform(get("/reservations/new")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user").roles("USER"))
                        .param("slotId", selected.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("Reservation"))
                .andExpect(content().string(containsString("A05")))
                .andExpect(content().string(containsString(
                        "Bạn chưa đăng ký xe. Vui lòng đăng ký xe trước khi đặt chỗ.")))
                .andExpect(content().string(containsString("href=\"/vehicles/new?slotId=")))
                .andExpect(content().string(containsString("Xác nhận đặt chỗ")))
                .andExpect(content().string(containsString("disabled=\"disabled\"")))
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    int registrationLink = body.indexOf("vehicle-registration-link");
                    int reservationForm = body.indexOf("<form class=\"guest-reservation-form\"");
                    assertTrue(registrationLink >= 0 && registrationLink < reservationForm);
                });
    }

    @Test
    void reservationFormLoadsOnlyVehiclesOwnedByCurrentAccount() throws Exception {
        ParkingSlot selected = slot("A05", "A", ParkingSlot.SlotType.VIP, ParkingSlot.SlotStatus.AVAILABLE);
        User owner = new User();
        owner.setId(7L);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(5L);
        vehicle.setPlateNumber("51A-12345");
        vehicle.setVehicleType(Vehicle.VehicleType.CAR);
        when(parkingSlotService.getById(selected.getId())).thenReturn(selected);
        when(userRepository.findByEmail("test-user")).thenReturn(Optional.of(owner));
        when(vehicleRepository.findByOwnerId(7L)).thenReturn(List.of(vehicle));

        mockMvc.perform(get("/reservations/new")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user").roles("USER"))
                        .param("slotId", selected.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"5\"")))
                .andExpect(content().string(containsString("51A-12345 (CAR)")))
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    int registrationLink = body.indexOf("vehicle-registration-link");
                    int reservationForm = body.indexOf("<form class=\"guest-reservation-form\"");
                    assertTrue(registrationLink >= 0 && registrationLink < reservationForm);
                });

        verify(vehicleRepository).findByOwnerId(7L);
    }

    @Test
    void guestReservationApiReturnsValidationErrorsAsJson() throws Exception {
        mockMvc.perform(post("/reservations/guest")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user").roles("USER"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .header("Accept", "application/json")
                        .param("slotId", "1")
                        .param("fullName", "Khách kiểm thử")
                        .param("phone", "0912345678")
                        .param("plateNumber", "51A-12345")
                        .param("vehicleType", "UNKNOWN")
                        .param("date", "not-a-date")
                        .param("startTime", "08:00")
                        .param("endTime", "09:00"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void reservationApiReturnsHelpfulErrorWhenVehicleIsNotSelected() throws Exception {
        mockMvc.perform(post("/reservations/guest")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user").roles("USER"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .header("Accept", "application/json")
                        .param("slotId", "1")
                        .param("date", "2030-01-01")
                        .param("startTime", "08:00")
                        .param("endTime", "09:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "Vui lòng chọn xe đã đăng ký trước khi đặt chỗ."));
    }

    @Test
    void reservationApiCreatesReservationFromSubmittedForm() throws Exception {
        User user = new User();
        user.setId(7L);
        when(userRepository.findByEmail("test-user")).thenReturn(Optional.of(user));
        Reservation reservation = new Reservation();
        reservation.setId(42L);
        when(reservationService.create(
                7L,
                5L,
                1L,
                LocalDate.of(2030, 1, 1),
                LocalTime.of(8, 0),
                LocalTime.of(9, 0)))
                .thenReturn(reservation);

        mockMvc.perform(post("/reservations/guest")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user").roles("USER"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .header("Accept", "application/json")
                        .param("slotId", "1")
                        .param("vehicleId", "5")
                        .param("date", "2030-01-01")
                        .param("startTime", "08:00")
                        .param("endTime", "09:00"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Yêu cầu đặt chỗ 42 đã được gửi và đang chờ xác nhận."));

        verify(reservationService).create(
                7L,
                5L,
                1L,
                LocalDate.of(2030, 1, 1),
                LocalTime.of(8, 0),
                LocalTime.of(9, 0));
    }

    @Test
    void guestReservationApiReturnsNotFoundSlotAsBadRequestJson() throws Exception {
        User user = new User();
        user.setId(7L);
        when(userRepository.findByEmail("test-user")).thenReturn(Optional.of(user));
        doThrow(new IllegalArgumentException("Không tìm thấy vị trí đỗ"))
                .when(reservationService).create(
                        anyLong(), anyLong(), anyLong(),
                        any(LocalDate.class), any(LocalTime.class), any(LocalTime.class));

        mockMvc.perform(post("/reservations/guest")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user").roles("USER"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .header("Accept", "application/json")
                        .param("slotId", "99999")
                        .param("vehicleId", "5")
                        .param("date", "2030-01-01")
                        .param("startTime", "08:00")
                        .param("endTime", "09:00"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy vị trí đỗ"));
    }

    private static ParkingSlot slot(
            String code,
            String zone,
            ParkingSlot.SlotType type,
            ParkingSlot.SlotStatus status) {
        ParkingSlot slot = new ParkingSlot();
        slot.setId(Math.abs((long) code.hashCode()));
        slot.setCode(code);
        slot.setZone(zone);
        slot.setSlotType(type);
        slot.setStatus(status);
        return slot;
    }
}

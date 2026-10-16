package com.example.SmartPark.service;

import com.example.SmartPark.model.ParkingSlot;
import com.example.SmartPark.model.Reservation;
import com.example.SmartPark.model.Vehicle;
import com.example.SmartPark.repository.ReservationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional
@EnabledIfSystemProperty(named = "smartpark.mariadb.integration", matches = "true")
class GuestReservationDatabaseVerificationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ParkingSlotService parkingSlotService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsGuestReservationUsingMariaDbRowLockAndExistingSchema() {
        ParkingSlot slot = parkingSlotService.filter("free").stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("The database has no available slot for verification"));
        String plateNumber = "SPT" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        LocalDate date = LocalDate.now().plusDays(30);

        Reservation reservation = reservationService.createGuest(
                "SmartPark Integration Test",
                "0912345678",
                plateNumber,
                Vehicle.VehicleType.CAR,
                slot.getId(),
                date,
                LocalTime.of(8, 0),
                LocalTime.of(9, 0));
        entityManager.flush();
        entityManager.clear();

        Reservation persisted = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertNotNull(persisted.getId());
        assertNull(persisted.getUser());
        assertEquals("SmartPark Integration Test", persisted.getGuestName());
        assertEquals("0912345678", persisted.getGuestPhone());
        assertEquals(plateNumber.toUpperCase(Locale.ROOT), persisted.getVehicle().getPlateNumber());
        assertEquals(slot.getId(), persisted.getSlot().getId());
        assertEquals(Reservation.Status.PENDING, persisted.getStatus());
        assertTrue(reservationRepository.existsBySlotIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                slot.getId(),
                List.of(Reservation.Status.PENDING, Reservation.Status.CONFIRMED),
                date.atTime(9, 0),
                date.atTime(8, 0)));
    }
}

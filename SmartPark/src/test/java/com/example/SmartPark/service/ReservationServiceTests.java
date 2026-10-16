package com.example.SmartPark.service;

import com.example.SmartPark.model.ParkingSlot;
import com.example.SmartPark.model.Reservation;
import com.example.SmartPark.model.User;
import com.example.SmartPark.model.Vehicle;
import com.example.SmartPark.repository.ParkingSlotRepository;
import com.example.SmartPark.repository.ReservationRepository;
import com.example.SmartPark.repository.UserRepository;
import com.example.SmartPark.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationServiceTests {

    private ReservationRepository reservationRepository;
    private ParkingSlotRepository slotRepository;
    private UserRepository userRepository;
    private VehicleRepository vehicleRepository;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        slotRepository = mock(ParkingSlotRepository.class);
        userRepository = mock(UserRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        ParkingSlotService slotService = new ParkingSlotService(slotRepository, reservationRepository);
        reservationService = new ReservationService(
                reservationRepository, slotRepository, userRepository, vehicleRepository, slotService);
    }

    @Test
    void rejectsInvalidTimeRangeBeforeLoadingRecords() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);

        assertThrows(IllegalArgumentException.class,
                () -> reservationService.create(1L, 2L, 3L, start, start));

        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void rejectsVehicleOwnedByAnotherUser() {
        User selectedUser = new User();
        selectedUser.setId(1L);
        User actualOwner = new User();
        actualOwner.setId(9L);
        Vehicle vehicle = new Vehicle();
        vehicle.setOwner(actualOwner);
        when(userRepository.findById(1L)).thenReturn(Optional.of(selectedUser));
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(vehicle));

        assertThrows(IllegalArgumentException.class,
                () -> reservationService.create(1L, 2L, 3L, futureStart(), futureEnd()));

        verify(slotRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void serializesSlotAvailabilityCheckAndRejectsOverlappingReservation() {
        User user = new User();
        Vehicle vehicle = new Vehicle();
        ParkingSlot slot = availableSlot();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(vehicle));
        when(slotRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(slot));
        when(reservationRepository.existsBySlotIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(3L), anyCollection(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> reservationService.create(1L, 2L, 3L, futureStart(), futureEnd()));

        verify(slotRepository).findByIdForUpdate(3L);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void createsPendingReservationWhenSlotIsAvailable() {
        User user = new User();
        Vehicle vehicle = new Vehicle();
        ParkingSlot slot = availableSlot();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(vehicle));
        when(slotRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(slot));
        when(reservationRepository.existsBySlotIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(3L), anyCollection(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation created = reservationService.create(1L, 2L, 3L, futureStart(), futureEnd());

        assertEquals(Reservation.Status.PENDING, created.getStatus());
        assertEquals(slot, created.getSlot());
        verify(slotRepository).findByIdForUpdate(3L);
        verify(reservationRepository).save(created);
    }

    @Test
    void createsReservationForGuestWithoutCreatingAUserAccount() {
        ParkingSlot slot = availableSlot();
        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber("51A-12345");
        vehicle.setVehicleType(Vehicle.VehicleType.CAR);
        when(slotRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(slot));
        when(reservationRepository.existsBySlotIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(3L), anyCollection(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(vehicleRepository.findByPlateNumber("51A-12345")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation created = reservationService.createGuest(
                "Nguyen Van A", "0912345678", "51a-12345", Vehicle.VehicleType.CAR,
                3L, LocalDate.now().plusDays(1), LocalTime.of(8, 0), LocalTime.of(10, 0));

        assertEquals("Nguyen Van A", created.getGuestName());
        assertEquals("0912345678", created.getGuestPhone());
        assertEquals("51A-12345", created.getVehicle().getPlateNumber());
        assertEquals("51A-12345", created.getVehicle().getLicensePlate());
        assertEquals(Reservation.Status.PENDING, created.getStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsGuestReservationForOccupiedOrMaintainedSlot() {
        ParkingSlot slot = availableSlot();
        slot.setStatus(ParkingSlot.SlotStatus.MAINTENANCE);
        when(slotRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(slot));

        assertThrows(IllegalStateException.class, () -> reservationService.createGuest(
                "Nguyen Van A", "0912345678", "51A-12345", Vehicle.VehicleType.CAR,
                3L, LocalDate.now().plusDays(1), LocalTime.of(8, 0), LocalTime.of(10, 0)));

        verify(vehicleRepository, never()).save(any(Vehicle.class));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void rejectsGuestReservationWhenAnExistingBookingOverlaps() {
        when(slotRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(availableSlot()));
        when(reservationRepository.existsBySlotIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(3L), anyCollection(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> reservationService.createGuest(
                "Nguyen Van A", "0912345678", "51A-12345", Vehicle.VehicleType.CAR,
                3L, LocalDate.now().plusDays(1), LocalTime.of(8, 0), LocalTime.of(10, 0)));

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void rejectsMalformedGuestContactDetails() {
        assertThrows(IllegalArgumentException.class, () -> reservationService.createGuest(
                "Nguyen Van A", "------", "51A-12345", Vehicle.VehicleType.CAR,
                3L, LocalDate.now().plusDays(1), LocalTime.of(8, 0), LocalTime.of(10, 0)));

        verify(slotRepository, never()).findByIdForUpdate(anyLong());
    }

    private static ParkingSlot availableSlot() {
        ParkingSlot slot = new ParkingSlot();
        slot.setId(3L);
        slot.setCode("A03");
        slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
        return slot;
    }

    private static LocalDateTime futureStart() {
        return LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
    }

    private static LocalDateTime futureEnd() {
        return futureStart().plusHours(2);
    }
}

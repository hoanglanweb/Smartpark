package com.example.SmartPark.service;

import com.example.SmartPark.model.ParkingSession;
import com.example.SmartPark.model.ParkingSlot;
import com.example.SmartPark.model.Vehicle;
import com.example.SmartPark.repository.ParkingSessionRepository;
import com.example.SmartPark.repository.ParkingSlotRepository;
import com.example.SmartPark.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParkingSessionServiceTests {

    private ParkingSessionRepository sessionRepository;
    private ParkingSlotRepository slotRepository;
    private VehicleRepository vehicleRepository;
    private ParkingSessionService parkingSessionService;
    private ParkingSlot slot;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(ParkingSessionRepository.class);
        slotRepository = mock(ParkingSlotRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        parkingSessionService = new ParkingSessionService(
                sessionRepository, slotRepository, vehicleRepository);

        slot = new ParkingSlot();
        slot.setId(1L);
        slot.setCode("A01");
        slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));

        vehicle = new Vehicle();
        vehicle.setId(2L);
        vehicle.setPlateNumber("51A12345");
        vehicle.setVehicleType(Vehicle.VehicleType.CAR);
        when(vehicleRepository.findByPlateNumber("51A12345")).thenReturn(Optional.of(vehicle));
        when(sessionRepository.findByVehicleIdAndStatus(2L, ParkingSession.Status.ACTIVE))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any(ParkingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void checkInStartsSessionAndMarksSlotOccupied() {
        ParkingSession session = parkingSessionService.checkIn(
                1L, " 51a12345 ", Vehicle.VehicleType.CAR);

        assertEquals(ParkingSession.Status.ACTIVE, session.getStatus());
        assertEquals(vehicle, session.getVehicle());
        assertEquals(slot, session.getSlot());
        assertNotNull(session.getCheckInTime());
        assertEquals(ParkingSlot.SlotStatus.OCCUPIED, slot.getStatus());
        verify(slotRepository).save(slot);
        verify(sessionRepository).save(session);
    }

    @Test
    void checkOutFinishesSessionAndFreesSlot() {
        slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
        ParkingSession session = new ParkingSession();
        session.setVehicle(vehicle);
        session.setSlot(slot);
        session.setStatus(ParkingSession.Status.ACTIVE);
        when(sessionRepository.findBySlotIdAndStatus(1L, ParkingSession.Status.ACTIVE))
                .thenReturn(Optional.of(session));

        ParkingSession finished = parkingSessionService.checkOut(1L);

        assertEquals(ParkingSession.Status.FINISHED, finished.getStatus());
        assertNotNull(finished.getCheckOutTime());
        assertEquals(ParkingSlot.SlotStatus.AVAILABLE, slot.getStatus());
        verify(slotRepository).save(slot);
        verify(sessionRepository).save(session);
    }

    @Test
    void checkInRejectsAlreadyOccupiedSlot() {
        slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);

        assertThrows(IllegalStateException.class,
                () -> parkingSessionService.checkIn(1L, "51A12345", Vehicle.VehicleType.CAR));
    }
}

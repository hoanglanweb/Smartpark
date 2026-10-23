package com.example.SmartPark.service;

import com.example.SmartPark.model.User;
import com.example.SmartPark.model.Vehicle;
import com.example.SmartPark.model.VehicleRegistrationForm;
import com.example.SmartPark.repository.UserRepository;
import com.example.SmartPark.repository.VehicleRepository;
import jakarta.persistence.JoinColumn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VehicleServiceTests {

    private VehicleRepository vehicleRepository;
    private UserRepository userRepository;
    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleRepository.class);
        userRepository = mock(UserRepository.class);
        vehicleService = new VehicleService(vehicleRepository, userRepository);
    }

    @Test
    void registersVehicleForTheSpecifiedOwnerAndNormalizesPlate() {
        User owner = new User();
        owner.setId(12L);
        when(userRepository.findById(12L)).thenReturn(Optional.of(owner));
        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        VehicleRegistrationForm form = form("  51a-12345 ", Vehicle.VehicleType.MOTORBIKE);

        Vehicle saved = vehicleService.register(12L, form);

        assertEquals("51A-12345", saved.getPlateNumber());
        assertEquals("51A-12345", saved.getLicensePlate());
        assertEquals(Vehicle.VehicleType.MOTORBIKE, saved.getVehicleType());
        assertEquals("Honda", saved.getBrand());
        assertEquals("Đen", saved.getColor());
        assertEquals(owner, saved.getOwner());
        verify(vehicleRepository).save(saved);
    }

    @Test
    void mapsVehicleOwnerToExistingUserForeignKeyColumn() throws NoSuchFieldException {
        JoinColumn joinColumn = Vehicle.class.getDeclaredField("owner").getAnnotation(JoinColumn.class);

        assertEquals("user_id", joinColumn.name());
    }

    @Test
    void rejectsPlateAlreadyRegisteredToAnyAccount() {
        when(vehicleRepository.existsByPlateNumberOrLicensePlate("51A-12345", "51A-12345"))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.register(12L, form("51a-12345", Vehicle.VehicleType.CAR)));

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void listsVehiclesOnlyForRequestedOwner() {
        Vehicle vehicle = new Vehicle();
        when(vehicleRepository.findByOwnerId(12L)).thenReturn(List.of(vehicle));

        assertEquals(List.of(vehicle), vehicleService.getByOwnerId(12L));
        verify(vehicleRepository).findByOwnerId(12L);
    }

    @Test
    void rejectsBrandLongerThanDatabaseColumnLimit() {
        VehicleRegistrationForm form = form("51A-12345", Vehicle.VehicleType.CAR);
        form.setBrand("B".repeat(81));

        assertThrows(IllegalArgumentException.class, () -> vehicleService.register(12L, form));

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    private static VehicleRegistrationForm form(String plate, Vehicle.VehicleType type) {
        VehicleRegistrationForm form = new VehicleRegistrationForm();
        form.setPlateNumber(plate);
        form.setVehicleType(type);
        form.setBrand("Honda");
        form.setColor("Đen");
        return form;
    }
}

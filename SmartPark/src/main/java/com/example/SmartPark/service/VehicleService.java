package com.example.SmartPark.service;

import com.example.SmartPark.model.User;
import com.example.SmartPark.model.Vehicle;
import com.example.SmartPark.model.VehicleRegistrationForm;
import com.example.SmartPark.repository.UserRepository;
import com.example.SmartPark.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Vehicle register(Long ownerId, VehicleRegistrationForm form) {
        if (ownerId == null || ownerId <= 0) {
            throw new IllegalArgumentException("Tài khoản đăng ký xe không hợp lệ.");
        }
        if (form == null) {
            throw new IllegalArgumentException("Vui lòng nhập thông tin xe.");
        }

        String plateNumber = form.getPlateNumber() == null
                ? ""
                : form.getPlateNumber().trim().toUpperCase(Locale.ROOT);
        if (plateNumber.isBlank() || plateNumber.length() > 20) {
            throw new IllegalArgumentException("Biển số xe là bắt buộc và tối đa 20 ký tự.");
        }
        if (form.getVehicleType() == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại xe.");
        }
        String brand = normalizeOptional(form.getBrand(), 80, "Hãng xe");
        String color = normalizeOptional(form.getColor(), 40, "Màu xe");
        if (vehicleRepository.existsByPlateNumberOrLicensePlate(plateNumber, plateNumber)) {
            throw new IllegalArgumentException("Biển số xe này đã được đăng ký.");
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản đăng ký."));
        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber(plateNumber);
        vehicle.setVehicleType(form.getVehicleType());
        vehicle.setBrand(brand);
        vehicle.setColor(color);
        vehicle.setOwner(owner);
        return vehicleRepository.save(vehicle);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getByOwnerId(Long ownerId) {
        if (ownerId == null || ownerId <= 0) {
            throw new IllegalArgumentException("Tài khoản không hợp lệ.");
        }
        return vehicleRepository.findByOwnerId(ownerId);
    }

    private String normalizeOptional(String value, int maxLength, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " tối đa " + maxLength + " ký tự.");
        }
        return normalized;
    }
}

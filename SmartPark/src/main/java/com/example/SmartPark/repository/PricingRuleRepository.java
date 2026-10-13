package com.example.SmartPark.repository;

import com.example.SmartPark.model.ParkingSlot;
import com.example.SmartPark.model.PricingRule;
import com.example.SmartPark.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    Optional<PricingRule> findByVehicleTypeAndSlotTypeAndActiveTrue(
            Vehicle.VehicleType vehicleType, ParkingSlot.SlotType slotType);
}
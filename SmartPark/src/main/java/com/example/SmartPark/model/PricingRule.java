package com.example.SmartPark.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "pricing_rules")
public class PricingRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Vehicle.VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParkingSlot.SlotType slotType = ParkingSlot.SlotType.STANDARD;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerHour;

    @Column(precision = 12, scale = 2)
    private BigDecimal maxPricePerDay;

    private boolean active = true;

    public PricingRule() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Vehicle.VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(Vehicle.VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public ParkingSlot.SlotType getSlotType() { return slotType; }
    public void setSlotType(ParkingSlot.SlotType slotType) { this.slotType = slotType; }

    public BigDecimal getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }

    public BigDecimal getMaxPricePerDay() { return maxPricePerDay; }
    public void setMaxPricePerDay(BigDecimal maxPricePerDay) { this.maxPricePerDay = maxPricePerDay; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

}

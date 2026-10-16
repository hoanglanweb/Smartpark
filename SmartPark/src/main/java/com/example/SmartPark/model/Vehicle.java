package com.example.SmartPark.model;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    public enum VehicleType { CAR, MOTORBIKE, ELECTRIC }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_number", nullable = false, unique = true, length = 20)
    private String plateNumber;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleType vehicleType = VehicleType.CAR;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    public Vehicle() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
        this.licensePlate = plateNumber;
    }
    public String getLicensePlate() { return licensePlate; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

}

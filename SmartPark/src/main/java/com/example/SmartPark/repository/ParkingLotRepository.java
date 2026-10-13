package com.example.SmartPark.repository;

import com.example.SmartPark.model.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {
    Optional<ParkingLot> findByName(String name);
}
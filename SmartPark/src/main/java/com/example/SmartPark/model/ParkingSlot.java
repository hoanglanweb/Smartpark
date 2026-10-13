package com.example.SmartPark.model;

import jakarta.persistence.*;

@Entity
@Table(name = "parking_slots", uniqueConstraints = @UniqueConstraint(columnNames = { "lot_id", "slot_code" }))
public class ParkingSlot {

    public enum SlotType {
        STANDARD, VIP, PRIORITY, CAR
    }

    public enum SlotStatus {
        AVAILABLE, RESERVED, OCCUPIED, MAINTENANCE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mã ô trong database nằm ở cột slot_code
    @Column(name = "slot_code", nullable = false, length = 20)
    private String code;

    @Column(name = "zone", nullable = false, length = 5)
    private String zone;

    @Column(name = "row_name", length = 20)
    private String rowName;

    @Column(name = "covered", nullable = false)
    private boolean covered;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type", nullable = false, length = 20)
    private SlotType slotType = SlotType.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private SlotStatus status = SlotStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private ParkingLot lot;

    public ParkingSlot() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getRowName() {
        return rowName;
    }

    public void setRowName(String rowName) {
        this.rowName = rowName;
    }

    public boolean isCovered() {
        return covered;
    }

    public void setCovered(boolean covered) {
        this.covered = covered;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    public void setSlotType(SlotType slotType) {
        this.slotType = slotType;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }

    public ParkingLot getLot() {
        return lot;
    }

    public void setLot(ParkingLot lot) {
        this.lot = lot;
    }
}
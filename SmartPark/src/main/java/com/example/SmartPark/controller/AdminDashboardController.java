package com.example.SmartPark.controller;

import com.example.SmartPark.service.ParkingSlotService;
import com.example.SmartPark.service.ParkingSlotService.Stats;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {

    private final ParkingSlotService parkingSlotService;

    public AdminDashboardController(ParkingSlotService parkingSlotService) {
        this.parkingSlotService = parkingSlotService;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        Stats stats = parkingSlotService.getStats();
        model.addAttribute("totalSlots", stats.total());
        model.addAttribute("availableSlots", stats.available());
        model.addAttribute("occupiedSlots", stats.occupied());
        return "admin/Dashboard";
    }
}

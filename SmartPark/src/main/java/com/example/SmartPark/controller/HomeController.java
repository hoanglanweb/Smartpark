package com.example.SmartPark.controller;

import com.example.SmartPark.model.ParkingSlot;
import com.example.SmartPark.repository.ParkingSlotRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;

@Controller
public class HomeController {

        private final ParkingSlotRepository parkingSlotRepository;

        public HomeController(ParkingSlotRepository parkingSlotRepository) {
                this.parkingSlotRepository = parkingSlotRepository;
        }

        @GetMapping("/")
        public String home(Model model) {

                List<ParkingSlot> allSlots = parkingSlotRepository.findAll();

                Comparator<ParkingSlot> sortByCode = Comparator.comparingInt(slot -> getNumber(slot.getCode()));

                List<ParkingSlot> khuA = allSlots.stream()
                                .filter(slot -> "A".equalsIgnoreCase(slot.getZone()))
                                .sorted(sortByCode)
                                .toList();

                // Khu A: tối đa 10 ô VIP
                List<ParkingSlot> khuAVip = khuA.stream()
                                .filter(slot -> slot.getSlotType() == ParkingSlot.SlotType.VIP)
                                .limit(10)
                                .toList();

                // Các ô còn lại của khu A
                List<ParkingSlot> khuABinhThuong = khuA.stream()
                                .filter(slot -> slot.getSlotType() != ParkingSlot.SlotType.VIP)
                                .toList();

                // Khu B: dãy giữa, mã B01 đến B28
                List<ParkingSlot> khuBGiua = allSlots.stream()
                                .filter(slot -> "B".equalsIgnoreCase(slot.getZone()))
                                .filter(slot -> {
                                        int number = getNumber(slot.getCode());
                                        return number >= 1 && number <= 28;
                                })
                                .sorted(sortByCode)
                                .toList();

                // Khu B: dãy dưới, mã B29 đến B56
                List<ParkingSlot> khuBDuoi = allSlots.stream()
                                .filter(slot -> "B".equalsIgnoreCase(slot.getZone()))
                                .filter(slot -> {
                                        int number = getNumber(slot.getCode());
                                        return number >= 29 && number <= 56;
                                })
                                .sorted(sortByCode)
                                .toList();

                long tongO = allSlots.size();

                long conTrong = allSlots.stream()
                                .filter(slot -> slot.getStatus() == ParkingSlot.SlotStatus.AVAILABLE)
                                .count();

                long daDo = allSlots.stream()
                                .filter(slot -> slot.getStatus() == ParkingSlot.SlotStatus.OCCUPIED)
                                .count();

                model.addAttribute("khuA", khuABinhThuong);
                model.addAttribute("khuAVip", khuAVip);
                model.addAttribute("khuBGiua", khuBGiua);
                model.addAttribute("khuBDuoi", khuBDuoi);

                model.addAttribute("tongO", tongO);
                model.addAttribute("conTrong", conTrong);
                model.addAttribute("daDo", daDo);

                return "Home";
        }

        private int getNumber(String code) {
                if (code == null) {
                        return Integer.MAX_VALUE;
                }

                String number = code.replaceAll("[^0-9]", "");

                if (number.isEmpty()) {
                        return Integer.MAX_VALUE;
                }

                try {
                        return Integer.parseInt(number);
                } catch (NumberFormatException e) {
                        return Integer.MAX_VALUE;
                }
        }
}
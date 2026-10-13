package com.example.SmartPark.controller;

import com.example.SmartPark.model.ParkingSlot;
import com.example.SmartPark.repository.ParkingSlotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@WebMvcTest(controllers = HomeController.class)
class HomeTemplateRenderingTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParkingSlotRepository parkingSlotRepository;

    @Test
    void rendersBackendSlotsAndStatsWithoutTemplateBindings() throws Exception {
        when(parkingSlotRepository.findAll()).thenReturn(List.of(
                slot("A01", "A", ParkingSlot.SlotType.STANDARD, ParkingSlot.SlotStatus.AVAILABLE),
                slot("A02", "A", ParkingSlot.SlotType.VIP, ParkingSlot.SlotStatus.RESERVED),
                slot("A03", "A", ParkingSlot.SlotType.PRIORITY, ParkingSlot.SlotStatus.OCCUPIED),
                slot("B01", "B", ParkingSlot.SlotType.STANDARD, ParkingSlot.SlotStatus.AVAILABLE),
                slot("B29", "B", ParkingSlot.SlotType.CAR, ParkingSlot.SlotStatus.MAINTENANCE)));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("Home"))
                .andExpect(content().string(containsString("id=\"stat-total\">5</strong>")))
                .andExpect(content().string(containsString("id=\"stat-available\">2</strong>")))
                .andExpect(content().string(containsString("id=\"stat-occupied\">1</strong>")))
                .andExpect(content().string(containsString("data-code=\"A02\"")))
                .andExpect(content().string(containsString("data-code=\"B29\"")))
                .andExpect(content().string(containsString("Đã đặt")))
                .andExpect(content().string(containsString("Bảo trì")))
                .andExpect(content().string(not(containsString("th:each"))));
    }

    private static ParkingSlot slot(
            String code,
            String zone,
            ParkingSlot.SlotType type,
            ParkingSlot.SlotStatus status) {
        ParkingSlot slot = new ParkingSlot();
        slot.setCode(code);
        slot.setZone(zone);
        slot.setSlotType(type);
        slot.setStatus(status);
        return slot;
    }
}

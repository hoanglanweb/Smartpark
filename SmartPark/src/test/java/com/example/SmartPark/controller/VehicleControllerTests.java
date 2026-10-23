package com.example.SmartPark.controller;

import com.example.SmartPark.model.User;
import com.example.SmartPark.model.Vehicle;
import com.example.SmartPark.repository.UserRepository;
import com.example.SmartPark.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(controllers = VehicleController.class)
@WithMockUser(username = "owner@example.com", roles = "USER")
class VehicleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleService vehicleService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void rendersRegistrationFormWithReturnSlot() throws Exception {
        mockMvc.perform(get("/vehicles/new")
                        .param("slotId", "24"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicles/Register"))
                .andExpect(content().string(containsString("name=\"slotId\"")))
                .andExpect(content().string(containsString("vehicle-registration-layout")))
                .andExpect(content().string(containsString("vehicle-registration-form")))
                .andExpect(content().string(containsString("name=\"plateNumber\"")))
                .andExpect(content().string(containsString("name=\"vehicleType\"")))
                .andExpect(content().string(containsString("name=\"brand\"")))
                .andExpect(content().string(containsString("name=\"color\"")));
    }

    @Test
    void registersVehicleForAuthenticatedAccountAndReturnsToBookingForm() throws Exception {
        User owner = new User();
        owner.setId(12L);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        mockMvc.perform(post("/vehicles")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("slotId", "24")
                        .param("plateNumber", "51A-12345")
                        .param("brand", "Honda")
                        .param("color", "Đen")
                        .param("vehicleType", "CAR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/reservations/new?slotId=24"))
                .andExpect(flash().attribute(
                        "vehicleRegistrationSuccess",
                        "Đăng ký xe thành công. Bạn có thể chọn xe này để đặt chỗ."));

        verify(vehicleService).register(eq(12L), argThat(
                form -> "51A-12345".equals(form.getPlateNumber())
                        && "Honda".equals(form.getBrand())
                        && "Đen".equals(form.getColor())
                        && form.getVehicleType() == Vehicle.VehicleType.CAR
                        && Long.valueOf(24L).equals(form.getSlotId())));
    }
}

package com.example.SmartPark.controller;

import com.example.SmartPark.config.SecurityConfig;
import com.example.SmartPark.repository.UserRepository;
import com.example.SmartPark.repository.VehicleRepository;
import com.example.SmartPark.service.ParkingSlotService;
import com.example.SmartPark.service.ParkingSessionService;
import com.example.SmartPark.service.ReservationService;
import com.example.SmartPark.service.RegistrationService;
import com.example.SmartPark.service.SmartParkUserDetailsService;
import com.example.SmartPark.service.ParkingSlotService.Stats;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {
        AuthController.class,
        PublicController.class,
        AdminDashboardController.class,
        HomeController.class
})
@EnableWebSecurity
@Import(SecurityConfig.class)
class SecurityFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private ParkingSlotService parkingSlotService;

    @MockitoBean
    private ParkingSessionService parkingSessionService;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private SmartParkUserDetailsService userDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private VehicleRepository vehicleRepository;

    @Test
    void publicLandingAndAuthenticationPagesRender() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("Landing"));
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/Login"));
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/Register"));
    }

    @Test
    void anonymousAdminRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void regularUserCannotOpenAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("customer").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanOpenDashboard() throws Exception {
        when(parkingSlotService.getStats()).thenReturn(new Stats(12, 8, 4));

        mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/Dashboard"));
    }

    @Test
    void customerCanOpenParkingMapWithoutSeeingGlobalReservations() throws Exception {
        stubHomePage();

        mockMvc.perform(get("/user/parking").with(user("customer").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("Home"))
                .andExpect(content().string(not(containsString("id=\"reservations\""))));

        verify(reservationService, never()).getRecent();
    }

    @Test
    void adminSeesOperationsAndReservationOverviewOnParkingMap() throws Exception {
        stubHomePage();
        when(reservationService.getRecent()).thenReturn(List.of());

        mockMvc.perform(get("/user/parking").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"reservations\"")))
                .andExpect(content().string(containsString("id=\"check-in-dialog\"")));
    }

    @Test
    void loginRedirectsUserAndAdminToTheirOwnAreas() throws Exception {
        String password = "StrongPass123!";
        when(userDetailsService.loadUserByUsername("customer@example.com"))
                .thenReturn(userDetails("customer@example.com", password, "ROLE_USER"));
        when(userDetailsService.loadUserByUsername("admin@example.com"))
                .thenReturn(userDetails("admin@example.com", password, "ROLE_ADMIN"));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "customer@example.com")
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/parking"));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "admin@example.com")
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void loginPostRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "customer@example.com")
                        .param("password", "StrongPass123!"))
                .andExpect(status().isForbidden());
    }

    private UserDetails userDetails(String username, String rawPassword, String authority) {
        return User.withUsername(username)
                .password(passwordEncoder.encode(rawPassword))
                .authorities(authority)
                .build();
    }

    private void stubHomePage() {
        when(parkingSlotService.filter("all")).thenReturn(List.of());
        when(parkingSlotService.filter("free")).thenReturn(List.of());
        when(parkingSlotService.getStats()).thenReturn(new Stats(0, 0, 0));
    }
}

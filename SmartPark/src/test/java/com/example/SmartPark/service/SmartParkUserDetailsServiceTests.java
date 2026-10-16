package com.example.SmartPark.service;

import com.example.SmartPark.model.User;
import com.example.SmartPark.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartParkUserDetailsServiceTests {

    @Mock
    private UserRepository userRepository;

    @Test
    void mapsLegacyStaffAccountsToRegularUserRole() {
        User staff = user(User.Role.STAFF, "ACTIVE");
        when(userRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staff));

        UserDetails details = new SmartParkUserDetailsService(userRepository)
                .loadUserByUsername(" Staff@Example.com ");

        assertTrue(details.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
        assertTrue(details.isEnabled());
        verify(userRepository).findByEmail("staff@example.com");
    }

    @Test
    void disablesAccountsThatAreNotActive() {
        User disabled = user(User.Role.CUSTOMER, "DISABLED");
        when(userRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(disabled));

        UserDetails details = new SmartParkUserDetailsService(userRepository)
                .loadUserByUsername("staff@example.com");

        assertFalse(details.isEnabled());
    }

    private static User user(User.Role role, String status) {
        User user = new User();
        user.setEmail("staff@example.com");
        user.setPasswordHash("encoded");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}

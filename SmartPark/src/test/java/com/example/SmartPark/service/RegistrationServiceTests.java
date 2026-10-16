package com.example.SmartPark.service;

import com.example.SmartPark.model.RegisterForm;
import com.example.SmartPark.model.User;
import com.example.SmartPark.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTests {

    @Mock
    private UserRepository userRepository;

    private RegistrationService registrationService;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        registrationService = new RegistrationService(userRepository, passwordEncoder);
    }

    @Test
    void registersCustomerWithNormalizedEmailAndEncodedPassword() {
        when(userRepository.existsByEmail("customer@example.com")).thenReturn(false);
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User registered = registrationService.register(validForm());

        assertEquals("customer@example.com", registered.getEmail());
        assertEquals(User.Role.CUSTOMER, registered.getRole());
        assertEquals("ACTIVE", registered.getStatus());
        assertTrue(passwordEncoder.matches("StrongPass123!", registered.getPasswordHash()));
        assertFalse(registered.getPasswordHash().equals("StrongPass123!"));
    }

    @Test
    void rejectsDuplicateEmailBeforeSaving() {
        when(userRepository.existsByEmail("customer@example.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(validForm()));

        assertEquals("Email này đã được đăng ký.", exception.getMessage());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    private static RegisterForm validForm() {
        RegisterForm form = new RegisterForm();
        form.setFullName(" Khách SmartPark ");
        form.setEmail(" Customer@Example.com ");
        form.setPhone("0912345678");
        form.setPassword("StrongPass123!");
        form.setConfirmPassword("StrongPass123!");
        return form;
    }
}

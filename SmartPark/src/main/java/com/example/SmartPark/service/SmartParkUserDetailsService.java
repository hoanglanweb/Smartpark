package com.example.SmartPark.service;

import com.example.SmartPark.model.User;
import com.example.SmartPark.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class SmartParkUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public SmartParkUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản."));

        String authority = user.getRole() == User.Role.ADMIN ? "ROLE_ADMIN" : "ROLE_USER";
        boolean active = "ACTIVE".equalsIgnoreCase(user.getStatus());
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authority)
                .disabled(!active)
                .build();
    }
}

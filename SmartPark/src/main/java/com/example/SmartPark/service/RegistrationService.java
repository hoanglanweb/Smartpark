package com.example.SmartPark.service;

import com.example.SmartPark.model.RegisterForm;
import com.example.SmartPark.model.User;
import com.example.SmartPark.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterForm form) {
        String fullName = form.getFullName() == null ? "" : form.getFullName().trim();
        String email = form.getEmail() == null
                ? ""
                : form.getEmail().trim().toLowerCase(Locale.ROOT);
        String phone = form.getPhone() == null || form.getPhone().isBlank()
                ? null
                : form.getPhone().trim();
        String password = form.getPassword() == null ? "" : form.getPassword();

        if (fullName.isBlank() || fullName.length() > 100) {
            throw new IllegalArgumentException("Họ tên là bắt buộc và tối đa 100 ký tự.");
        }
        if (email.length() > 120 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Địa chỉ email không hợp lệ.");
        }
        if (phone != null && (phone.length() > 20 || !phone.matches("[+0-9() .-]{7,20}"))) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email này đã được đăng ký.");
        }
        if (password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự và tối đa 72 byte UTF-8.");
        }
        if (!password.equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(User.Role.CUSTOMER);
        user.setStatus("ACTIVE");
        return userRepository.save(user);
    }
}

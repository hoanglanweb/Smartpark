package com.example.SmartPark.controller;

import com.example.SmartPark.model.RegisterForm;
import com.example.SmartPark.service.RegistrationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final RegistrationService registrationService;

    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/Login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "auth/Register";
    }
    
    //?: tiếp nhận dữ liệu người dùng đăng ký
    @PostMapping("/register")
    public String register(
            @ModelAttribute("registerForm") RegisterForm form,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            //!: gọi service để kiểm tra dữ liệu và tài khoản. 
            registrationService.register(form);
            redirectAttributes.addFlashAttribute(
                    "registrationSuccess", "Tạo tài khoản thành công. Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("registerError", exception.getMessage());
            return "auth/Register";
        } catch (DataIntegrityViolationException exception) {
            model.addAttribute(
                    "registerError", "Email hoặc số điện thoại này đã được đăng ký.");
            return "auth/Register";
        }
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "auth/AccessDenied";
    }
}

package com.example.SmartPark.controller;

import com.example.SmartPark.model.User;
import com.example.SmartPark.model.VehicleRegistrationForm;
import com.example.SmartPark.repository.UserRepository;
import com.example.SmartPark.service.VehicleService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class VehicleController {

    private final VehicleService vehicleService;
    private final UserRepository userRepository;

    public VehicleController(VehicleService vehicleService, UserRepository userRepository) {
        this.vehicleService = vehicleService;
        this.userRepository = userRepository;
    }

    @GetMapping("/vehicles/new")
    public String vehicleRegistrationForm(
            @RequestParam(required = false) Long slotId,
            Model model) {
        if (!model.containsAttribute("vehicleRegistrationForm")) {
            VehicleRegistrationForm form = new VehicleRegistrationForm();
            form.setSlotId(slotId);
            model.addAttribute("vehicleRegistrationForm", form);
        }
        return "vehicles/Register";
    }

    @PostMapping("/vehicles")
    public String registerVehicle(
            @ModelAttribute("vehicleRegistrationForm") VehicleRegistrationForm form,
            Model model,
            RedirectAttributes redirectAttributes) {
        Long slotId = form.getSlotId();
        try {
            User owner = currentUser();
            vehicleService.register(owner.getId(), form);
            redirectAttributes.addFlashAttribute(
                    "vehicleRegistrationSuccess",
                    "Đăng ký xe thành công. Bạn có thể chọn xe này để đặt chỗ.");
            return reservationRedirect(slotId);
        } catch (IllegalArgumentException exception) {
            model.addAttribute("vehicleRegistrationError", exception.getMessage());
            return "vehicles/Register";
        } catch (DataIntegrityViolationException exception) {
            model.addAttribute(
                    "vehicleRegistrationError",
                    "Biển số xe này đã được đăng ký. Vui lòng kiểm tra lại.");
            return "vehicles/Register";
        }
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để đăng ký xe.");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản đăng nhập."));
    }

    private String reservationRedirect(Long slotId) {
        return slotId == null
                ? "redirect:/user/parking"
                : "redirect:/reservations/new?slotId=" + slotId;
    }
}

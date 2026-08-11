package com.witbank.carwash.controller;

import com.witbank.carwash.repository.StaffRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class StaffAuthController {

    @Autowired private StaffRepository staffRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping("/staff/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            @RequestParam(required = false) String disabled,
                            HttpSession session, Model model) {
        if (session.getAttribute("staffUser") != null) {
            var s = (com.witbank.carwash.model.Staff) session.getAttribute("staffUser");
            return "ADMIN".equalsIgnoreCase(s.getRole())
                    ? "redirect:/admin/dashboard" : "redirect:/staff/dashboard";
        }
        if (error    != null) model.addAttribute("error",   "Invalid username or password.");
        if (disabled != null) model.addAttribute("error",   "This account has been disabled.");
        if (logout   != null) model.addAttribute("message", "You have been signed out.");
        return "staff_login";
    }

    @PostMapping("/staff/login")
    public String doLogin(@RequestParam String username, @RequestParam String password,
                          HttpSession session) {
        return staffRepository.findByUsername(username)
                .filter(s -> passwordEncoder.matches(password, s.getPassword()))
                .map(s -> {
                    if (!s.isActive()) return "redirect:/staff/login?disabled";
                    session.setAttribute("staffUser", s);
                    session.setMaxInactiveInterval(3600);
                    // ADMIN → full dashboard, STAFF → simplified panel
                    return "ADMIN".equalsIgnoreCase(s.getRole())
                            ? "redirect:/admin/dashboard"
                            : "redirect:/staff/dashboard";
                })
                .orElse("redirect:/staff/login?error");
    }

    @GetMapping("/staff/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/staff/login?logout";
    }
}

package com.witbank.carwash.controller;

import com.witbank.carwash.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/customer")
public class CustomerAuthController {

    @Autowired private CustomerService customerService;

    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (session.getAttribute("customerUser") != null) return "redirect:/customer/dashboard";
        return "customer_register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String fullName, @RequestParam String email,
                             @RequestParam String cellphone, @RequestParam String password,
                             @RequestParam String confirmPassword,
                             Model model, HttpSession session) {
        if (fullName.isBlank() || email.isBlank() || cellphone.isBlank() || password.isBlank()) {
            model.addAttribute("error", "All fields are required."); return "customer_register";
        }
        if (password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters."); return "customer_register";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match."); return "customer_register";
        }
        if (customerService.emailTaken(email)) {
            model.addAttribute("error", "An account with that email already exists."); return "customer_register";
        }
        session.setAttribute("customerUser", customerService.register(fullName, email, cellphone, password));
        return "redirect:/customer/dashboard";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            HttpSession session, Model model) {
        if (session.getAttribute("customerUser") != null) return "redirect:/customer/dashboard";
        if (error  != null) model.addAttribute("error",   "Invalid email or password.");
        if (logout != null) model.addAttribute("message", "You have been signed out.");
        return "customer_login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email, @RequestParam String password,
                          HttpSession session) {
        return customerService.authenticate(email, password)
                .map(c -> { session.setAttribute("customerUser", c); return "redirect:/customer/dashboard"; })
                .orElse("redirect:/customer/login?error");
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/customer/login?logout";
    }
}

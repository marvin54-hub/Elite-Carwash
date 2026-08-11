package com.witbank.carwash.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;

@Controller
public class GlobalExceptionHandler implements ErrorController {

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object statusCode = request.getAttribute("jakarta.servlet.error.status_code");
        Object message    = request.getAttribute("jakarta.servlet.error.message");
        model.addAttribute("status",       statusCode != null ? statusCode : 500);
        model.addAttribute("errorMessage", message    != null ? message    : "An unexpected error occurred.");
        return "error";
    }
}

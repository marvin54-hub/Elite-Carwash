package com.witbank.carwash.controller;

import com.witbank.carwash.model.Booking;
import com.witbank.carwash.model.Customer;
import com.witbank.carwash.repository.BookingRepository;
import com.witbank.carwash.service.BookingService;
import com.witbank.carwash.service.CustomerService;
import com.witbank.carwash.service.YocoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Autowired private CustomerService  customerService;
    @Autowired private BookingService   bookingService;
    @Autowired private YocoService      yocoService;
    @Autowired private BookingRepository bookingRepository;

    private Customer req(HttpSession s) { return (Customer) s.getAttribute("customerUser"); }

    // ── Dashboard ─────────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session,
                            @ModelAttribute("passwordError")   String passwordError,
                            @ModelAttribute("passwordSuccess") String passwordSuccess,
                            @ModelAttribute("bookingMsg")      String bookingMsg) {
        Customer c = req(session);
        if (c == null) return "redirect:/customer/login";
        c = customerService.findById(c.getId()).orElse(c);
        session.setAttribute("customerUser", c);

        List<Booking> bookings = customerService.getBookingHistory(c.getId());
        Map<Long, Boolean> feedbackGiven = new HashMap<>();
        bookings.forEach(b -> feedbackGiven.put(b.getId(), customerService.hasFeedback(b.getId())));

        model.addAttribute("customerUser",    c);
        model.addAttribute("vehicles",        customerService.getVehicles(c.getId()));
        model.addAttribute("bookings",        bookings);
        model.addAttribute("feedbackGiven",   feedbackGiven);
        model.addAttribute("vehicleLabels",   customerService.getVehicleLabelsByBookings(bookings));
        model.addAttribute("services",        bookingService.getServices());
        model.addAttribute("yocoConfigured",  yocoService.isConfigured());
        if (passwordError   != null && !passwordError.isEmpty())
            model.addAttribute("passwordError", passwordError);
        if (passwordSuccess != null && !passwordSuccess.isEmpty())
            model.addAttribute("passwordSuccess", passwordSuccess);
        if (bookingMsg      != null && !bookingMsg.isEmpty())
            model.addAttribute("bookingMsg", bookingMsg);
        return "customer_dashboard";
    }

    // ── Profile ───────────────────────────────────────────────────────────────
    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String fullName, @RequestParam String cellphone,
                                HttpSession session) {
        Customer c = req(session);
        if (c == null) return "redirect:/customer/login";
        customerService.updateProfile(c.getId(), fullName, cellphone);
        customerService.findById(c.getId()).ifPresent(u -> session.setAttribute("customerUser", u));
        return "redirect:/customer/dashboard#profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session, RedirectAttributes ra) {
        Customer c = req(session);
        if (c == null) return "redirect:/customer/login";
        String error = customerService.changePassword(c, currentPassword, newPassword, confirmPassword);
        if (error != null) ra.addFlashAttribute("passwordError", error);
        else {
            customerService.findById(c.getId()).ifPresent(u -> session.setAttribute("customerUser", u));
            ra.addFlashAttribute("passwordSuccess", "Password updated successfully.");
        }
        return "redirect:/customer/dashboard#profile";
    }

    // ── Vehicles ──────────────────────────────────────────────────────────────
    @PostMapping("/vehicles/add")
    public String addVehicle(@RequestParam String make, @RequestParam String model_,
                             @RequestParam String regPlate,
                             @RequestParam(defaultValue = "") String color,
                             HttpSession session) {
        Customer c = req(session);
        if (c == null) return "redirect:/customer/login";
        if (!make.isBlank() && !model_.isBlank() && !regPlate.isBlank())
            customerService.addVehicle(c.getId(), make, model_, regPlate, color);
        return "redirect:/customer/dashboard#vehicles";
    }

    @PostMapping("/vehicles/delete")
    public String deleteVehicle(@RequestParam Long id, HttpSession session) {
        Customer c = req(session);
        if (c == null) return "redirect:/customer/login";
        customerService.deleteVehicle(c.getId(), id);
        return "redirect:/customer/dashboard#vehicles";
    }

    // ── Bookings — Edit & Delete ───────────────────────────────────────────────
    @PostMapping("/bookings/reschedule")
    public String rescheduleBooking(@RequestParam Long bookingId,
                                    @RequestParam String newTime,
                                    HttpSession session, RedirectAttributes ra) {
        Customer c = req(session);
        if (c == null) return "redirect:/customer/login";
        bookingRepository.findById(bookingId).ifPresent(b -> {
            // Ownership check
            if (!c.getId().equals(b.getCustomerId())) return;
            // Cannot reschedule completed/cancelled
            if ("Completed".equalsIgnoreCase(b.getStatus()) ||
                "Cancelled".equalsIgnoreCase(b.getStatus())) return;
            try {
                LocalDateTime newDt = LocalDateTime.parse(newTime);
                if (bookingService.isSlotAvailable(newDt)) {
                    b.setBookingTime(newDt);
                    bookingRepository.save(b);
                }
            } catch (Exception ignored) {}
        });
        ra.addFlashAttribute("bookingMsg", "Booking rescheduled successfully.");
        return "redirect:/customer/dashboard#bookings";
    }

    @PostMapping("/bookings/cancel")
    public String cancelBooking(@RequestParam Long bookingId,
                                HttpSession session, RedirectAttributes ra) {
        Customer c = req(session);
        if (c == null) return "redirect:/customer/login";
        bookingRepository.findById(bookingId).ifPresent(b -> {
            if (!c.getId().equals(b.getCustomerId())) return;
            if ("Completed".equalsIgnoreCase(b.getStatus())) return;
            b.setStatus("Cancelled");
            bookingRepository.save(b);
        });
        ra.addFlashAttribute("bookingMsg", "Booking cancelled.");
        return "redirect:/customer/dashboard#bookings";
    }

    @PostMapping("/bookings/delete")
    public String deleteBooking(@RequestParam Long bookingId,
                                HttpSession session, RedirectAttributes ra) {
        Customer c = req(session);
        if (c == null) return "redirect:/customer/login";
        bookingRepository.findById(bookingId).ifPresent(b -> {
            if (!c.getId().equals(b.getCustomerId())) return;
            // Only allow deletion if Cancelled
            if ("Cancelled".equalsIgnoreCase(b.getStatus())) {
                bookingRepository.delete(b);
            }
        });
        ra.addFlashAttribute("bookingMsg", "Booking deleted.");
        return "redirect:/customer/dashboard#bookings";
    }

    // ── Feedback ──────────────────────────────────────────────────────────────
    @PostMapping("/feedback/add")
    public String addFeedback(@RequestParam Long bookingId, @RequestParam int rating,
                              @RequestParam(defaultValue = "") String comment,
                              HttpSession session) {
        Customer c = req(session);
        if (c == null) return "redirect:/customer/login";
        bookingService.getAllBookings().stream()
                .filter(b -> b.getId().equals(bookingId)).findFirst()
                .ifPresent(b -> customerService.addFeedback(c.getId(), bookingId,
                        c.getFullName(), c.getEmail(), b.getServiceType(), rating, comment));
        return "redirect:/customer/dashboard#bookings";
    }

    // ── Payment ───────────────────────────────────────────────────────────────
    @PostMapping("/payment/pay")
    public String pay(@RequestParam Long bookingId, @RequestParam double amount,
                      @RequestParam String method, HttpSession session) {
        if (req(session) == null) return "redirect:/customer/login";
        customerService.recordSimulatedPayment(bookingId, amount, method);
        return "redirect:/customer/dashboard#bookings";
    }

    // ── Loyalty ───────────────────────────────────────────────────────────────
    @PostMapping("/loyalty/redeem")
    public String redeemLoyalty(HttpSession session) {
        Customer c = req(session);
        if (c == null) return "redirect:/customer/login";
        int pts = customerService.redeemPoints(c.getId());
        if (pts >= 0) { c.setLoyaltyPoints(pts); session.setAttribute("customerUser", c); }
        return "redirect:/customer/dashboard#loyalty";
    }
}

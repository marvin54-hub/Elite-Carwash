package com.witbank.carwash.controller;

import com.witbank.carwash.model.Staff;
import com.witbank.carwash.repository.CustomerRepository;
import com.witbank.carwash.repository.FeedbackRepository;
import com.witbank.carwash.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private BookingService      bookingService;
    @Autowired private NotificationService notificationService;
    @Autowired private CustomerService     customerService;
    @Autowired private StaffService        staffService;
    @Autowired private FeedbackRepository  feedbackRepository;
    @Autowired private CustomerRepository  customerRepository;

    private Staff me(HttpSession s)          { return (Staff) s.getAttribute("staffUser"); }
    private boolean notLoggedIn(HttpSession s) { return me(s) == null; }
    private boolean notAdmin(HttpSession s) {
        Staff st = me(s);
        return st == null || !"ADMIN".equalsIgnoreCase(st.getRole());
    }
    private String redirect(String anchor)   { return "redirect:/admin/dashboard" + anchor; }

    // ── Dashboard ─────────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(Model model, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        Staff staff   = me(session);
        boolean admin = !notAdmin(session);

        model.addAttribute("staffUser",             staff);
        model.addAttribute("isAdmin",               admin);
        model.addAttribute("bookings",              admin ? bookingService.getAllBookings()
                                                          : bookingService.getBookingsForStaff(staff.getId()));
        model.addAttribute("staffList",             staffService.getAll());
        model.addAttribute("allStaff",              staffService.getAll());
        model.addAttribute("totalRevenue",          bookingService.getTotalRevenue());
        model.addAttribute("totalBookings",         (long) bookingService.getAllBookings().size());
        model.addAttribute("inventory",             bookingService.getInventory());
        long lowStockCount = bookingService.getInventory().stream().filter(com.witbank.carwash.model.InventoryItem::isLowStock).count();
        model.addAttribute("lowStockCount",          lowStockCount);
        model.addAttribute("services",              bookingService.getServices());
        model.addAttribute("customerStats",         bookingService.getCustomerVisitCount());
        model.addAttribute("notifications",         notificationService.getAllDispatchedLogs());
        model.addAttribute("feedbackList",          customerService.getAllFeedback());
        model.addAttribute("avgRating",             customerService.getAverageRating());
        model.addAttribute("weekSchedule",          bookingService.getScheduleForWeek(LocalDate.now()));

        // These maps must never be null — admin.html uses #maps.isEmpty() checks on them
        Map<String, Double>  revMap = bookingService.getRevenueByService();
        Map<String, Long>    cntMap = bookingService.getBookingCountByService();
        model.addAttribute("revenueByService",      revMap != null ? revMap : new HashMap<>());
        model.addAttribute("bookingCountByService", cntMap != null ? cntMap : new HashMap<>());

        var customers = customerService.getAllCustomers();
        model.addAttribute("allCustomers", customers != null ? customers : List.of());
        if (admin && customers != null) {
            Map<Long, Integer> vcnt = new HashMap<>(), bcnt = new HashMap<>();
            customers.forEach(c -> {
                vcnt.put(c.getId(), customerService.getVehicleCount(c.getId()));
                bcnt.put(c.getId(), customerService.getBookingCount(c.getId()));
            });
            model.addAttribute("custVehicleCounts", vcnt);
            model.addAttribute("custBookingCounts", bcnt);
        } else {
            model.addAttribute("custVehicleCounts", new HashMap<>());
            model.addAttribute("custBookingCounts", new HashMap<>());
        }
        return "admin";
    }

    // ── Analytics JSON ────────────────────────────────────────────────────────
    @GetMapping("/analytics/data")
    @ResponseBody
    public Map<String, Object> analyticsData(HttpSession session) {
        if (notLoggedIn(session)) return Map.of("error", "Unauthorized");
        return Map.of("revenueByService",  bookingService.getRevenueByService(),
                      "bookingsByService", bookingService.getBookingCountByService());
    }

    // ── QR Verification ───────────────────────────────────────────────────────
    @GetMapping("/verify")
    public String verifyPage(@RequestParam(required = false) String code,
                             Model model, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (code != null && !code.isBlank()) {
            String raw = code.trim().toUpperCase().replace("WE-", "");
            try {
                bookingService.getAllBookings().stream()
                        .filter(b -> b.getId().equals(Long.parseLong(raw))).findFirst()
                        .ifPresentOrElse(b -> model.addAttribute("found", b),
                                         () -> model.addAttribute("notFound", true));
            } catch (NumberFormatException e) { model.addAttribute("notFound", true); }
        }
        model.addAttribute("staffUser", me(session));
        return "staff_verify";
    }

    @PostMapping("/verify/confirm")
    public String confirmArrival(@RequestParam Long id, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        bookingService.markVerified(id);
        return "redirect:/admin/verify?code=WE-" + id;
    }

    // ── Bookings ──────────────────────────────────────────────────────────────
    @PostMapping("/bookings/update-status")
    public String updateStatus(@RequestParam Long id, @RequestParam String status, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        bookingService.updateBookingStatus(id, status);
        return redirect("#bookings-section");
    }

    @PostMapping("/bookings/delete")
    public String deleteBooking(@RequestParam Long id, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        bookingService.deleteBooking(id);
        return redirect("#bookings-section");
    }

    @PostMapping("/bookings/assign")
    public String assignBooking(@RequestParam Long id,
                                @RequestParam(required = false) Long staffId, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        bookingService.assignStaff(id, staffId);
        return redirect("#bookings-section");
    }

    // ── Staff Management (alias /admin/staff/* for B's template) ─────────────
    @PostMapping("/staff/add")
    public String addStaff(@RequestParam String username, @RequestParam String password,
                           @RequestParam String fullName, @RequestParam String role,
                           HttpSession session, RedirectAttributes ra) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        if (username.isBlank() || password.length() < 6 || fullName.isBlank()) {
            ra.addFlashAttribute("employeeError", "All fields required; password ≥ 6 characters.");
            return redirect("#staff-section");
        }
        if (staffService.usernameTaken(username.trim())) {
            ra.addFlashAttribute("employeeError", "Username already taken.");
            return redirect("#staff-section");
        }
        staffService.register(username, password, fullName, role);
        return redirect("#staff-section");
    }

    @PostMapping("/staff/change-password")
    public String changePassword(@RequestParam Long id, @RequestParam String newPassword,
                                 HttpSession session, RedirectAttributes ra) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        if (newPassword.length() < 6) {
            ra.addFlashAttribute("employeeError", "Password must be at least 6 characters.");
            return redirect("#staff-section");
        }
        staffService.resetPassword(id, newPassword);
        return redirect("#staff-section");
    }

    @PostMapping("/staff/delete")
    public String deleteStaff(@RequestParam Long id, HttpSession session, RedirectAttributes ra) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        if (staffService.findById(id).filter(s ->
                "ADMIN".equalsIgnoreCase(s.getRole()) && staffService.countActiveAdmins() <= 1).isPresent()) {
            ra.addFlashAttribute("employeeError", "Cannot remove the last active administrator.");
            return redirect("#staff-section");
        }
        staffService.setActive(id, false);
        return redirect("#staff-section");
    }

    @PostMapping("/employees/availability")
    public String selfAvailability(@RequestParam boolean onLeave, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        Staff s = me(session);
        staffService.setOnLeave(s.getId(), onLeave);
        s.setOnLeave(onLeave);
        session.setAttribute("staffUser", s);
        return redirect("");
    }

    // ── Customers ─────────────────────────────────────────────────────────────
    @PostMapping("/customers/edit")
    public String editCustomer(@RequestParam Long id, @RequestParam String fullName,
                               @RequestParam String cellphone, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        customerRepository.findById(id).ifPresent(c -> {
            c.setFullName(fullName.trim()); c.setCellphone(cellphone.trim());
            customerRepository.save(c);
        });
        return redirect("#customers-section");
    }

    @PostMapping("/customers/delete")
    public String deleteCustomer(@RequestParam Long id, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        customerService.deleteCustomer(id);
        return redirect("#customers-section");
    }

    // ── Services & Pricing ────────────────────────────────────────────────────
    @PostMapping("/services/add")
    public String addService(@RequestParam String name, @RequestParam String description,
                             @RequestParam double price,
                             @RequestParam(defaultValue = "🚗") String icon, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        bookingService.addService(name, description, price, icon);
        return redirect("#services-section");
    }

    @PostMapping("/services/update")
    public String updateService(@RequestParam String id, @RequestParam String name,
                                @RequestParam String description, @RequestParam double price,
                                @RequestParam(defaultValue = "🚗") String icon, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        bookingService.updateService(id, name, description, price, icon);
        return redirect("#services-section");
    }

    @PostMapping("/services/delete")
    public String deleteService(@RequestParam String id, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        bookingService.deleteService(id);
        return redirect("#services-section");
    }

    // ── Inventory ─────────────────────────────────────────────────────────────
    @PostMapping("/inventory/add")
    public String addInventory(@RequestParam String itemName, @RequestParam int quantity,
                               @RequestParam int threshold, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        bookingService.addInventoryItem(itemName, quantity, threshold);
        return redirect("#inventory-section");
    }

    @PostMapping("/inventory/update")
    public String updateInventory(@RequestParam Long id, @RequestParam int quantity,
                                  @RequestParam int threshold, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        bookingService.updateInventory(id, quantity, threshold);
        return redirect("#inventory-section");
    }

    @PostMapping("/inventory/delete")
    public String deleteInventory(@RequestParam Long id, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        bookingService.deleteInventory(id);
        return redirect("#inventory-section");
    }

    // ── Notifications ─────────────────────────────────────────────────────────
    @PostMapping("/notifications/dispatch")
    public String dispatch(@RequestParam String recipient, @RequestParam String type,
                           @RequestParam String messageContent, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        if ("SMS".equalsIgnoreCase(type)) notificationService.dispatchSms(recipient, messageContent);
        else notificationService.dispatchEmail(recipient, "Notice – Witbank Elite", messageContent);
        return redirect("#notifications-section");
    }

    // ── Schedule ──────────────────────────────────────────────────────────────
    @PostMapping("/schedule/add")
    public String addSchedule(@RequestParam Long staffId,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              @RequestParam String shiftStart, @RequestParam String shiftEnd,
                              @RequestParam(defaultValue = "") String notes, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        bookingService.addSchedule(staffId, date, shiftStart, shiftEnd, notes);
        return redirect("#schedule-section");
    }

    @PostMapping("/schedule/delete")
    public String deleteSchedule(@RequestParam Long id, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        bookingService.deleteSchedule(id);
        return redirect("#schedule-section");
    }

    // ── Feedback ──────────────────────────────────────────────────────────────
    @PostMapping("/feedback/delete")
    public String deleteFeedback(@RequestParam Long id, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        feedbackRepository.deleteById(id);
        return redirect("#feedback-section");
    }

    // ── Report export ─────────────────────────────────────────────────────────
    @GetMapping("/export")
    public String export(Model model, HttpSession session) {
        if (notLoggedIn(session)) return "redirect:/staff/login";
        if (notAdmin(session))    return redirect("?denied");
        model.addAttribute("bookings",          bookingService.getAllBookings());
        model.addAttribute("totalRevenue",      bookingService.getTotalRevenue());
        model.addAttribute("totalBookings",     (long) bookingService.getAllBookings().size());
        model.addAttribute("revenueByService",  bookingService.getRevenueByService());
        model.addAttribute("bookingsByService", bookingService.getBookingCountByService());
        model.addAttribute("avgRating",         customerService.getAverageRating());
        model.addAttribute("date",              LocalDate.now());
        return "report_print";
    }
}

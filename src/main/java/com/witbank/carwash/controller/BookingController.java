package com.witbank.carwash.controller;

import com.witbank.carwash.model.Customer;
import com.witbank.carwash.service.BookingService;
import com.witbank.carwash.service.CustomerService;
import com.witbank.carwash.service.QrCodeService;
import com.witbank.carwash.service.WeatherService;
import com.witbank.carwash.service.YocoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class BookingController {

    @Autowired private BookingService  bookingService;
    @Autowired private CustomerService customerService;
    @Autowired private QrCodeService   qrCodeService;
    @Autowired private YocoService     yocoService;
    @Autowired private WeatherService  weatherService;

    private Customer loggedIn(HttpSession s) {
        return (Customer) s.getAttribute("customerUser");
    }

    private void commonModel(Model model, HttpSession session) {
        Customer c = loggedIn(session);
        model.addAttribute("services",         bookingService.getServices());
        model.addAttribute("addOns",           bookingService.getActiveAddOns());
        model.addAttribute("publicReviews",    customerService.getAllFeedback().stream().limit(6).toList());
        model.addAttribute("avgRating",        customerService.getAverageRating());
        model.addAttribute("yocoConfigured",   yocoService.isConfigured());
        model.addAttribute("customerUser",     c);
        model.addAttribute("loggedInCustomer", c);
        model.addAttribute("isVip",            c != null && bookingService.isVipCustomer(c.getEmail()));
        model.addAttribute("myVehicles",       c != null ? customerService.getVehicles(c.getId()) : List.of());
        model.addAttribute("paymentStatus",    "");
        model.addAttribute("discountApplied",  false);
        model.addAttribute("bookedVehicle",    "");
    }

    // ── Homepage ──────────────────────────────────────────────────────────────
    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        commonModel(model, session);
        return "index";
    }

    // ── Book a wash ───────────────────────────────────────────────────────────
    @PostMapping("/book")
    public String book(
            @RequestParam String name,
            @RequestParam String cellphone,
            @RequestParam String email,
            @RequestParam String service,
            @RequestParam String time,
            @RequestParam(required = false, defaultValue = "") String vehicleReg,
            @RequestParam(required = false, defaultValue = "") String vehicleId,
            @RequestParam(required = false) List<Long> selectedAddOns,
            Model model, HttpSession session) {

        commonModel(model, session);
        Customer customer = loggedIn(session);

        // ── Validation ────────────────────────────────────────────────────────
        if (name.isBlank() || cellphone.isBlank() || email.isBlank()) {
            model.addAttribute("formError", "Please fill in all required fields.");
            return "index";
        }
        if (!cellphone.matches("^(\\+27\\d{9}|0\\d{9})$")) {
            model.addAttribute("formError", "Enter a valid SA cellphone number, e.g. 0812345678.");
            return "index";
        }

        // ── Slot availability ─────────────────────────────────────────────────
        // parsedTime must be effectively final for use in lambdas below
        LocalDateTime parsedTime;
        try {
            parsedTime = LocalDateTime.parse(time);
        } catch (Exception e) {
            model.addAttribute("formError", "Please select a valid date and time slot.");
            return "index";
        }
        // now parsedTime is effectively final (no reassignment after this point)
        if (!bookingService.isSlotAvailable(parsedTime)) {
            model.addAttribute("formError",
                    "That slot is fully booked (" + bookingService.getBayCapacity()
                    + " bays). Please choose another time.");
            return "index";
        }

        // ── Resolve IDs — must be effectively final for lambdas ───────────────
        final Long custId;
        custId = (customer != null) ? customer.getId() : null;

        final Long finalVidId;
        Long parsedVidId = null;
        try {
            if (!vehicleId.isBlank()) parsedVidId = Long.parseLong(vehicleId);
        } catch (NumberFormatException ignored) {}
        finalVidId = parsedVidId;

        // ── Pricing & Add-Ons ──────────────────────────────────────────────────
        double basePrice = bookingService.getServices().stream()
                .filter(s -> s.getName().equals(service))
                .findFirst()
                .map(s -> s.getPrice())
                .orElse(0.0);

        double addOnTotal = 0.0;
        List<String> addOnList = new java.util.ArrayList<>();
        if (selectedAddOns != null && !selectedAddOns.isEmpty()) {
            var activeAddOns = bookingService.getActiveAddOns();
            for (Long addOnId : selectedAddOns) {
                activeAddOns.stream()
                        .filter(a -> a.getId().equals(addOnId))
                        .findFirst()
                        .ifPresent(a -> {
                            addOnList.add(a.getName() + " (R" + (int)a.getPrice() + ")");
                        });
            }
            addOnTotal = activeAddOns.stream()
                    .filter(a -> selectedAddOns.contains(a.getId()))
                    .mapToDouble(a -> a.getPrice())
                    .sum();
        }
        String addOnString = String.join(", ", addOnList);
        double totalBeforeDiscount = basePrice + addOnTotal;
        final double price    = bookingService.applyLoyaltyDiscount(email, totalBeforeDiscount);
        final boolean discount = price < totalBeforeDiscount;

        // ── Vehicle label for confirmation display ────────────────────────────
        String label = "";
        if (finalVidId != null && custId != null) {
            label = customerService.getVehicles(custId).stream()
                    .filter(v -> v.getId().equals(finalVidId))
                    .findFirst()
                    .map(v -> v.getDisplayLabel())
                    .orElse("");
        } else if (!vehicleReg.isBlank()) {
            label = vehicleReg.trim().toUpperCase();
        }
        final String vehicleLabel = label;

        // ── Save booking ──────────────────────────────────────────────────────
        var booking = bookingService.addBooking(
                name.trim(), cellphone.trim(), email.trim(),
                service, time, price, custId, finalVidId, vehicleReg, addOnString);

        // ── Confirmation model ────────────────────────────────────────────────
        commonModel(model, session);
        model.addAttribute("message",         "Thank you, " + name + "! Your "
                                              + service + " on " + time.replace("T", " ")
                                              + " is confirmed.");
        model.addAttribute("bookedName",      name.trim());
        model.addAttribute("bookedEmail",     email.trim());
        model.addAttribute("bookedCellphone", cellphone.trim());
        model.addAttribute("bookedService",   service);
        model.addAttribute("bookedTime",      time);
        model.addAttribute("bookedPrice",     String.format("%.2f", price));
        model.addAttribute("bookedVehicle",   vehicleLabel);
        model.addAttribute("bookingId",       booking.getId());
        model.addAttribute("paymentStatus",   "Pay on Arrival");
        model.addAttribute("discountApplied", discount);
        return "index";
    }

    // ── Real-time slot availability API ───────────────────────────────────────
    @GetMapping("/api/availability")
    @ResponseBody
    public Map<String, Object> availability(
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String date) {

        Map<String, Object> r = new HashMap<>();

        if (time != null && !time.isBlank()) {
            try {
                r.put("available", bookingService.isSlotAvailable(LocalDateTime.parse(time)));
                r.put("bays", bookingService.getBayCapacity());
            } catch (Exception e) {
                r.put("available", true);
            }
            return r;
        }

        if (date != null && !date.isBlank()) {
            String[] hours = {"08:00","09:00","10:00","11:00","12:00",
                              "13:00","14:00","15:00","16:00","17:00"};
            List<Map<String, Object>> slots = new ArrayList<>();
            for (String h : hours) {
                try {
                    // slot is effectively final inside the loop iteration
                    LocalDateTime slot = LocalDateTime.parse(date + "T" + h);
                    long used = bookingService.getAllBookings().stream()
                            .filter(b -> b.getBookingTime().equals(slot)
                                      && !"Cancelled".equalsIgnoreCase(b.getStatus()))
                            .count();
                    int left = (int) (bookingService.getBayCapacity() - used);
                    slots.add(Map.of(
                            "time",      h,
                            "available", left > 0,
                            "left",      Math.max(0, left)));
                } catch (Exception ignored) {}
            }
            r.put("slots", slots);
            return r;
        }

        r.put("available", true);
        return r;
    }

    // ── QR code image ─────────────────────────────────────────────────────────
    @GetMapping(value = "/booking/qr/{id}", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> qrCode(@PathVariable Long id) {
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qrCodeService.generatePng("WE-" + id, 220));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Printable booking slip ────────────────────────────────────────────────
    @GetMapping("/booking/slip/{id}")
    public String slip(@PathVariable Long id, Model model) {
        return bookingService.getAllBookings().stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .map(b -> {
                    model.addAttribute("booking",        b);
                    model.addAttribute("yocoConfigured", yocoService.isConfigured());
                    return "booking_slip";
                })
                .orElse("redirect:/");
    }

    // ── Guest simulated payment ───────────────────────────────────────────────
    @PostMapping("/book/pay")
    public String paySimulated(@RequestParam Long bookingId,
                               @RequestParam double amount,
                               @RequestParam String method) {
        customerService.recordSimulatedPayment(bookingId, amount, method);
        return "redirect:/booking/slip/" + bookingId;
    }

    // ── Guest feedback ────────────────────────────────────────────────────────
    @PostMapping("/book/feedback")
    public String feedbackGuest(@RequestParam Long bookingId,
                                @RequestParam String name,
                                @RequestParam(defaultValue = "") String email,
                                @RequestParam int rating,
                                @RequestParam(defaultValue = "") String comment) {
        bookingService.getAllBookings().stream()
                .filter(b -> b.getId().equals(bookingId))
                .findFirst()
                .ifPresent(b -> customerService.addFeedback(
                        null, bookingId, name, email, b.getServiceType(), rating, comment));
        return "redirect:/booking/slip/" + bookingId;
    }

    // ── Vehicle service history lookup ────────────────────────────────────────
    @GetMapping("/vehicle/history")
    public String vehicleHistory(@RequestParam(required = false) String reg, Model model) {
        if (reg != null && !reg.isBlank()) {
            model.addAttribute("reg",     reg.trim().toUpperCase());
            model.addAttribute("history", bookingService.getBookingsByVehicleReg(reg));
        }
        return "vehicle_history";
    }
}

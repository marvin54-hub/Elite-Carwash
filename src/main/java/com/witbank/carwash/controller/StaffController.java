package com.witbank.carwash.controller;

import com.witbank.carwash.model.Staff;
import com.witbank.carwash.service.BookingService;
import com.witbank.carwash.service.StaffService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequestMapping("/staff")
public class StaffController {

    @Autowired private BookingService bookingService;
    @Autowired private StaffService   staffService;

    private Staff me(HttpSession s)      { return (Staff) s.getAttribute("staffUser"); }
    private boolean notStaff(HttpSession s) { return me(s) == null; }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(Model model, HttpSession session) {
        if (notStaff(session)) return "redirect:/staff/login";
        Staff staff = me(session);
        // If ADMIN accidentally hits /staff/dashboard, redirect them properly
        if ("ADMIN".equalsIgnoreCase(staff.getRole())) return "redirect:/admin/dashboard";

        model.addAttribute("staffUser",      staff);
        model.addAttribute("myBookings",     bookingService.getBookingsForStaff(staff.getId()));
        model.addAttribute("allBookings",    bookingService.getAllBookings()); // for unassigned view
        model.addAttribute("services",       bookingService.getServices());
        model.addAttribute("inspectionsMap", bookingService.getInspectionsMap());
        return "staff_dashboard";
    }

    @PostMapping("/bookings/update-status")
    public String updateStatus(@RequestParam Long id, @RequestParam String status,
                               @RequestParam(required = false) String serviceNotes,
                               HttpSession session) {
        if (notStaff(session)) return "redirect:/staff/login";
        bookingService.updateBookingStatusAndNotes(id, status, serviceNotes);
        return "redirect:/staff/dashboard#bookings";
    }

    @PostMapping("/inspection/save")
    public String saveInspection(@RequestParam Long bookingId,
                                 @RequestParam(required = false, defaultValue = "") String vehicleReg,
                                 @RequestParam String conditionRating,
                                 @RequestParam(required = false, defaultValue = "") String existingDamageNotes,
                                 @RequestParam(required = false, defaultValue = "") String photoUrls,
                                 @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile,
                                 HttpSession session) {
        if (notStaff(session)) return "redirect:/staff/login";
        Staff staff = me(session);

        String finalPhotoUrl = photoUrls;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String filename = "inspection_" + bookingId + "_" + System.currentTimeMillis() + "_"
                        + imageFile.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
                java.nio.file.Path targetUploadDir = java.nio.file.Paths.get("target/classes/static/uploads/inspections");
                if (!java.nio.file.Files.exists(targetUploadDir)) {
                    java.nio.file.Files.createDirectories(targetUploadDir);
                }
                java.nio.file.Path srcUploadDir = java.nio.file.Paths.get("src/main/resources/static/uploads/inspections");
                if (!java.nio.file.Files.exists(srcUploadDir)) {
                    java.nio.file.Files.createDirectories(srcUploadDir);
                }
                java.nio.file.Files.copy(imageFile.getInputStream(), targetUploadDir.resolve(filename), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                try {
                    java.nio.file.Files.copy(targetUploadDir.resolve(filename), srcUploadDir.resolve(filename), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {}
                finalPhotoUrl = "/uploads/inspections/" + filename;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        bookingService.saveInspection(bookingId, vehicleReg, conditionRating, existingDamageNotes, finalPhotoUrl, staff.getFullName());
        return "redirect:/staff/dashboard#bookings";
    }

    @PostMapping("/availability")
    public String toggleAvailability(@RequestParam boolean onLeave, HttpSession session) {
        if (notStaff(session)) return "redirect:/staff/login";
        Staff s = me(session);
        staffService.setOnLeave(s.getId(), onLeave);
        s.setOnLeave(onLeave);
        session.setAttribute("staffUser", s);
        return "redirect:/staff/dashboard";
    }
}

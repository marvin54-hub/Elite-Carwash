package com.witbank.carwash.service;

import com.witbank.carwash.model.*;
import com.witbank.carwash.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired private BookingRepository        bookingRepository;
    @Autowired private ServicePackageRepository servicePackageRepository;
    @Autowired private InventoryItemRepository  inventoryItemRepository;
    @Autowired private StaffRepository          staffRepository;
    @Autowired private CustomerRepository       customerRepository;
    @Autowired private FeedbackRepository       feedbackRepository;
    @Autowired private StaffScheduleRepository  scheduleRepository;
    @Autowired private NotificationService      notificationService;
    @Autowired private PasswordEncoder          passwordEncoder;
    @Autowired private com.witbank.carwash.repository.AddOnRepository addOnRepository;
    @Autowired private com.witbank.carwash.repository.VehicleInspectionRepository inspectionRepository;

    @Value("${carwash.bays:2}")
    private int bays;

    // ── Seed data ─────────────────────────────────────────────────────────────
    @PostConstruct
    public void init() {
        if (staffRepository.count() == 0) {
            staffRepository.save(new Staff("admin",  passwordEncoder.encode("admin123"), "System Administrator", "ADMIN"));
            staffRepository.save(new Staff("staff1", passwordEncoder.encode("staff123"), "Car Wash Attendant",   "STAFF"));
        }
        if (servicePackageRepository.count() == 0) {
            servicePackageRepository.save(new ServicePackage("express",  "Express Shine",    "Quick exterior wash, wheel clean and hand dry.",            150.0, "🚿"));
            servicePackageRepository.save(new ServicePackage("elite",    "Elite Detail",     "Full interior & exterior, wax protection, engine bay.",      450.0, "✨"));
            servicePackageRepository.save(new ServicePackage("platinum", "Platinum Ceramic", "Ultra-deep clean + ceramic coating for lasting shine.",      850.0, "💎"));
        }
        if (inventoryItemRepository.count() == 0) {
            inventoryItemRepository.save(new InventoryItem("Car Shampoo",       50, 10));
            inventoryItemRepository.save(new InventoryItem("Wax Polish",        20,  5));
            inventoryItemRepository.save(new InventoryItem("Microfiber Cloths",100, 20));
            inventoryItemRepository.save(new InventoryItem("Tire Shine",          5,  8));
        }
        if (addOnRepository.count() == 0) {
            addOnRepository.save(new com.witbank.carwash.model.AddOn("Tyre Shine & Dressing", 40.0, "High-gloss tire dressing for long-lasting dark shine."));
            addOnRepository.save(new com.witbank.carwash.model.AddOn("Engine Bay Wash", 150.0, "Degrease, high-pressure rinse & protective shine for engine."));
            addOnRepository.save(new com.witbank.carwash.model.AddOn("Leather Conditioning", 120.0, "Deep clean and nourish leather upholstery."));
            addOnRepository.save(new com.witbank.carwash.model.AddOn("Air Freshener & Deodorizer", 25.0, "Long-lasting fresh car scent treatment."));
            addOnRepository.save(new com.witbank.carwash.model.AddOn("Headlight Restoration", 180.0, "Restore cloudy or oxidized headlight lenses."));
        }
    }

    // ── Real-time slot availability ───────────────────────────────────────────
    public boolean isSlotAvailable(LocalDateTime time) {
        long active = bookingRepository.findByBookingTime(time).stream()
                .filter(b -> !"Cancelled".equalsIgnoreCase(b.getStatus()))
                .count();
        return active < bays;
    }
    public int getBayCapacity() { return bays; }

    // ── Bookings ──────────────────────────────────────────────────────────────
    public Booking addBooking(String name, String cellphone, String email,
                              String serviceName, String time, double price,
                              Long customerId, Long vehicleId, String vehicleReg) {
        return addBooking(name, cellphone, email, serviceName, time, price, customerId, vehicleId, vehicleReg, null);
    }

    public Booking addBooking(String name, String cellphone, String email,
                              String serviceName, String time, double price,
                              Long customerId, Long vehicleId, String vehicleReg,
                              String selectedAddOns) {

        Booking b = new Booking(null, name, cellphone, email,
                serviceName, LocalDateTime.parse(time), price);
        b.setCustomerId(customerId);
        b.setVehicleId(vehicleId);
        if (vehicleReg != null && !vehicleReg.isBlank())
            b.setVehicleReg(vehicleReg.trim().toUpperCase());
        if (selectedAddOns != null && !selectedAddOns.isBlank())
            b.setSelectedAddOns(selectedAddOns.trim());
        b = bookingRepository.save(b);

        // Deduct one Car Shampoo unit per wash
        inventoryItemRepository.findByName("Car Shampoo").ifPresent(item -> {
            if (item.getQuantity() > 0) {
                item.setQuantity(item.getQuantity() - 1);
                inventoryItemRepository.save(item);
            }
        });

        String when = time.replace("T", " ");
        notificationService.dispatchSms(cellphone,
                "Witbank Elite: Hi " + name + ", your " + serviceName
                + " booking on " + when + " is CONFIRMED. Thank you!");
        notificationService.dispatchEmail(email, "Booking Confirmed – " + serviceName,
                "Hi " + name + ", your " + serviceName + " on " + when
                + " is confirmed. Amount: R" + price);

        return b;
    }

    public List<Booking> getAllBookings()                    { return bookingRepository.findAll(); }
    public List<Booking> getBookingsForStaff(Long staffId)  { return bookingRepository.findByAssignedStaffId(staffId); }

    public List<Booking> getBookingsByVehicleReg(String reg) {
        return bookingRepository.findAll().stream()
                .filter(b -> reg.equalsIgnoreCase(b.getVehicleReg()))
                .collect(Collectors.toList());
    }

    public void updateBookingStatus(Long id, String status) {
        updateBookingStatusAndNotes(id, status, null);
    }

    public void updateBookingStatusAndNotes(Long id, String status, String serviceNotes) {
        bookingRepository.findById(id).ifPresent(b -> {
            boolean justCompleted = "Completed".equalsIgnoreCase(status)
                    && !"Completed".equalsIgnoreCase(b.getStatus());
            b.setStatus(status);
            if (serviceNotes != null) {
                b.setServiceNotes(serviceNotes);
            }
            bookingRepository.save(b);

            if (justCompleted) {
                notificationService.dispatchSms(b.getCellphone(),
                        "Witbank Elite: Hi " + b.getCustomerName()
                        + ", your vehicle is ready! " + b.getServiceType() + " COMPLETED.");
                notificationService.dispatchEmail(b.getEmail(),
                        "Service Completed – Witbank Elite",
                        "Hi " + b.getCustomerName() + ", your " + b.getServiceType()
                        + " is complete. Please collect your vehicle.");

                // Award loyalty points (1 pt per R10) to linked customer account
                if (b.getCustomerId() != null) {
                    customerRepository.findById(b.getCustomerId()).ifPresent(c -> {
                        c.setLoyaltyPoints(c.getLoyaltyPoints() + (int)(b.getPrice() / 10));
                        customerRepository.save(c);
                    });
                }
            }
        });
    }

    public void deleteBooking(Long id)               { bookingRepository.deleteById(id); }

    public void assignStaff(Long bookingId, Long staffId) {
        bookingRepository.findById(bookingId).ifPresent(b -> {
            b.setAssignedStaffId(staffId);
            bookingRepository.save(b);
        });
    }

    public void markVerified(Long bookingId) {
        bookingRepository.findById(bookingId).ifPresent(b -> {
            b.setVerified(true);
            if ("Pending".equalsIgnoreCase(b.getStatus())) b.setStatus("In Progress");
            bookingRepository.save(b);
        });
    }

    public void recordPayment(Long bookingId, String paymentStatus, String paymentRef) {
        bookingRepository.findById(bookingId).ifPresent(b -> {
            b.setPaymentStatus(paymentStatus);
            b.setPaymentRef(paymentRef);
            bookingRepository.save(b);
        });
    }

    // ── Services / Pricing ────────────────────────────────────────────────────
    public List<ServicePackage> getServices() { return servicePackageRepository.findAll(); }

    public void addService(String name, String description, double price, String icon) {
        String base = name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        String id = base.isBlank() ? "service" : base;
        int n = 1;
        while (servicePackageRepository.existsById(id)) id = base + "-" + (++n);
        servicePackageRepository.save(new ServicePackage(id, name.trim(), description.trim(), price, icon));
    }

    public void updateService(String id, String name, String description, double price, String icon) {
        servicePackageRepository.findById(id).ifPresent(s -> {
            s.setName(name); s.setDescription(description); s.setPrice(price); s.setIcon(icon);
            servicePackageRepository.save(s);
        });
    }

    public void deleteService(String id) { servicePackageRepository.deleteById(id); }

    // ── Inventory ─────────────────────────────────────────────────────────────
    public List<InventoryItem> getInventory() { return inventoryItemRepository.findAll(); }

    public void addInventoryItem(String name, int quantity, int threshold) {
        if (inventoryItemRepository.findByName(name).isEmpty())
            inventoryItemRepository.save(new InventoryItem(name, quantity, threshold));
    }

    public void updateInventory(Long id, int quantity, int threshold) {
        inventoryItemRepository.findById(id).ifPresent(item -> {
            item.setQuantity(quantity); item.setThreshold(threshold);
            inventoryItemRepository.save(item);
        });
    }

    public void deleteInventory(Long id) { inventoryItemRepository.deleteById(id); }

    // ── Staff management ──────────────────────────────────────────────────────
    public List<Staff> getAllStaff() { return staffRepository.findAll(); }

    // ── Customer lookup (for admin) ───────────────────────────────────────────
    public List<com.witbank.carwash.model.Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    // ── Feedback ──────────────────────────────────────────────────────────────
    public List<com.witbank.carwash.model.Feedback> getAllFeedback() {
        return feedbackRepository.findAllByOrderBySubmittedAtDesc();
    }

    public double getAverageRating() {
        List<com.witbank.carwash.model.Feedback> all = feedbackRepository.findAll();
        if (all.isEmpty()) return 0;
        return all.stream().mapToInt(com.witbank.carwash.model.Feedback::getRating).average().orElse(0);
    }

    // ── Staff schedule ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<StaffSchedule> getScheduleForWeek(LocalDate from) {
        return scheduleRepository.findByWorkDateBetweenOrderByWorkDateAscShiftStartAsc(from, from.plusDays(6));
    }

    public void addSchedule(Long staffId, LocalDate date, String start, String end, String notes) {
        staffRepository.findById(staffId).ifPresent(s ->
            scheduleRepository.save(new StaffSchedule(s, date, start, end, notes)));
    }

    public void deleteSchedule(Long id) { scheduleRepository.deleteById(id); }

    // ── Analytics ─────────────────────────────────────────────────────────────
    public double getTotalRevenue() {
        return bookingRepository.findAll().stream().mapToDouble(Booking::getPrice).sum();
    }

    public Map<String, Long> getCustomerVisitCount() {
        return bookingRepository.findAll().stream()
                .collect(Collectors.groupingBy(Booking::getCustomerName, Collectors.counting()));
    }

    public Map<String, Double> getRevenueByService() {
        return bookingRepository.findAll().stream()
                .collect(Collectors.groupingBy(Booking::getServiceType,
                        Collectors.summingDouble(Booking::getPrice)));
    }

    public Map<String, Long> getBookingCountByService() {
        return bookingRepository.findAll().stream()
                .collect(Collectors.groupingBy(Booking::getServiceType, Collectors.counting()));
    }

    /** VIP: 3+ completed bookings → 10% discount on next booking. */
    public boolean isVipCustomer(String email) {
        return bookingRepository.findAll().stream()
                .filter(b -> email.equalsIgnoreCase(b.getEmail())
                          && "Completed".equalsIgnoreCase(b.getStatus()))
                .count() >= 3;
    }

    public double applyLoyaltyDiscount(String email, double price) {
        return isVipCustomer(email) ? Math.round(price * 0.90 * 100.0) / 100.0 : price;
    }

    // ── Add-Ons Management ───────────────────────────────────────────────────
    public List<com.witbank.carwash.model.AddOn> getAllAddOns() { return addOnRepository.findAll(); }
    public List<com.witbank.carwash.model.AddOn> getActiveAddOns() { return addOnRepository.findByActiveTrue(); }
    public void saveAddOn(com.witbank.carwash.model.AddOn item) { addOnRepository.save(item); }
    public void toggleAddOnStatus(Long id) {
        addOnRepository.findById(id).ifPresent(item -> {
            item.setActive(!item.isActive());
            addOnRepository.save(item);
        });
    }
    public void deleteAddOn(Long id) { addOnRepository.deleteById(id); }

    // ── Vehicle Inspection Management ─────────────────────────────────────────
    public java.util.Optional<com.witbank.carwash.model.VehicleInspection> getInspectionForBooking(Long bookingId) {
        return inspectionRepository.findByBookingId(bookingId);
    }

    public void saveInspection(Long bookingId, String vehicleReg, String rating,
                               String damageNotes, String photoUrls, String staffName) {
        var existing = inspectionRepository.findByBookingId(bookingId).orElseGet(() -> {
            var ins = new com.witbank.carwash.model.VehicleInspection();
            ins.setBookingId(bookingId);
            return ins;
        });
        existing.setVehicleReg(vehicleReg);
        existing.setConditionRating(rating);
        existing.setExistingDamageNotes(damageNotes);
        existing.setPhotoUrls(photoUrls);
        existing.setInspectedByStaff(staffName);
        existing.setInspectedAt(java.time.LocalDateTime.now());
        inspectionRepository.save(existing);
    }

    public java.util.Map<Long, com.witbank.carwash.model.VehicleInspection> getInspectionsMap() {
        return inspectionRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.witbank.carwash.model.VehicleInspection::getBookingId,
                        ins -> ins, (a, b) -> b));
    }
}

package com.witbank.carwash.service;

import com.witbank.carwash.model.*;
import com.witbank.carwash.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CustomerService {

    @Autowired private CustomerRepository  customerRepository;
    @Autowired private VehicleRepository   vehicleRepository;
    @Autowired private FeedbackRepository  feedbackRepository;
    @Autowired private PaymentRepository   paymentRepository;
    @Autowired private BookingRepository   bookingRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private PasswordEncoder     passwordEncoder;
    @Autowired private com.witbank.carwash.repository.SupportTicketRepository ticketRepository;

    public static final int    REDEEM_THRESHOLD = 100;
    public static final double REDEEM_VALUE      = 50.0;

    // ── Registration / Auth ───────────────────────────────────────────────────
    public boolean emailTaken(String email) { return customerRepository.existsByEmail(email.trim().toLowerCase()); }

    public Customer register(String fullName, String email, String cellphone, String password) {
        return customerRepository.save(new Customer(
                fullName.trim(), email.trim().toLowerCase(),
                cellphone.trim(), passwordEncoder.encode(password)));
    }

    public Optional<Customer> authenticate(String email, String password) {
        return customerRepository.findByEmail(email.trim().toLowerCase())
                .filter(c -> passwordEncoder.matches(password, c.getPassword()));
    }

    public Optional<Customer> findById(Long id) { return customerRepository.findById(id); }

    public List<Customer> getAllCustomers()      { return customerRepository.findAll(); }

    public void updateProfile(Long id, String fullName, String cellphone) {
        customerRepository.findById(id).ifPresent(c -> {
            c.setFullName(fullName.trim());
            c.setCellphone(cellphone.trim());
            customerRepository.save(c);
        });
    }

    /** Returns null on success, or an error message string on failure. */
    public String changePassword(Customer customer, String current, String newPass, String confirm) {
        if (!passwordEncoder.matches(current, customer.getPassword())) return "Current password is incorrect.";
        if (newPass == null || newPass.length() < 6) return "New password must be at least 6 characters.";
        if (!newPass.equals(confirm)) return "New passwords do not match.";
        customerRepository.findById(customer.getId()).ifPresent(c -> {
            c.setPassword(passwordEncoder.encode(newPass));
            customerRepository.save(c);
        });
        return null; // success
    }

    public void deleteCustomer(Long id) { customerRepository.deleteById(id); }

    // ── Vehicles ──────────────────────────────────────────────────────────────
    public List<Vehicle> getVehicles(Long customerId) { return vehicleRepository.findByCustomerId(customerId); }

    public void addVehicle(Long customerId, String make, String model, String regPlate, String color) {
        vehicleRepository.save(new Vehicle(customerId, make.trim(), model.trim(),
                regPlate.trim().toUpperCase(), color));
    }

    public void deleteVehicle(Long customerId, Long vehicleId) {
        vehicleRepository.findById(vehicleId)
                .filter(v -> v.getCustomerId().equals(customerId))
                .ifPresent(vehicleRepository::delete);
    }

    public void saveVehicle(Vehicle vehicle) {
        vehicleRepository.save(vehicle);
    }

    public Map<Long, String> getVehicleLabelsByBookings(List<Booking> bookings) {
        Map<Long, String> labels = new HashMap<>();
        bookings.stream()
                .filter(b -> b.getVehicleId() != null)
                .forEach(b -> vehicleRepository.findById(b.getVehicleId())
                        .ifPresent(v -> labels.put(b.getId(), v.getDisplayLabel())));
        return labels;
    }

    // ── Booking history ───────────────────────────────────────────────────────
    public List<Booking> getBookingHistory(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByBookingTimeDesc(customerId);
    }

    public Optional<Booking> getBookingById(Long id) { return bookingRepository.findById(id); }

    // ── Feedback ──────────────────────────────────────────────────────────────
    public boolean hasFeedback(Long bookingId) { return feedbackRepository.findByBookingId(bookingId).isPresent(); }

    public boolean addFeedback(Long customerId, Long bookingId,
                               String name, String email, String serviceType,
                               int rating, String comment) {
        if (bookingId != null && hasFeedback(bookingId)) return false;
        int r = Math.max(1, Math.min(5, rating));
        Feedback f = new Feedback(name, email, serviceType, r, comment);
        f.setBookingId(bookingId);
        f.setCustomerId(customerId);
        feedbackRepository.save(f);
        return true;
    }

    public List<Feedback>  getAllFeedback()    { return feedbackRepository.findAllByOrderBySubmittedAtDesc(); }
    public void            deleteFeedback(Long id) { feedbackRepository.deleteById(id); }
    public double          getAverageRating() {
        List<Feedback> all = feedbackRepository.findAll();
        return all.isEmpty() ? 0 : all.stream().mapToInt(Feedback::getRating).average().orElse(0);
    }

    // ── Payments ──────────────────────────────────────────────────────────────
    public void recordSimulatedPayment(Long bookingId, double amount, String method) {
        String status = "CASH_ON_SITE".equals(method) ? "Cash on Site – Pending" : "Paid";
        String ref    = "SIM-" + System.currentTimeMillis();
        Payment p = new Payment(bookingId, amount, method, status, ref);
        paymentRepository.save(p);
        if (!"CASH_ON_SITE".equals(method)) {
            updateBookingPayment(bookingId, status, ref);
        }
    }

    public void recordYocoPayment(Long bookingId, double amount, String checkoutId) {
        Payment p = new Payment(bookingId, amount, "YOCO", "Paid", checkoutId);
        paymentRepository.save(p);
        updateBookingPayment(bookingId, "Paid", checkoutId);
    }

    private void updateBookingPayment(Long bookingId, String status, String ref) {
        bookingRepository.findById(bookingId).ifPresent(b -> {
            b.setPaymentStatus(status);
            b.setPaymentRef(ref);
            bookingRepository.save(b);
        });
    }

    public int getVehicleCount(Long customerId) { return vehicleRepository.findByCustomerId(customerId).size(); }
    public int getBookingCount(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByBookingTimeDesc(customerId).size();
    }

    // ── Loyalty redemption ────────────────────────────────────────────────────
    public int redeemPoints(Long customerId) {
        return customerRepository.findById(customerId).map(c -> {
            if (c.getLoyaltyPoints() < REDEEM_THRESHOLD) return -1;
            c.setLoyaltyPoints(c.getLoyaltyPoints() - REDEEM_THRESHOLD);
            customerRepository.save(c);
            String code = "WELITE-" + (System.currentTimeMillis() % 100000);
            notificationService.dispatchEmail(c.getEmail(),
                    "Loyalty Reward Redeemed – Witbank Elite",
                    "Hi " + c.getFullName() + ", you redeemed " + REDEEM_THRESHOLD
                    + " points for a R" + (int) REDEEM_VALUE
                    + " voucher. Code: " + code + ". Show at your next visit.");
            return c.getLoyaltyPoints();
        }).orElse(-1);
    }

    // ── Support Tickets ───────────────────────────────────────────────────────
    public com.witbank.carwash.model.SupportTicket submitTicket(Long customerId, String name,
                                                                 String phone, String email,
                                                                 String subject, String message) {
        var t = new com.witbank.carwash.model.SupportTicket(customerId, name.trim(), phone.trim(), email.trim(), subject.trim(), message.trim());
        return ticketRepository.save(t);
    }

    public List<com.witbank.carwash.model.SupportTicket> getTicketsForCustomer(Long customerId) {
        return ticketRepository.findByCustomerIdOrderBySubmittedAtDesc(customerId);
    }

    public List<com.witbank.carwash.model.SupportTicket> getAllTickets() {
        return ticketRepository.findAllByOrderBySubmittedAtDesc();
    }

    public void respondToTicket(Long ticketId, String response) {
        ticketRepository.findById(ticketId).ifPresent(t -> {
            t.setAdminResponse(response.trim());
            t.setStatus("Answered");
            t.setRespondedAt(java.time.LocalDateTime.now());
            ticketRepository.save(t);
            if (t.getEmail() != null && !t.getEmail().isBlank()) {
                notificationService.dispatchEmail(t.getEmail(),
                        "Response to Support Enquiry #" + t.getId() + " – Witbank Elite",
                        "Hi " + t.getCustomerName() + ",\n\nRe: " + t.getSubject() + "\n\nResponse:\n" + response);
            }
        });
    }
}

package com.witbank.carwash.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.witbank.carwash.service.CustomerService;
import com.witbank.carwash.service.YocoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class PaymentController {

    @Autowired private YocoService     yocoService;
    @Autowired private CustomerService customerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/book/pay/yoco")
    public String startYoco(@RequestParam Long bookingId, @RequestParam double amount, Model model) {
        var booking = customerService.getBookingById(bookingId).orElse(null);
        if (booking == null) return "redirect:/";
        var result = yocoService.createCheckout(bookingId, amount,
                booking.getServiceType() + " — Witbank Elite Car Wash");
        if (result.success) return "redirect:" + result.redirectUrl;
        model.addAttribute("booking",        booking);
        model.addAttribute("yocoError",      result.error);
        model.addAttribute("yocoConfigured", yocoService.isConfigured());
        return "booking_slip";
    }

    @GetMapping("/book/pay/yoco/return")
    public String yocoReturn(@RequestParam Long bookingId, Model model) {
        return customerService.getBookingById(bookingId).map(b -> {
            model.addAttribute("booking",        b);
            model.addAttribute("yocoReturn",     true);
            model.addAttribute("yocoConfigured", yocoService.isConfigured());
            return "booking_slip";
        }).orElse("redirect:/");
    }

    @GetMapping("/book/pay/yoco/cancel")
    public String yocoCancel(@RequestParam Long bookingId, Model model) {
        return customerService.getBookingById(bookingId).map(b -> {
            model.addAttribute("booking",        b);
            model.addAttribute("yocoCancelled",  true);
            model.addAttribute("yocoConfigured", yocoService.isConfigured());
            return "booking_slip";
        }).orElse("redirect:/");
    }

    /**
     * Yoco webhook — signature verified via Svix HMAC before trusting the payload.
     * NOTE: field names (payload.id, payload.metadata.bookingId, payload.amount)
     * are based on Yoco docs and must be confirmed against a real webhook delivery.
     */
    @PostMapping("/webhooks/yoco")
    @ResponseBody
    public ResponseEntity<String> webhook(
            @RequestBody String body,
            @RequestHeader(value = "svix-id",        required = false) String svixId,
            @RequestHeader(value = "svix-timestamp",  required = false) String svixTs,
            @RequestHeader(value = "svix-signature",  required = false) String svixSig) {
        if (!yocoService.verifyWebhookSignature(svixId, svixTs, svixSig, body))
            return ResponseEntity.status(401).body("Invalid signature");
        try {
            JsonNode event = objectMapper.readTree(body);
            if ("payment.succeeded".equals(event.path("type").asText(""))) {
                JsonNode p  = event.path("payload");
                String bidStr = p.path("metadata").path("bookingId").asText(null);
                if (bidStr != null)
                    customerService.recordYocoPayment(Long.parseLong(bidStr),
                            p.path("amount").asLong(0) / 100.0,
                            p.path("id").asText("YOCO"));
            }
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Bad payload: " + e.getMessage());
        }
    }
}

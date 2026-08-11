package com.witbank.carwash.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Real card payments via Yoco's Online Checkout API.
 * Docs: https://developer.yoco.com/online/api-reference/checkouts-api
 *
 * Flow:
 *   1. createCheckout() — POSTs to Yoco, gets back a redirectUrl, customer is sent there.
 *   2. Customer pays on Yoco's hosted page (we never see/touch card details).
 *   3. Yoco calls our /webhooks/yoco endpoint with a "payment.succeeded" event.
 *   4. verifyWebhookSignature() confirms the call genuinely came from Yoco
 *      before we trust it and mark the booking as paid.
 *
 * IMPORTANT: this has not been tested against a real Yoco account (no network
 * access / API keys available while building this). Test thoroughly with
 * Yoco's TEST secret key before ever pointing this at a live key.
 */
@Service
public class YocoService {

    @Value("${yoco.secret-key:}")
    private String secretKey;

    @Value("${yoco.webhook-secret:}")
    private String webhookSecret;

    @Value("${carwash.base-url:http://localhost:8080}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    public static class CheckoutResult {
        public final boolean success;
        public final String redirectUrl;
        public final String checkoutId;
        public final String error;
        CheckoutResult(boolean success, String redirectUrl, String checkoutId, String error) {
            this.success = success; this.redirectUrl = redirectUrl; this.checkoutId = checkoutId; this.error = error;
        }
    }

    /**
     * Creates a Yoco hosted checkout for the given booking.
     * @param amountRand the amount in Rand (e.g. 450.00) — Yoco's API wants cents.
     */
    public CheckoutResult createCheckout(Long bookingId, double amountRand, String description) {
        if (!isConfigured()) {
            return new CheckoutResult(false, null, null, "Yoco is not configured (yoco.secret-key is blank).");
        }
        try {
            long amountCents = Math.round(amountRand * 100);

            Map<String, Object> body = Map.of(
                    "amount", amountCents,
                    "currency", "ZAR",
                    "successUrl", baseUrl + "/book/pay/yoco/return?bookingId=" + bookingId,
                    "cancelUrl",  baseUrl + "/book/pay/yoco/cancel?bookingId=" + bookingId,
                    "failureUrl", baseUrl + "/book/pay/yoco/cancel?bookingId=" + bookingId,
                    "metadata",   Map.of("bookingId", String.valueOf(bookingId)),
                    "lineItems", new Object[] {
                            Map.of("displayName", description, "quantity", 1, "pricingDetails", Map.of("price", amountCents))
                    }
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://payments.yoco.com/api/checkouts"))
                    .header("Authorization", "Bearer " + secretKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode json = objectMapper.readTree(response.body());
                String redirectUrl = json.path("redirectUrl").asText(null);
                String checkoutId = json.path("id").asText(null);
                if (redirectUrl == null) {
                    return new CheckoutResult(false, null, null, "Yoco response missing redirectUrl: " + response.body());
                }
                return new CheckoutResult(true, redirectUrl, checkoutId, null);
            } else {
                return new CheckoutResult(false, null, null, "Yoco API error (" + response.statusCode() + "): " + response.body());
            }
        } catch (Exception e) {
            return new CheckoutResult(false, null, null, "Yoco request failed: " + e.getMessage());
        }
    }

    /**
     * Verifies a Yoco webhook using their Svix-compatible signing scheme.
     * Header format: "svix-id", "svix-timestamp", "svix-signature: v1,<base64sig> [v1,<base64sig> ...]"
     * Signed payload = "{svix-id}.{svix-timestamp}.{rawBody}"
     * Secret is "whsec_<base64>" — the part after the prefix is base64-decoded into HMAC key bytes.
     */
    public boolean verifyWebhookSignature(String svixId, String svixTimestamp, String svixSignatureHeader, String rawBody) {
        if (webhookSecret == null || webhookSecret.isBlank()) return false;
        if (svixId == null || svixTimestamp == null || svixSignatureHeader == null) return false;

        try {
            String secretPart = webhookSecret.startsWith("whsec_") ? webhookSecret.substring(6) : webhookSecret;
            byte[] keyBytes = Base64.getDecoder().decode(secretPart);

            String signedContent = svixId + "." + svixTimestamp + "." + rawBody;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] computed = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
            String computedB64 = Base64.getEncoder().encodeToString(computed);

            // Header can contain multiple space-separated "v1,<sig>" entries — any match is valid.
            for (String part : svixSignatureHeader.split(" ")) {
                String[] kv = part.split(",", 2);
                if (kv.length == 2 && kv[1].equals(computedB64)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}

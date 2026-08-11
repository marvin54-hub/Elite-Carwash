package com.witbank.carwash.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Real-time weather via OpenWeatherMap's free Current Weather API
 * (https://openweathermap.org/current). Used for a small homepage widget
 * recommending whether today's a good day to book a wash.
 *
 * Requires openweather.api-key in application.properties — get a free key at
 * https://openweathermap.org/api. If blank/unreachable, the widget is hidden
 * rather than showing broken/fake data.
 */
@Service
public class WeatherService {

    @Value("${openweather.api-key:}")
    private String apiKey;

    @Value("${openweather.city:Witbank,ZA}")
    private String city;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class WeatherInfo {
        public final double tempC;
        public final String description;
        public final String icon; // OpenWeatherMap icon code, e.g. "01d"
        public final boolean goodForWash;
        public WeatherInfo(double tempC, String description, String icon, boolean goodForWash) {
            this.tempC = tempC; this.description = description; this.icon = icon; this.goodForWash = goodForWash;
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public WeatherInfo getCurrentWeather() {
        if (!isConfigured()) return null;
        try {
            String url = "https://api.openweathermap.org/data/2.5/weather?q="
                    + URLEncoder.encode(city, StandardCharsets.UTF_8)
                    + "&units=metric&appid=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return null;

            JsonNode json = objectMapper.readTree(response.body());
            double temp = json.path("main").path("temp").asDouble();
            JsonNode weatherArr = json.path("weather");
            String description = weatherArr.size() > 0 ? weatherArr.get(0).path("description").asText("") : "";
            String icon = weatherArr.size() > 0 ? weatherArr.get(0).path("icon").asText("01d") : "01d";

            // Simple heuristic: rain/snow/thunderstorm in the description = not a great wash day.
            String lower = description.toLowerCase();
            boolean badWeather = lower.contains("rain") || lower.contains("storm")
                    || lower.contains("snow") || lower.contains("drizzle");

            return new WeatherInfo(temp, description, icon, !badWeather);
        } catch (Exception e) {
            return null; // fail quietly — widget just won't show
        }
    }
}

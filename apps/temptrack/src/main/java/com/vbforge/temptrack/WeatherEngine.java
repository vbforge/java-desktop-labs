package com.vbforge.temptrack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeatherEngine {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public City searchCity(String name) throws Exception {
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded + "&count=1&language=en&format=json";

        JsonNode root = mapper.readTree(get(url));
        JsonNode results = root.get("results");

        if (results != null && results.isArray() && results.size() > 0) {
            JsonNode first = results.get(0);
            return new City(
                    0,
                    first.get("name").asText(),
                    first.has("country") ? first.get("country").asText() : "",
                    first.get("latitude").asDouble(),
                    first.get("longitude").asDouble()
            );
        }
        return null;
    }

    public WeatherData fetchWeather(double lat, double lon) throws Exception {
        // BUG FIX: String.format without a Locale uses the system locale. With a Ukrainian
        // (or German, French...) locale %f prints "50,450000" with a comma and the API call breaks.
        String url = String.format(Locale.ROOT,
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true" +
                "&hourly=temperature_2m,relative_humidity_2m,precipitation,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min,weather_code,uv_index_max&timezone=auto",
                lat, lon);

        return parseWeatherJson(get(url));
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Server returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    // Extracted for easy unit testing
    public WeatherData parseWeatherJson(String json) throws Exception {
        JsonNode root = mapper.readTree(json);

        JsonNode current = root.get("current_weather");
        double currentTemp = current.get("temperature").asDouble();
        double windSpeed = current.get("windspeed").asDouble();
        int currentCode = current.get("weathercode").asInt();

        // "current_weather" has no humidity/precipitation, so take them from the hourly
        // arrays at the current hour.
        // BUG FIX: the old code used index 0, which is midnight of day 1, not "now".
        JsonNode hourly = root.get("hourly");
        int nowIdx = currentHourIndex(hourly.get("time"), current.path("time").asText(null));
        int humidity = hourly.get("relative_humidity_2m").get(nowIdx).asInt();
        double precip = hourly.get("precipitation").get(nowIdx).asDouble();

        List<WeatherData.DailyForecast> dailyList = new ArrayList<>();
        JsonNode daily = root.get("daily");
        for (int i = 0; i < daily.get("time").size(); i++) {
            dailyList.add(new WeatherData.DailyForecast(
                    daily.get("time").get(i).asText(),
                    daily.get("temperature_2m_max").get(i).asDouble(),
                    daily.get("temperature_2m_min").get(i).asDouble(),
                    daily.get("weather_code").get(i).asInt()
            ));
        }

        List<WeatherData.HourlyForecast> hourlyList = new ArrayList<>();
        for (int i = 0; i < hourly.get("time").size(); i++) {
            hourlyList.add(new WeatherData.HourlyForecast(
                    hourly.get("time").get(i).asText(),
                    hourly.get("temperature_2m").get(i).asDouble(),
                    hourly.get("precipitation").get(i).asDouble(),
                    hourly.get("weather_code").get(i).asInt()
            ));
        }

        JsonNode uvNode = daily.path("uv_index_max").path(0);
        double uvMax = uvNode.isNumber() ? uvNode.asDouble() : -1;
        int utcOffset = root.path("utc_offset_seconds").asInt(0);

        return new WeatherData(currentTemp, currentCode, windSpeed, humidity, precip,
                dailyList, hourlyList, uvMax, utcOffset);
    }

    /** Finds the hourly slot matching "2026-10-20T14:15" (-> "2026-10-20T14"); falls back to 0. */
    private static int currentHourIndex(JsonNode hourlyTimes, String currentTime) {
        if (hourlyTimes == null || currentTime == null || currentTime.length() < 13) return 0;
        String hourPrefix = currentTime.substring(0, 13);
        for (int i = 0; i < hourlyTimes.size(); i++) {
            if (hourlyTimes.get(i).asText().startsWith(hourPrefix)) return i;
        }
        return 0;
    }
}

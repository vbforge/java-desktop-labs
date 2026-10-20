package com.vbforge.temptrack;

import java.util.List;

public class WeatherData {
    public record DailyForecast(String date, double maxTemp, double minTemp, int weatherCode) {}
    public record HourlyForecast(String time, double temp, double precipitation, int weatherCode) {}

    public final double currentTemp;
    public final int currentWeatherCode;
    public final double windSpeed;
    public final int humidity;
    public final double precipitation;

    public final List<DailyForecast> daily;
    public final List<HourlyForecast> hourly;

    /** Today's max UV index, or a negative value when the API did not provide it. */
    public final double uvIndexMax;
    /** Offset of the city's local time from UTC, in seconds (from Open-Meteo "timezone=auto"). */
    public final int utcOffsetSeconds;

    public WeatherData(double currentTemp, int currentWeatherCode, double windSpeed, int humidity,
                       double precipitation, List<DailyForecast> daily, List<HourlyForecast> hourly,
                       double uvIndexMax, int utcOffsetSeconds) {
        this.currentTemp = currentTemp;
        this.currentWeatherCode = currentWeatherCode;
        this.windSpeed = windSpeed;
        this.humidity = humidity;
        this.precipitation = precipitation;
        this.daily = daily;
        this.hourly = hourly;
        this.uvIndexMax = uvIndexMax;
        this.utcOffsetSeconds = utcOffsetSeconds;
    }

    /** Kept so existing code/tests that use the original constructor still compile. */
    public WeatherData(double currentTemp, int currentWeatherCode, double windSpeed, int humidity,
                       double precipitation, List<DailyForecast> daily, List<HourlyForecast> hourly) {
        this(currentTemp, currentWeatherCode, windSpeed, humidity, precipitation, daily, hourly, -1, 0);
    }
}

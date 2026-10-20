package com.vbforge.temptrack;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeatherEngineTest {

    @Test
    void testParseWeatherJson() throws Exception {
        // A minimal mock JSON matching Open-Meteo's structure
        String mockJson = """
        {
          "current_weather": {
            "temperature": 22.5,
            "windspeed": 14.0,
            "weathercode": 2
          },
          "hourly": {
            "time": ["2026-10-20T00:00", "2026-10-20T01:00"],
            "temperature_2m": [18.0, 17.5],
            "relative_humidity_2m": [58, 60],
            "precipitation": [0.0, 0.0],
            "weather_code": [1, 1]
          },
          "daily": {
            "time": ["2026-10-20", "2026-10-21"],
            "temperature_2m_max": [26.0, 24.0],
            "temperature_2m_min": [15.0, 14.0],
            "weather_code": [2, 1]
          }
        }
        """;

        WeatherEngine engine = new WeatherEngine();
        WeatherData data = engine.parseWeatherJson(mockJson);

        assertEquals(22.5, data.currentTemp);
        assertEquals(14.0, data.windSpeed);
        assertEquals(2, data.currentWeatherCode);
        assertEquals(58, data.humidity);
        
        assertEquals(2, data.daily.size());
        assertEquals(26.0, data.daily.get(0).maxTemp());
        assertEquals(15.0, data.daily.get(0).minTemp());
        
        assertEquals(2, data.hourly.size());
        assertEquals(18.0, data.hourly.get(0).temp());
    }
}
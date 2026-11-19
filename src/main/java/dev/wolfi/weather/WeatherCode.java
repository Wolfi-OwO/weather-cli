package dev.wolfi.weather;

import java.util.Map;

/** WMO weather interpretation codes as documented by Open-Meteo. */
public final class WeatherCode {
    private WeatherCode() {}

    private static final Map<Integer, String> TEXT = Map.ofEntries(
            Map.entry(0, "Clear sky"), Map.entry(1, "Mainly clear"), Map.entry(2, "Partly cloudy"), Map.entry(3, "Overcast"),
            Map.entry(45, "Fog"), Map.entry(48, "Rime fog"),
            Map.entry(51, "Light drizzle"), Map.entry(53, "Drizzle"), Map.entry(55, "Dense drizzle"),
            Map.entry(56, "Freezing drizzle"), Map.entry(57, "Dense freezing drizzle"),
            Map.entry(61, "Light rain"), Map.entry(63, "Rain"), Map.entry(65, "Heavy rain"),
            Map.entry(66, "Freezing rain"), Map.entry(67, "Heavy freezing rain"),
            Map.entry(71, "Light snow"), Map.entry(73, "Snow"), Map.entry(75, "Heavy snow"), Map.entry(77, "Snow grains"),
            Map.entry(80, "Light showers"), Map.entry(81, "Showers"), Map.entry(82, "Violent showers"),
            Map.entry(85, "Snow showers"), Map.entry(86, "Heavy snow showers"),
            Map.entry(95, "Thunderstorm"), Map.entry(96, "Thunderstorm with hail"), Map.entry(99, "Thunderstorm with heavy hail"));

    public static String describe(int code) {
        // An unknown code is shown as-is rather than hidden, so a new API value is visible instead of silently wrong.
        return TEXT.getOrDefault(code, "Unknown (code " + code + ")");
    }

    public static String compass(int degrees) {
        String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return names[(int) Math.round(((degrees % 360) + 360) % 360 / 45.0) % 8];
    }
}

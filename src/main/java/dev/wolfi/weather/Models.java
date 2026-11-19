package dev.wolfi.weather;

import java.util.List;

/** Plain data carriers; kept in one file because none of them has behaviour. */
public final class Models {
    private Models() {}

    public record Location(String name, String region, String country, double latitude, double longitude, String timezone) {
        public String label() {
            StringBuilder sb = new StringBuilder(name);
            if (region != null && !region.isBlank() && !region.equals(name)) sb.append(", ").append(region);
            if (country != null && !country.isBlank()) sb.append(", ").append(country);
            return sb.toString();
        }
    }

    public record Current(String time, double temperature, double feelsLike, int humidity,
                          double windSpeed, int windDirection, double precipitation, int weatherCode, boolean day) {}

    public record Day(String date, int weatherCode, double min, double max, double precipitation) {}

    public record Units(String temperature, String wind, String precipitation) {}

    public record Report(Location location, Units units, Current current, List<Day> forecast) {}
}

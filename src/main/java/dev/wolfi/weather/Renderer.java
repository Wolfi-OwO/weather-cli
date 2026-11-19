package dev.wolfi.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.wolfi.weather.Models.*;

import java.util.Locale;

public final class Renderer {
    private Renderer() {}

    public static String text(Report r, boolean forecast) {
        Current c = r.current();
        Units u = r.units();
        StringBuilder sb = new StringBuilder();
        sb.append(r.location().label()).append("  (").append(c.time().replace('T', ' ')).append(")\n");
        sb.append(String.format(Locale.ROOT, "%s, %.1f%s (feels like %.1f%s)%n", WeatherCode.describe(c.weatherCode()),
                c.temperature(), u.temperature(), c.feelsLike(), u.temperature()));
        sb.append(String.format(Locale.ROOT, "Humidity %d%%   Wind %.1f %s %s   Precipitation %.1f %s%n", c.humidity(),
                c.windSpeed(), u.wind(), WeatherCode.compass(c.windDirection()), c.precipitation(), u.precipitation()));
        if (forecast && !r.forecast().isEmpty()) {
            sb.append('\n');
            for (Day d : r.forecast()) {
                sb.append(String.format(Locale.ROOT, "%s  %5.1f / %5.1f%s  %4.1f %s  %s%n", d.date(), d.min(), d.max(), u.temperature(),
                        d.precipitation(), u.precipitation(), WeatherCode.describe(d.weatherCode())));
            }
        }
        return sb.toString();
    }

    public static String json(Report r, boolean forecast) throws com.fasterxml.jackson.core.JsonProcessingException {
        Report out = forecast ? r : new Report(r.location(), r.units(), r.current(), java.util.List.of());
        return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(out);
    }
}

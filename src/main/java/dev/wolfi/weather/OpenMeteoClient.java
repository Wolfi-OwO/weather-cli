package dev.wolfi.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.wolfi.weather.Models.*;

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

/**
 * Talks to Open-Meteo. It needs no API key, which is why this tool has no credentials to configure
 * (the first version used OpenWeatherMap and shipped a key in its source).
 */
public class OpenMeteoClient {
    public static final String GEOCODING = "https://geocoding-api.open-meteo.com";
    public static final String FORECAST = "https://api.open-meteo.com";

    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();
    private final String geocodingBase;
    private final String forecastBase;

    public OpenMeteoClient() {
        this(GEOCODING, FORECAST);
    }

    /** Base URLs are injectable so the tests can run against a local server instead of the internet. */
    public OpenMeteoClient(String geocodingBase, String forecastBase) {
        this.geocodingBase = geocodingBase;
        this.forecastBase = forecastBase;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    }

    public Location locate(String query) throws WeatherException {
        JsonNode root = get(geocodingBase + "/v1/search?count=1&format=json&name=" + enc(query));
        JsonNode hit = root.path("results").path(0);
        if (hit.isMissingNode()) {
            throw new WeatherException(WeatherException.NOT_FOUND, "No place found for \"" + query + "\". Try a bigger nearby city or add the country.");
        }
        return new Location(hit.path("name").asText(), hit.path("admin1").asText(null), hit.path("country").asText(null),
                hit.path("latitude").asDouble(), hit.path("longitude").asDouble(), hit.path("timezone").asText("auto"));
    }

    public Report report(Location place, boolean imperial, int days) throws WeatherException {
        String url = forecastBase + "/v1/forecast?latitude=" + place.latitude() + "&longitude=" + place.longitude()
                + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,wind_direction_10m,weather_code,is_day,precipitation"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum"
                + "&forecast_days=" + Math.max(1, days) + "&timezone=auto"
                + (imperial ? "&temperature_unit=fahrenheit&wind_speed_unit=mph&precipitation_unit=inch" : "");
        JsonNode root = get(url);
        JsonNode c = root.path("current");
        JsonNode cu = root.path("current_units");
        if (c.isMissingNode()) throw new WeatherException(WeatherException.API, "The weather service returned no current data.");

        Current current = new Current(c.path("time").asText(), c.path("temperature_2m").asDouble(), c.path("apparent_temperature").asDouble(),
                c.path("relative_humidity_2m").asInt(), c.path("wind_speed_10m").asDouble(), c.path("wind_direction_10m").asInt(),
                c.path("precipitation").asDouble(), c.path("weather_code").asInt(), c.path("is_day").asInt() == 1);
        // The API labels miles per hour "mp/h"; that reads like a typo, so it is normalised.
        Units units = new Units(cu.path("temperature_2m").asText(), cu.path("wind_speed_10m").asText().replace("mp/h", "mph"), cu.path("precipitation").asText());

        List<Day> forecast = new ArrayList<>();
        JsonNode d = root.path("daily");
        for (int i = 0; i < d.path("time").size(); i++) {
            forecast.add(new Day(d.path("time").get(i).asText(), d.path("weather_code").get(i).asInt(),
                    d.path("temperature_2m_min").get(i).asDouble(), d.path("temperature_2m_max").get(i).asDouble(),
                    d.path("precipitation_sum").get(i).asDouble()));
        }
        return new Report(place, units, current, forecast);
    }

    private JsonNode get(String url) throws WeatherException {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "weather-cli").GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new WeatherException(WeatherException.API, "The weather service answered HTTP " + res.statusCode() + ".");
            }
            return json.readTree(res.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherException(WeatherException.NETWORK, "Interrupted while contacting the weather service.", e);
        } catch (IOException e) {
            throw new WeatherException(WeatherException.NETWORK, "Could not reach the weather service (" + e.getClass().getSimpleName() + "). Check your connection.", e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

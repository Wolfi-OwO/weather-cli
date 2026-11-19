package dev.wolfi.weather;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the real command against a local server that mimics the two Open-Meteo endpoints. */
class MainTest {
    private static HttpServer server;
    private static String base;

    private static final String GEO_HIT = "{\"results\":[{\"name\":\"Villach\",\"latitude\":46.61,\"longitude\":13.85,\"country\":\"Austria\",\"admin1\":\"Carinthia\",\"timezone\":\"Europe/Vienna\"}]}";
    private static final String FORECAST = "{\"current_units\":{\"temperature_2m\":\"°C\",\"wind_speed_10m\":\"km/h\",\"precipitation\":\"mm\"},"
            + "\"current\":{\"time\":\"2026-11-19T18:15\",\"temperature_2m\":5.3,\"apparent_temperature\":3.7,\"relative_humidity_2m\":96,"
            + "\"wind_speed_10m\":2.3,\"wind_direction_10m\":309,\"weather_code\":45,\"is_day\":0,\"precipitation\":0.0},"
            + "\"daily\":{\"time\":[\"2026-11-19\",\"2026-11-20\"],\"weather_code\":[63,45],\"temperature_2m_max\":[7.2,8.3],\"temperature_2m_min\":[1.5,1.2],\"precipitation_sum\":[13.3,0.0]}}";

    @BeforeAll
    static void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/search", ex -> {
            String q = ex.getRequestURI().getQuery();
            reply(ex, 200, q.contains("name=Nowhere") ? "{}" : GEO_HIT);
        });
        server.createContext("/v1/forecast", ex -> reply(ex, ex.getRequestURI().getQuery().contains("fail=1") ? 500 : 200, FORECAST));
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stop() {
        server.stop(0);
    }

    private static void reply(com.sun.net.httpserver.HttpExchange ex, int code, String body) throws java.io.IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }

    private record Result(int code, String out, String err) {}

    private static Result run(String... args) {
        StringWriter out = new StringWriter(), err = new StringWriter();
        CommandLine cli = new CommandLine(new Main(new OpenMeteoClient(base, base)));
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(err));
        int code = cli.execute(args);
        return new Result(code, out.toString(), err.toString());
    }

    @Test
    void printsCurrentWeather() {
        Result r = run("Villach");
        assertEquals(0, r.code());
        assertTrue(r.out().contains("Villach, Carinthia, Austria"), r.out());
        assertTrue(r.out().contains("Fog, 5.3°C (feels like 3.7°C)"), r.out());
        assertTrue(r.out().contains("Wind 2.3 km/h NW"), r.out());
        assertFalse(r.out().contains("2026-11-20"), "forecast must be opt-in");
    }

    @Test
    void forecastFlagAddsDays() {
        Result r = run("Villach", "--forecast", "2");
        assertEquals(0, r.code());
        assertTrue(r.out().contains("2026-11-20"), r.out());
        assertTrue(r.out().contains("Rain"), r.out());
    }

    @Test
    void jsonOutputIsParseable() throws Exception {
        Result r = run("Villach", "--json");
        assertEquals(0, r.code());
        var tree = new com.fasterxml.jackson.databind.ObjectMapper().readTree(r.out());
        assertEquals(5.3, tree.path("current").path("temperature").asDouble());
        assertEquals("Villach", tree.path("location").path("name").asText());
    }

    @Test
    void unknownPlaceExitsWithTwo() {
        Result r = run("Nowhere");
        assertEquals(WeatherException.NOT_FOUND, r.code());
        assertTrue(r.err().contains("No place found"), r.err());
        assertEquals("", r.out());
    }

    @Test
    void multiWordCityIsJoined() {
        assertEquals(0, run("Villach", "am", "See").code());
    }

    @Test
    void forecastOutOfRangeIsRejected() {
        Result r = run("Villach", "--forecast", "99");
        assertEquals(CommandLine.ExitCode.USAGE, r.code());
        assertTrue(r.err().contains("between 1 and 16"), r.err());
    }

    @Test
    void networkFailureExitsWithThree() {
        StringWriter err = new StringWriter();
        CommandLine cli = new CommandLine(new Main(new OpenMeteoClient("http://127.0.0.1:1", "http://127.0.0.1:1")));
        cli.setErr(new PrintWriter(err));
        assertEquals(WeatherException.NETWORK, cli.execute("Villach"));
        assertTrue(err.toString().contains("Could not reach"));
    }

    @Test
    void compassAndCodes() {
        assertEquals("N", WeatherCode.compass(0));
        assertEquals("N", WeatherCode.compass(359));
        assertEquals("S", WeatherCode.compass(180));
        assertEquals("Unknown (code 123)", WeatherCode.describe(123));
    }
}

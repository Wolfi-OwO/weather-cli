package dev.wolfi.weather;

import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.concurrent.Callable;

@Command(name = "weather", mixinStandardHelpOptions = true, versionProvider = Main.Version.class,
        description = "Current weather and a short forecast for any city, powered by Open-Meteo (no API key needed).",
        exitCodeListHeading = "%nExit codes:%n",
        exitCodeList = {"0:Success", "2:Invalid usage", "3:Place not found", "4:Network error", "5:Weather service error"},
        footer = {"", "Examples:", "  weather Villach", "  weather \"New York\" --imperial --forecast 5", "  weather Tokyo --json | jq .current.temperature"})
public class Main implements Callable<Integer> {

    @Parameters(arity = "1..*", paramLabel = "CITY", description = "City name; several words are joined, so quotes are optional.")
    private java.util.List<String> city;

    @Option(names = {"-f", "--forecast"}, arity = "0..1", fallbackValue = "3", paramLabel = "DAYS",
            description = "Also show a daily forecast (1-16 days, default 3 when the flag is given).")
    private Integer forecastDays;

    @Option(names = {"-i", "--imperial"}, description = "Use °F, mph and inches instead of °C, km/h and mm.")
    private boolean imperial;

    @Option(names = "--json", description = "Print machine-readable JSON instead of text.")
    private boolean json;

    @Spec private Model.CommandSpec spec;

    private final OpenMeteoClient client;

    public Main() {
        this(new OpenMeteoClient());
    }

    Main(OpenMeteoClient client) {
        this.client = client;
    }

    @Override
    public Integer call() {
        if (forecastDays != null && (forecastDays < 1 || forecastDays > 16)) {
            throw new ParameterException(spec.commandLine(), "--forecast must be between 1 and 16 days.");
        }
        try {
            var place = client.locate(String.join(" ", city));
            var report = client.report(place, imperial, forecastDays == null ? 1 : forecastDays);
            boolean showForecast = forecastDays != null;
            spec.commandLine().getOut().print(json ? Renderer.json(report, showForecast) + System.lineSeparator() : Renderer.text(report, showForecast));
            spec.commandLine().getOut().flush();
            return 0;
        } catch (WeatherException e) {
            spec.commandLine().getErr().println("weather: " + e.getMessage());
            return e.exitCode();
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            spec.commandLine().getErr().println("weather: could not format the result: " + e.getOriginalMessage());
            return WeatherException.API;
        }
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    static class Version implements IVersionProvider {
        @Override
        public String[] getVersion() {
            String v = Main.class.getPackage().getImplementationVersion();
            return new String[] {"weather " + (v == null ? "dev" : v)};
        }
    }
}

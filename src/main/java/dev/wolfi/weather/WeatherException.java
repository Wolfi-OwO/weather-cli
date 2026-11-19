package dev.wolfi.weather;

/** Failure with an exit code, so scripts can tell "no such city" from "no network". */
public class WeatherException extends Exception {
    public static final int NOT_FOUND = 3;
    public static final int NETWORK = 4;
    public static final int API = 5;

    private final int exitCode;

    public WeatherException(int exitCode, String message, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public WeatherException(int exitCode, String message) {
        this(exitCode, message, null);
    }

    public int exitCode() {
        return exitCode;
    }
}

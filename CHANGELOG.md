# Changelog

## 1.0.1

- The release workflow now starts each bundle (the Windows `.exe`, the macOS app, the Linux launcher) on its own operating system before publishing and checks `--version` and the exit code for an unknown place.
- README notes the SmartScreen warning on Windows and that the macOS build is Apple Silicon only.

## 1.0.0

Rewrite of the original single-file `weather-app`.

- Open-Meteo instead of OpenWeatherMap, so no API key is needed.
- Command-line options: forecast, imperial units, JSON output, help and version.
- Distinct exit codes for not found, network and service errors.
- Real JSON parsing, unit tests against a local fake API.
- CI on Linux, Windows and macOS; tagged releases ship a jar and a bundle per system.

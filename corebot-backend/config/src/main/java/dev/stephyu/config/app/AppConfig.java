package dev.stephyu.config.app;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record AppConfig(int port) {
    private static final int DEFAULT_PORT = 3000;

    public static AppConfig fromEnvironment() {
        String rawPort = System.getenv("PORT");
        if (rawPort == null || rawPort.isBlank()) {
            return new AppConfig(DEFAULT_PORT);
        }

        try {
            return new AppConfig(Integer.parseInt(rawPort));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("PORT must be a valid integer", exception);
        }
    }
}

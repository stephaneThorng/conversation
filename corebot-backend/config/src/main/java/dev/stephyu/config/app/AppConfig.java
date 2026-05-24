package dev.stephyu.config.app;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record AppConfig(int port, DatabaseConfig databaseConfig) {
    private static final int DEFAULT_PORT = 3000;

    public static AppConfig fromEnvironment() {
        String rawPort = System.getenv("PORT");
        DatabaseConfig databaseConfig = DatabaseConfig.fromEnvironment();
        if (rawPort == null || rawPort.isBlank()) {
            return new AppConfig(DEFAULT_PORT, databaseConfig);
        }

        try {
            return new AppConfig(Integer.parseInt(rawPort), databaseConfig);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("PORT must be a valid integer", exception);
        }
    }
}

package dev.stephyu.config.app;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record  DatabaseConfig(
        String url,
        String user,
        String password
) {

    public DatabaseConfig {
        if (url.isBlank()) {
            throw new IllegalArgumentException("DB_URL must not be blank");
        }
        if (user.isBlank()) {
            throw new IllegalArgumentException("DB_USER must not be blank");
        }
        if (password.isBlank()) {
            throw new IllegalArgumentException("DB_PASSWORD must not be blank");
        }
    }

    public static DatabaseConfig fromEnvironment() {
        return new DatabaseConfig(
                required("DB_URL"),
                required("DB_USER"),
                required("DB_PASSWORD"));
    }

    private static String required(String envName) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(envName + " must be configured");
        }
        return value;
    }
}

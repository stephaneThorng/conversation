package dev.stephyu.config.app;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record LlmModelConfig(
        String baseUrl,
        String modelName,
        String apiKey
) {

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    private static final String DEFAULT_MODEL_NAME = "deepseek-v4-flash";
    private static final String DEFAULT_API_KEY = "xxx";

    public LlmModelConfig {
        baseUrl = requireText(baseUrl, "baseUrl");
        modelName = requireText(modelName, "modelName");
        Objects.requireNonNull(apiKey, "apiKey must not be null");
    }

    public static LlmModelConfig fromEnvironment() {
        return new LlmModelConfig(
                readOrDefault("DEEPSEEK_BASE_URL", DEFAULT_BASE_URL),
                readOrDefault("DEEPSEEK_MODEL", DEFAULT_MODEL_NAME),
                readOrDefault("DEEPSEEK_API_KEY", DEFAULT_API_KEY));
    }

    private static String readOrDefault(String envName, String defaultValue) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}

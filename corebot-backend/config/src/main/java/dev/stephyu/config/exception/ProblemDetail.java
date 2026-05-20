package dev.stephyu.config.exception;

import java.util.Map;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record ProblemDetail(
    String type,
    String title,
    int status,
    String detail,
    Map<String, Object> properties
) {
}

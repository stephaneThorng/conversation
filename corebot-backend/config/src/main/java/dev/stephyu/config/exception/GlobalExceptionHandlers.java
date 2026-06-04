package dev.stephyu.config.exception;

import io.javalin.config.RoutesConfig;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.validation.ValidationError;
import io.javalin.validation.ValidationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.stephyu.shared.exception.BusinessException;

@NullMarked
public final class GlobalExceptionHandlers {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandlers.class);

    public void register(RoutesConfig routes) {
        routes.exception(ValidationException.class, (exception, ctx) -> {
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("code", "VALIDATION_ERROR");
            properties.put("errors", extractValidationErrors(exception));

            writeProblem(
                ctx,
                new ProblemDetail(
                    "https://api.corebot.dev/problems/validation-error",
                    "Request validation failed",
                    HttpStatus.BAD_REQUEST.getCode(),
                    "One or more request fields are invalid.",
                    properties
                )
            );
        });

        routes.exception(BadRequestResponse.class, (exception, ctx) -> {
            Map<String, Object> properties = Map.of("code", "BAD_REQUEST");
            writeProblem(
                ctx,
                new ProblemDetail(
                    "https://api.corebot.dev/problems/bad-request",
                    "Bad request",
                    HttpStatus.BAD_REQUEST.getCode(),
                    exception.getMessage() != null ? exception.getMessage() : "Bad request",
                    properties
                )
            );
        });

        routes.exception(BusinessException.class, (exception, ctx) -> {
            Map<String, Object> properties = new LinkedHashMap<>(exception.properties());
            properties.putIfAbsent("code", exception.code());

            writeProblem(
                ctx,
                new ProblemDetail(
                    exception.type(),
                    exception.title(),
                    exception.status(),
                    exception.getMessage() != null ? exception.getMessage() : exception.title(),
                    properties
                )
            );
        });

        routes.exception(Exception.class, (exception, ctx) -> {
            LOGGER.error("Unexpected error while handling request", exception);
            Map<String, Object> properties = Map.of("code", "INTERNAL_ERROR");
            writeProblem(
                ctx,
                new ProblemDetail(
                    "https://api.corebot.dev/problems/internal-error",
                    "Internal server error",
                    HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "Something went wrong.",
                    properties
                )
            );
        });
    }

    private Map<String, List<String>> extractValidationErrors(ValidationException exception) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (Map.Entry<String, List<ValidationError<Object>>> entry : exception.getErrors().entrySet()) {
            List<String> messages = entry.getValue().stream()
                .map(ValidationError::getMessage)
                .toList();
            errors.put(entry.getKey(), messages);
        }
        return errors;
    }

    private void writeProblem(Context ctx, ProblemDetail problemDetail) {
        ctx.status(problemDetail.status());
        ctx.contentType("application/problem+json");
        ctx.json(problemDetail);
    }
}

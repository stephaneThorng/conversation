package dev.stephyu.config.web;

import dev.stephyu.config.exception.GlobalExceptionHandlers;
import io.javalin.Javalin;
import java.util.Collection;

import io.javalin.config.RoutesConfig;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class JavalinFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavalinFactory.class);
    private final GlobalExceptionHandlers globalExceptionHandlers;

    public JavalinFactory() {
        this(new GlobalExceptionHandlers());
    }

    public JavalinFactory(GlobalExceptionHandlers globalExceptionHandlers) {
        this.globalExceptionHandlers = globalExceptionHandlers;
    }

    public Javalin createApp(Collection<HttpEndpoint> controllers) {
        return Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
                rule.path = "/api/*";
                rule.anyHost();
            }));

            config.requestLogger.http((ctx, executionTimeMs) ->
                LOGGER.info(
                    "{} {} -> {} ({} ms)",
                    ctx.method(),
                    ctx.path(),
                    ctx.status(),
                    executionTimeMs
                )
            );

            globalExceptionHandlers.register(config.routes);
            registerRoutes(config.routes, controllers);
        });
    }

    public void registerRoutes(RoutesConfig routes, Collection<HttpEndpoint> controllers) {
        routes.get("/health", ctx -> ctx.result("OK"));
        controllers.forEach(endpoint -> endpoint.register(routes));
    }
}

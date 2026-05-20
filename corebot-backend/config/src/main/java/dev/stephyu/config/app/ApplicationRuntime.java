package dev.stephyu.config.app;

import io.javalin.Javalin;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ApplicationRuntime {
    private final AppConfig appConfig;
    private final Javalin app;

    public ApplicationRuntime(AppConfig appConfig, Javalin app) {
        this.appConfig = appConfig;
        this.app = app;
    }

    public void start() {
        app.start(appConfig.port());
    }

    public void stop() {
        app.stop();
    }
}

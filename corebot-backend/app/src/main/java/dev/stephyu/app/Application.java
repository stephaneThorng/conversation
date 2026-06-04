package dev.stephyu.app;

import dev.stephyu.config.app.AppBootstrap;
import dev.stephyu.config.app.AppConfig;
import dev.stephyu.config.app.ApplicationRuntime;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class Application {
    private Application() {
    }

    static void main(String[] args) {
        AppConfig appConfig = AppConfig.fromEnvironment();
        ApplicationRuntime runtime = new AppBootstrap().bootstrap(appConfig);
        Runtime.getRuntime().addShutdownHook(new Thread(runtime::stop));
        runtime.start();
    }
}

package dev.stephyu.config.app;

import dev.stephyu.config.dependency.ConversationModule;
import dev.stephyu.config.web.HttpEndpoint;
import dev.stephyu.config.web.JavalinFactory;
import io.javalin.Javalin;
import java.util.List;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class AppBootstrap {

    public ApplicationRuntime bootstrap(AppConfig appConfig) {
        List<HttpEndpoint> controllers = new ConversationModule().httpEndpoints();

        Javalin app = new JavalinFactory().createApp(controllers);
        return new ApplicationRuntime(appConfig, app);
    }
}

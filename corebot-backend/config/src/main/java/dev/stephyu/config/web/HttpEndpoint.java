package dev.stephyu.config.web;

import io.javalin.config.RoutesConfig;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface HttpEndpoint {
    void register(RoutesConfig routes);
}

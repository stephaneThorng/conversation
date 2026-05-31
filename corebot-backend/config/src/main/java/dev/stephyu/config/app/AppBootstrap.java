package dev.stephyu.config.app;

import dev.stephyu.config.db.DatabaseBootstrap;
import dev.stephyu.config.dependency.ConversationModule;
import dev.stephyu.config.web.HttpEndpoint;
import dev.stephyu.config.web.JavalinFactory;
import dev.stephyu.conversation.adapter.outbound.persistence.postgres.PostgresMenuSearchRepository;
import dev.stephyu.conversation.application.port.outbound.SearchMenuRepositoryPort;
import io.javalin.Javalin;
import java.util.List;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class AppBootstrap {

    public ApplicationRuntime bootstrap(AppConfig appConfig) {
        DatabaseBootstrap databaseBootstrap = new DatabaseBootstrap();
        DataSource dataSource = databaseBootstrap.createDataSource(appConfig.databaseConfig());
        databaseBootstrap.migrate(dataSource);
        DSLContext dslContext = databaseBootstrap.createDslContext(dataSource);
        SearchMenuRepositoryPort searchMenuRepositoryPort = new PostgresMenuSearchRepository(dslContext);

        List<HttpEndpoint> controllers = new ConversationModule().httpEndpoints(searchMenuRepositoryPort, dslContext);

        Javalin app = new JavalinFactory().createApp(controllers);
        return new ApplicationRuntime(appConfig, app);
    }
}

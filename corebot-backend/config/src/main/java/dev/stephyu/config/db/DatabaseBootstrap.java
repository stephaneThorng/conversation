package dev.stephyu.config.db;

import dev.stephyu.config.app.DatabaseConfig;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.postgresql.ds.PGSimpleDataSource;

@NullMarked
public final class DatabaseBootstrap {

    public DataSource createDataSource(DatabaseConfig databaseConfig) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(databaseConfig.url());
        dataSource.setUser(databaseConfig.user());
        dataSource.setPassword(databaseConfig.password());
        return dataSource;
    }

    public void migrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .sqlMigrationPrefix("")
                .sqlMigrationSeparator("_")
                .load()
                .migrate();
    }

    public DSLContext createDslContext(DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
}

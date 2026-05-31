import nu.studer.gradle.jooq.JooqEdition
import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.Property

plugins {
    id("java-library")
    id("nu.studer.jooq")
}

dependencies {
    implementation(project(":shared"))

    // Web
    implementation("io.javalin:javalin:7.2.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.2")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.21.2")

    // AI
    implementation("dev.langchain4j:langchain4j:1.15.0")
    implementation("dev.langchain4j:langchain4j-open-ai:1.15.0")
    implementation("org.jooq:jooq:3.19.18")
    implementation("org.flywaydb:flyway-core:11.8.2")
    implementation("org.flywaydb:flyway-database-postgresql:11.8.2")
    implementation("org.postgresql:postgresql:42.7.7")

    jooqGenerator("org.postgresql:postgresql:42.7.7")
    jooqGenerator("org.jooq:jooq-meta-extensions:3.19.18")

    testImplementation("org.testcontainers:junit-jupiter:1.21.0")
    testImplementation("org.testcontainers:postgresql:1.21.0")
    testImplementation("dev.langchain4j:langchain4j-ollama:1.15.0")
}

jooq {
    version.set("3.19.18")
    edition.set(JooqEdition.OSS)
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(true)
            jooqConfiguration.apply {
                logging = Logging.WARN
                generator.database.name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                generator.database.inputSchema = "PUBLIC"
                generator.database.properties = listOf(
                    Property().withKey("scripts").withValue("src/main/resources/db/migration/001_*.sql"),
                    Property().withKey("sort").withValue("flyway"),
                    Property().withKey("defaultNameCase").withValue("lower"),
                    Property().withKey("parseIgnoreComments").withValue("true")
                )
                generator.target.packageName = "dev.stephyu.conversation.jooq.generated"
                generator.target.directory = "build/generated-src/jooq/main"
            }
        }
    }
}

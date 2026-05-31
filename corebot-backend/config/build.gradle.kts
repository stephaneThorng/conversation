plugins {
    id("java-library")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":features:conversation"))
    implementation("io.javalin:javalin:7.2.2")
    implementation("org.jooq:jooq:3.19.18")
    implementation("org.flywaydb:flyway-core:11.8.2")
    implementation("org.flywaydb:flyway-database-postgresql:11.8.2")
    implementation("org.postgresql:postgresql:42.7.7")

    // AI
    implementation("dev.langchain4j:langchain4j:1.15.0")
    implementation("dev.langchain4j:langchain4j-open-ai:1.15.0")
}

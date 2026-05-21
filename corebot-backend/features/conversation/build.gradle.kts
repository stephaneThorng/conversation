plugins {
    id("java-library")
}

dependencies {
    implementation(project(":shared"))

    // Web
    implementation("io.javalin:javalin:7.2.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.2")

    // AI
    implementation("dev.langchain4j:langchain4j:1.15.0")
}

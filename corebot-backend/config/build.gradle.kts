plugins {
    id("java-library")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":features:conversation"))
    implementation("io.javalin:javalin:7.2.2")

    // AI
    implementation("dev.langchain4j:langchain4j-ollama:1.15.0")
    implementation("dev.langchain4j:langchain4j:1.15.0")
}

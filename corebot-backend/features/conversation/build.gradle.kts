plugins {
    id("java-library")
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
    implementation("com.microsoft.recognizers.text.datetime:recognizers-text-date-time:1.0-SNAPSHOT")
    implementation("com.microsoft.recognizers.text.number:recognizers-text-number:1.0-SNAPSHOT")
}

plugins {
    id("java-library")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":features:conversation"))
    implementation("io.javalin:javalin:7.2.2")
}

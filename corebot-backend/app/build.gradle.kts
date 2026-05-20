plugins {
    id("application")
}

dependencies {
    implementation(project(":config"))
    runtimeOnly("ch.qos.logback:logback-classic:1.5.18")
}

application {
    mainClass.set("dev.stephyu.app.Application")
}

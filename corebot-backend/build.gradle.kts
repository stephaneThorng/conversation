import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    base
    id("net.ltgt.errorprone") version "5.1.0" apply false
    id("net.ltgt.nullaway") version "2.2.0" apply false
}

group = "dev.stephyu"
version = "1.0-SNAPSHOT"

val javaVersion = 25
val junitBomVersion = "6.0.0"
val jspecifyVersion = "1.0.0"
val errorProneVersion = "2.49.0"
val nullAwayVersion = "0.13.3"

subprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenLocal()
        mavenCentral()
    }

    pluginManager.withPlugin("java") {
        pluginManager.apply("net.ltgt.errorprone")
        pluginManager.apply("net.ltgt.nullaway")

        extensions.getByType<JavaPluginExtension>().toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion))
        }

        dependencies {
            add("compileOnly", "org.jspecify:jspecify:$jspecifyVersion")
            add("errorprone", "com.google.errorprone:error_prone_core:$errorProneVersion")
            add("errorprone", "com.uber.nullaway:nullaway:$nullAwayVersion")
            add("testImplementation", platform("org.junit:junit-bom:$junitBomVersion"))
            add("testImplementation", "org.junit.jupiter:junit-jupiter")
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<JavaCompile>().configureEach {
            options.errorprone {
                disableWarningsInGeneratedCode.set(true)
                check("NullAway", CheckSeverity.ERROR)
                option("NullAway:JSpecifyMode", "true")
                option("NullAway:OnlyNullMarked", "true")
            }
        }

        tasks.named<JavaCompile>("compileTestJava") {
            options.errorprone.disable("NullAway")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}

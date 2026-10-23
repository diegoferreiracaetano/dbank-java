plugins {
    java
    checkstyle
    id("com.diffplug.spotless")
}

val catalog = the<VersionCatalogsExtension>().named("libs")

fun catalogVersion(name: String) = catalog.findVersion(name).get().requiredVersion

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

checkstyle {
    toolVersion = catalogVersion("checkstyle")
    configDirectory = rootProject.file("config/checkstyle")
    maxWarnings = 0
}

testing {
    suites {
        register<JvmTestSuite>("e2eTest") {
            useJUnitJupiter()
            dependencies {
                implementation(platform("org.springframework.boot:spring-boot-dependencies:${catalogVersion("spring-boot")}"))
                implementation(platform("org.testcontainers:testcontainers-bom:${catalogVersion("testcontainers")}"))
                implementation("org.testcontainers:postgresql")
                implementation("org.testcontainers:kafka")
                implementation("com.fasterxml.jackson.core:jackson-databind")
                implementation("org.assertj:assertj-core")
                implementation("org.awaitility:awaitility")
                runtimeOnly("org.junit.platform:junit-platform-launcher")
            }
        }
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

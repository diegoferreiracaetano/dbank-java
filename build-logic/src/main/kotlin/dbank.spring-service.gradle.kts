plugins {
    java
    checkstyle
    jacoco
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.diffplug.spotless")
    id("com.github.spotbugs")
}

val catalog = the<VersionCatalogsExtension>().named("libs")

fun catalogVersion(name: String) = catalog.findVersion(name).get().requiredVersion

fun catalogLibrary(name: String) = catalog.findLibrary(name).get()

extra["testcontainers.version"] = catalogVersion("testcontainers")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(catalogLibrary("archunit-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    spotbugs(catalogLibrary("spotbugs-tool"))
    spotbugs(catalogLibrary("commons-lang3"))
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

spotbugs {
    effort = com.github.spotbugs.snom.Effort.MAX
    reportLevel = com.github.spotbugs.snom.Confidence.MEDIUM
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports {
        create("html") { required = true }
        create("xml") { required = true }
    }
}

tasks.named("spotbugsTest") {
    enabled = false
}

tasks.matching { it.name == "spotbugsIntegrationTest" }.configureEach {
    enabled = false
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation("org.springframework.boot:spring-boot-testcontainers")
                implementation("org.testcontainers:postgresql")
                implementation("org.testcontainers:kafka")
            }
            targets {
                all {
                    testTask.configure { shouldRunAfter(tasks.test) }
                }
            }
        }
    }
}

configurations["integrationTestImplementation"].extendsFrom(
    configurations.implementation.get(),
    configurations.testImplementation.get()
)
configurations["integrationTestRuntimeOnly"].extendsFrom(
    configurations.runtimeOnly.get(),
    configurations.testRuntimeOnly.get()
)

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<Test>().matching { it.name == "integrationTest" }.configureEach {
    if (project.hasProperty("updateContract")) {
        systemProperty("updateContract", "true")
    }
}

jacoco {
    toolVersion = catalogVersion("jacoco")
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) { include("**/domain/**", "**/application/**", "**/service/**") }
            }
        )
    )
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification, testing.suites.named("integrationTest"))
}

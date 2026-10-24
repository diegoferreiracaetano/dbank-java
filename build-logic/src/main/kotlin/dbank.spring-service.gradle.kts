plugins {
    id("dbank.java-quality")
    jacoco
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.github.spotbugs")
}

extra["testcontainers.version"] = catalogVersion("testcontainers")

dependencies {
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(catalogLibrary("archunit-junit5"))
    testImplementation(project(":dbank-test-support"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    spotbugs(catalogLibrary("spotbugs-tool"))
    spotbugs(catalogLibrary("commons-lang3"))
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

val mainSources = the<SourceSetContainer>()["main"]

val jacocoFullReport by tasks.registering(JacocoReport::class) {
    group = "verification"
    description = "Coverage of the unit and the integration tests together, as a report with no minimum"
    val integrationTest = tasks.named<Test>("integrationTest")
    dependsOn(tasks.test, integrationTest)
    executionData(tasks.test.get(), integrationTest.get())
    sourceDirectories.from(mainSources.allSource.srcDirs)
    classDirectories.from(mainSources.output)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification, testing.suites.named("integrationTest"))
    finalizedBy(jacocoFullReport)
}

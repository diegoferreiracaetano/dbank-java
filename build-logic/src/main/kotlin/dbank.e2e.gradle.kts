plugins {
    id("dbank.java-quality")
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

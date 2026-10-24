plugins {
    id("dbank.java-quality")
    `java-library`
}

dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))
    api(platform("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}"))
    api("org.testcontainers:postgresql")
    api("org.testcontainers:kafka")
    api("org.springframework.boot:spring-boot-starter-test")
    api("org.springframework.kafka:spring-kafka")
    api(libs.archunit.junit5)
}

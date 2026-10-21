plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.spring.boot.plugin)
    implementation(libs.spring.dependency.management.plugin)
    implementation(libs.spotless.plugin)
    implementation(libs.spotbugs.plugin)
}

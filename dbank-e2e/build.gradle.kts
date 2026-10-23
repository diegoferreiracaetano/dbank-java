plugins {
    id("dbank.e2e")
}

tasks.named<Test>("e2eTest") {
    dependsOn(":dbank-ledger:bootJar", ":dbank-payments:bootJar")
    systemProperty("ledger.libs", project(":dbank-ledger").layout.buildDirectory.dir("libs").get().asFile.path)
    systemProperty("payments.libs", project(":dbank-payments").layout.buildDirectory.dir("libs").get().asFile.path)
    outputs.upToDateWhen { false }
}

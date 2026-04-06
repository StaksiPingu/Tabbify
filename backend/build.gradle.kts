plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "com.tabbify.server"
version = "1.0.0"

application {
    mainClass.set("com.tabbify.server.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.sessions)
    implementation(libs.ktor.serialization.json)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)

    implementation(libs.postgresql)
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation(libs.logback.classic)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.hocon)
    implementation(libs.bcrypt)

    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.datetime)
    implementation(libs.kotlin.uuid)

    testImplementation(libs.ktor.server.core)
    testImplementation(kotlin("test-junit"))
}

kotlin {
    jvmToolchain(21)
}

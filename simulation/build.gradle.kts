plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.java)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.websockets)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.core)
}

tasks.test {
    useJUnitPlatform {
        excludeTags("load")
    }
}

tasks.register<Test>("loadTest") {
    group = "verification"
    description = "Runs load-tagged simulation tests"
    useJUnitPlatform {
        includeTags("load")
    }
    classpath = tasks.test.get().classpath
    testClassesDirs = tasks.test.get().testClassesDirs
}

kotlin {
    jvmToolchain(11)
}

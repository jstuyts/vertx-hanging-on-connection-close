import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
}

group = "hangonclose"
version = "1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.vertx:vertx-core:4.3.4")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:4.3.4")
    implementation("io.vertx:vertx-pg-client:4.3.4")
    implementation("org.postgresql:postgresql:42.4.1")
    implementation("org.testcontainers:testcontainers:1.17.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

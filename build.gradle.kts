val assertjVersion = "3.14.0"
val kotlinLoggingVersion = "1.7.8"
val logbackVersion = "1.2.3"
val nimbusSdkVersion = "6.23"
val mockWebServerVersion = "4.3.1"
val jacksonVersion = "2.10.1"
val junitJupiterVersion = "5.5.2"
val kotlinVersion = "1.3.61"

group = "no.nav.security"
version = "0.1-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.3.61"
    id("org.jmailen.kotlinter") version "2.2.0"
}

apply(plugin = "org.jmailen.kotlinter")

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.squareup.okhttp3:mockwebserver:$mockWebServerVersion")
    implementation("com.nimbusds:oauth2-oidc-sdk:$nimbusSdkVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }
/*
    "build" {
        dependsOn("shadowJar")
    }

 */
}
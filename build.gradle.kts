import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

val assertjVersion = "3.27.6"
val kotlinLoggingVersion = "3.0.5"
val logbackVersion = "1.5.20"
val nimbusSdkVersion = "11.29.2"
val mockWebServerVersion = "5.2.1"
val jacksonVersion = "2.20.0"
val nettyVersion = "4.2.6.Final"
val junitJupiterVersion = "6.0.0"
val freemarkerVersion = "2.3.34"
val kotestVersion = "6.0.4"
val bouncyCastleVersion = "1.82"
val springBootVersion = "3.5.7"
val reactorTestVersion = "3.7.11"
val ktorVersion = "3.3.1"
val jsonPathVersion = "2.9.0"

val mainClassKt = "no.nav.security.mock.oauth2.StandaloneMockOAuth2ServerKt"

plugins {
    application
    alias(libs.plugins.kotlin.jvm) // refers to plugin declared in gradle/libs.versions.toml
    id("se.patrikerdes.use-latest-versions") version "0.2.19"
    id("com.github.ben-manes.versions") version "0.53.0"
    id("org.jmailen.kotlinter") version "5.2.0"
    id("com.google.cloud.tools.jib") version "3.4.5"
    id("com.vanniktech.maven.publish") version "0.34.0"
    id("org.jetbrains.dokka") version "2.0.0"
    kotlin("plugin.serialization") version "2.2.21"
    `java-library`
    signing
}

application {
    mainClass.set(mainClassKt)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    val kotlinTarget = libs.versions.kotlinTarget
    val kotlinTargetVersion = kotlinTarget.map {
        KotlinVersion.fromVersion(it.toKotlinMinor())
    }

    compilerOptions {
        languageVersion = kotlinTargetVersion
        apiVersion = kotlinTargetVersion
        // Syncing Kotlin JVM target with Java plugin JVM target
        jvmTarget = JvmTarget.JVM_17
    }

    // Setting core libraries version to manage compile and runtime dependencies exposed in the published artifact metadata
    // These will become transitive dependencies for our users.
    // Core libraries for JVM are kotlin-stdlib and kotlin-test.
    coreLibrariesVersion = kotlinTarget.get()
}

// 1.7.21 => 1.7, 1.9 => 1.9
fun String.toKotlinMinor() = split(".").take(2).joinToString(".")

apply(plugin = "org.jmailen.kotlinter")

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    api("com.squareup.okhttp3:mockwebserver:$mockWebServerVersion")
    api("com.nimbusds:oauth2-oidc-sdk:$nimbusSdkVersion")
    implementation("io.netty:netty-codec-http:$nettyVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("org.freemarker:freemarker:$freemarkerVersion")
    implementation("org.bouncycastle:bcpkix-jdk18on:$bouncyCastleVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion") // for kotest core jvm assertions
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5") // uses version matching kotlin-jvm plugin
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    // example use with different frameworks
    testImplementation("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-oauth2-client:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    constraints {
        testImplementation("com.jayway.jsonpath:json-path") {
            version {
                require(jsonPathVersion)
            }
        }
    }
    testImplementation("org.springframework.boot:spring-boot-test:$springBootVersion")
    constraints {
        testImplementation("org.xmlunit:xmlunit-core") {
            because("previous versions have security vulnerabilities")
            version {
                require("2.10.0")
            }
        }
        testImplementation("org.yaml:snakeyaml:2.5") {
            because("previous versions have security vulnerabilities")
        }
        add("api", "com.squareup.okio:okio") {
            version {
                require("3.4.0")
            }
        }
        add("testImplementation", "com.google.guava:guava") {
            version {
                require("32.1.2-jre")
            }
        }
    }
    testImplementation("io.projectreactor:reactor-test:$reactorTestVersion")
    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
    testImplementation("io.ktor:ktor-server-sessions:$ktorVersion")
    testImplementation("io.ktor:ktor-server-resources:$ktorVersion")
    testImplementation("io.ktor:ktor-server-auth:$ktorVersion")
    testImplementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    testImplementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-client-core:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion"){
        //Provides transitive vulnerable dependency maven:commons-codec:commons-codec:1.11 WS-2019-0379 6.5 Input Validation  Results powered by Mend.io
        exclude("commons-codec", "commons-codec")
    }
}

configurations {
    all {
        resolutionStrategy.force("com.fasterxml.woodstox:woodstox-core:7.1.1")
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), rootProject.name, version.toString())

    pom {
        name.set(rootProject.name)
        description.set("A simple mock oauth2 server based on OkHttp MockWebServer")
        url.set("https://github.com/navikt/${rootProject.name}")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                organization.set("Nav (Arbeids- og velferdsdirektoratet) - The Norwegian Labour and Welfare Administration")
                organizationUrl.set("https://www.nav.no")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/navikt/${rootProject.name}.git")
            developerConnection.set("scm:git:ssh://github.com/navikt/${rootProject.name}.git")
            url.set("https://github.com/navikt/${rootProject.name}")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/${rootProject.name}")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

jib {
    from {
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
        image = "cgr.dev/chainguard/jre:latest-dev"
    }

    to {
        // Image tags are set via the CLI (--image=...) in the workflow
        // But can set defaults here as well (optional)
        tags = setOf("latest")
    }

    container {
        ports = listOf("8080")
        mainClass = mainClassKt
        jvmFlags = listOf(
            "--sun-misc-unsafe-memory-access=allow", // see https://netty.io/wiki/java-24-and-sun.misc.unsafe.html
        )
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.named("dependencyUpdates", DependencyUpdatesTask::class.java).configure {
    this.resolutionStrategy {
        componentSelection {
            all {
                if (isNonStable(candidate.version)) {
                    reject("Release candidate")
                }
            }
        }
    }
}

buildscript {
    dependencies {
        configurations.classpath.get().exclude("xerces", "xercesImpl")
    }
}

tasks {
    withType<org.jmailen.gradle.kotlinter.tasks.LintTask> {
        dependsOn("formatKotlin")
    }

    withType<Test> {
        jvmArgs("--add-opens=java.base/java.util=ALL-UNNAMED")
        useJUnitPlatform()
    }

    withType<Wrapper> {
        gradleVersion = "8.14.1"
    }
}

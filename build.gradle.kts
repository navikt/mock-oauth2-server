import org.gradle.api.artifacts.ComponentSelection
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

val assertjVersion = "3.27.7"
val kotlinLoggingVersion = "3.0.5"
val logbackVersion = "1.6.0"
val nimbusSdkVersion = "11.38.2"
val mockWebServerVersion = "5.4.0"
val jacksonVersion = "3.2.1"
val jackson2TestVersion = "2.22.1"
val nettyVersion = "4.2.17.Final"
val junitJupiterVersion = "6.1.3"
val freemarkerVersion = "2.3.34"
val kotestVersion = "6.2.3"
val bouncyCastleVersion = "1.85"
val httpCore5Version = "5.4.3"
val springBootVersion = "4.1.1"
val reactorTestVersion = "3.8.6"
val ktorVersion = "3.5.2"

val mainClassKt = "no.nav.security.mock.oauth2.StandaloneMockOAuth2ServerKt"

plugins {
    application
    alias(libs.plugins.kotlin.jvm) // refers to plugin declared in gradle/libs.versions.toml
    id("se.patrikerdes.use-latest-versions") version "0.2.19"
    id("com.github.ben-manes.versions") version "0.54.0"
    id("org.jmailen.kotlinter") version "5.6.0"
    id("com.google.cloud.tools.jib") version "3.5.4"
    id("com.vanniktech.maven.publish") version "0.37.0"
    id("org.jetbrains.dokka") version "2.2.0"
    id("io.github.usefulness.maven-sympathy") version "0.3.0"
    alias(libs.plugins.kotlin.serialization)
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
    val kotlinLanguage = libs.versions.kotlinLanguage
    val kotlinToolchain = libs.versions.kotlinToolchain
    val kotlinLanguageVersion = kotlinLanguage.map {
        KotlinVersion.fromVersion(it.toKotlinMinor())
    }

    // Consumers must be able to compile against what we publish, and we must be able to build it.
    require(kotlinLanguage.get().toVersionRank() <= kotlinTarget.get().toVersionRank()) {
        "kotlinLanguage (${kotlinLanguage.get()}) must not exceed kotlinTarget (${kotlinTarget.get()})"
    }
    require(kotlinTarget.get().toVersionRank() <= kotlinToolchain.get().toVersionRank()) {
        "kotlinTarget (${kotlinTarget.get()}) must not exceed kotlinToolchain (${kotlinToolchain.get()})"
    }

    compilerOptions {
        languageVersion = kotlinLanguageVersion
        apiVersion = kotlinLanguageVersion
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

// 2.1.21 => 2001021, orders versions with unequal part counts
fun String.toVersionRank() =
    (split(".").map { it.toInt() } + listOf(0, 0)).take(3).fold(0) { rank, part -> rank * 1000 + part }

apply(plugin = "org.jmailen.kotlinter")

repositories {
    mavenCentral()
}

// Dependencies needed only when running the standalone server (Docker image via jib).
// runtimeClasspath extends this, runtimeElements does not, so these stay out of the
// published POM/module metadata and off library consumers' classpaths.
val standaloneRuntime = configurations.dependencyScope("standaloneRuntime")
configurations.runtimeClasspath {
    extendsFrom(standaloneRuntime.get())
}

// Smoke test on the kotlin-stdlib version we publish, which the normal test suite never uses.
// Keep it on plain JUnit: Kotest, Spring and Ktor have Kotlin floors of their own.
val minStdlibTest = testing.suites.register<JvmTestSuite>("minStdlibTest") {
    useJUnitJupiter(junitJupiterVersion)
    dependencies {
        // Depend on the project the way a consumer does, so we get the versions we publish
        implementation(project())
        runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
    }
}

configurations.named("minStdlibTestRuntimeClasspath") {
    val floor = libs.versions.kotlinTarget.get()
    resolutionStrategy.force(
        "org.jetbrains.kotlin:kotlin-stdlib:$floor",
        "org.jetbrains.kotlin:kotlin-reflect:$floor",
    )
}

tasks.check {
    dependsOn(minStdlibTest)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("tools.jackson.core:jackson-databind:$jacksonVersion")
    implementation("tools.jackson.module:jackson-module-kotlin:$jacksonVersion")
    add(standaloneRuntime.name, "ch.qos.logback:logback-classic:$logbackVersion")
    testRuntimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
    api("com.squareup.okhttp3:mockwebserver:$mockWebServerVersion")
    api("com.nimbusds:oauth2-oidc-sdk:$nimbusSdkVersion")
    implementation("io.netty:netty-codec-http:$nettyVersion")
    implementation("io.netty:netty-codec-dns:$nettyVersion")
    implementation("io.netty:netty-codec-http3:$nettyVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("org.freemarker:freemarker:$freemarkerVersion")
    implementation("org.bouncycastle:bcpkix-jdk18on:$bouncyCastleVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
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
    testImplementation("org.springframework.boot:spring-boot-test:$springBootVersion")
    constraints {
        testImplementation("org.yaml:snakeyaml:2.6") {
            because("previous versions have security vulnerabilities")
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
    testImplementation("io.ktor:ktor-serialization-jackson3:$ktorVersion")
    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion"){
        //Provides transitive vulnerable dependency maven:commons-codec:commons-codec:1.11 WS-2019-0379 6.5 Input Validation  Results powered by Mend.io
        exclude("commons-codec", "commons-codec")
    }
    testImplementation("org.apache.httpcomponents.core5:httpcore5-h2:$httpCore5Version")
}

configurations {
    all {
        resolutionStrategy {
            force(
                "com.fasterxml.woodstox:woodstox-core:7.2.1",
                // Security (test/build-scope transitive deps flagged by Dependabot).
                // These are not part of the published artifact (runtimeClasspath uses Jackson 3 only).
                // Jackson 2 (com.fasterxml.*): test scope is already pinned via the jackson-bom
                // platform, but Dokka's generator config isn't, so force it here too.
                "com.fasterxml.jackson.core:jackson-databind:$jackson2TestVersion", // GHSA-5jmj-h7xm-6q6v et al
                "com.fasterxml.jackson.core:jackson-core:$jackson2TestVersion", // GHSA-r7wm-3cxj-wff9
                "org.apache.httpcomponents.client5:httpclient5:5.6.3", // GHSA-hjcp-jmpx-g3qm, via ktor-client-apache5
                "org.apache.httpcomponents.core5:httpcore5:5.4.3", // GHSA-hf6x-8p5f-cgmf
                "org.apache.httpcomponents.core5:httpcore5-h2:5.4.3", // GHSA-v3jc-474w-2wm6
                "org.apache.logging.log4j:log4j-api:2.25.5", // GHSA-qv9r-c865-cp47, via spring-boot-starter-logging
                "org.jsoup:jsoup:1.23.1", // GHSA-pmhh-3w7g-xqp8, via spring-boot-starter-test
            )
            // Netty ships all modules in lockstep; transitive deps drag in older/mixed
            // versions (4.1.x/4.2.15) with CVEs in codec-http2/http3/dns. Align the whole
            // group on the version of our direct netty-codec-http dependency.
            eachDependency {
                if (requested.group == "io.netty") {
                    useVersion(nettyVersion)
                    because("align all netty modules; fixes GHSA-93wv-jw9v-4972, GHSA-hpcc-26xq-25fv, GHSA-mfg7-5gfp-c4w3")
                }
            }
        }
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
            all { selection: ComponentSelection ->
                if (isNonStable(selection.candidate.version)) {
                    selection.reject("Release candidate")
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
}

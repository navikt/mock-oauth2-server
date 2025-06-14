import java.time.Duration
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

val assertjVersion = "3.27.3"
val kotlinLoggingVersion = "3.0.5"
val logbackVersion = "1.5.18"
val nimbusSdkVersion = "11.25"
val mockWebServerVersion = "4.12.0"
val jacksonVersion = "2.19.0"
val nettyVersion = "4.2.2.Final"
val junitJupiterVersion = "5.13.0"
val freemarkerVersion = "2.3.34"
val kotestVersion = "5.9.1"
val bouncyCastleVersion = "1.81"
val springBootVersion = "3.5.0"
val reactorTestVersion = "3.7.6"
val ktorVersion = "2.3.13"
val jsonPathVersion = "2.9.0"

val mavenRepoBaseUrl = "https://oss.sonatype.org"
val mainClassKt = "no.nav.security.mock.oauth2.StandaloneMockOAuth2ServerKt"

plugins {
    application
    alias(libs.plugins.kotlin.jvm) // refers to plugin declared in gradle/libs.versions.toml
    id("se.patrikerdes.use-latest-versions") version "0.2.18"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("org.jmailen.kotlinter") version "5.1.0"
    id("com.google.cloud.tools.jib") version "3.4.5"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("net.researchgate.release") version "3.1.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("org.jetbrains.dokka") version "2.0.0"
    `java-library`
    `maven-publish`
    signing
}

application {
    mainClass.set(mainClassKt)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
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
        testImplementation("org.yaml:snakeyaml:2.4") {
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
    testImplementation("io.ktor:ktor-server-locations:$ktorVersion")
    testImplementation("io.ktor:ktor-server-auth:$ktorVersion")
    testImplementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    testImplementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-client-core:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

configurations {
    all {
        resolutionStrategy.force("com.fasterxml.woodstox:woodstox-core:7.1.1")
    }
}

nexusPublishing {
    packageGroup.set("no.nav")
    clientTimeout.set(Duration.ofMinutes(2))
    repositories {
        sonatype {
            username.set(System.getenv("SONATYPE_USERNAME"))
            password.set(System.getenv("SONATYPE_PASSWORD"))
        }
    }

    transitionCheckOptions {
        maxRetries.set(60)
        delayBetween.set(Duration.ofMillis(10000))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = rootProject.name
            from(components["java"])

            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }

                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set(rootProject.name)
                description.set("A simple mock oauth2 server based on OkHttp MockWebServer")
                url.set("https://github.com/navikt/${rootProject.name}")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        organization.set("NAV (Arbeids- og velferdsdirektoratet) - The Norwegian Labour and Welfare Administration")
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
    }
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

ext["signing.gnupg.keyName"] = System.getenv("GPG_KEY_NAME")
ext["signing.gnupg.passphrase"] = System.getenv("GPG_PASSPHRASE")
ext["signing.gnupg.executable"] = "gpg"

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
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
        image = "gcr.io/distroless/java17-debian11"
    }
    container {
        ports = listOf("8080")
        mainClass = mainClassKt
    }
}

(components["java"] as AdhocComponentWithVariants).withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
    skip()
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

tasks.named("useLatestVersions", se.patrikerdes.UseLatestVersionsTask::class.java).configure {
    updateBlacklist = listOf(
        "io.codearte:nexus-staging"
    )
}

// This task is added by Gradle when we use java.withJavadocJar()
tasks.named<Jar>("javadocJar") {
    from(tasks.named("dokkaJavadoc"))
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

    withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
        archiveBaseName.set("app")
        archiveClassifier.set("")
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to mainClassKt
                )
            )
        }
    }

    withType<Test> {
        jvmArgs("--add-opens=java.base/java.util=ALL-UNNAMED")
        useJUnitPlatform()
    }

    "jibDockerBuild" {
        dependsOn("shadowJar")
    }

    withType<Sign>().configureEach {
        onlyIf {
            System.getenv("GPG_KEYS") != null
        }
    }

    withType<Wrapper> {
        gradleVersion = "8.14.1"
    }
}

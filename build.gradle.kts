import java.time.Duration
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

val assertjVersion = "3.23.1"
val kotlinLoggingVersion = "2.1.23"
val logbackVersion = "1.2.11"
val nimbusSdkVersion = "9.38"
val mockWebServerVersion = "4.10.0"
val jacksonVersion = "2.13.3"
val nettyVersion = "4.1.79.Final"
val junitJupiterVersion = "5.8.2"
val kotlinVersion = "1.7.10"
val freemarkerVersion = "2.3.31"
val kotestVersion = "5.3.2"
val bouncyCastleVersion = "1.70"
val springBootVersion = "2.7.1"
val reactorTestVersion = "3.4.21"
val ktorVersion = "2.0.3"

val mavenRepoBaseUrl = "https://oss.sonatype.org"
val mainClassKt = "no.nav.security.mock.oauth2.StandaloneMockOAuth2ServerKt"

plugins {
    application
    kotlin("jvm") version "1.7.10"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"
    id("com.github.ben-manes.versions") version "0.42.0"
    id("org.jmailen.kotlinter") version "3.11.1"
    id("com.google.cloud.tools.jib") version "3.2.1"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.researchgate.release") version "3.0.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("org.jetbrains.dokka") version "1.7.10"
    `java-library`
    `maven-publish`
    signing
}

application {
    mainClassName = mainClassKt
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}

apply(plugin = "org.jmailen.kotlinter")

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    api("com.squareup.okhttp3:mockwebserver:$mockWebServerVersion")
    api("com.nimbusds:oauth2-oidc-sdk:$nimbusSdkVersion")
    implementation("io.netty:netty-all:$nettyVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("org.freemarker:freemarker:$freemarkerVersion")
    implementation("org.bouncycastle:bcpkix-jdk15on:$bouncyCastleVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion") // for kotest core jvm assertions
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    //example use with different frameworks
    testImplementation("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-oauth2-client:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
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
        maxRetries.set(40)
        delayBetween.set(Duration.ofMillis(5000))
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
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
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

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    withType<Test> {
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
        gradleVersion = "7.4.2"
    }
}

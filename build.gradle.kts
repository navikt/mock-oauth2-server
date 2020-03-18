import java.time.Duration

val assertjVersion = "3.14.0"
val kotlinLoggingVersion = "1.7.8"
val logbackVersion = "1.2.3"
val nimbusSdkVersion = "6.23"
val mockWebServerVersion = "4.3.1"
val jacksonVersion = "2.10.1"
val junitJupiterVersion = "5.5.2"
val konfigVersion = "1.6.10.0"
val kotlinVersion = "1.3.61"
val freemarkerVersion = "2.3.29"
val mavenRepoBaseUrl = "https://oss.sonatype.org"
val mainClassKt = "no.nav.security.mock.StandaloneMockOAuth2ServerKt"

plugins {
    application
    kotlin("jvm") version "1.3.61"
    id("org.jmailen.kotlinter") version "2.2.0"
    id("com.google.cloud.tools.jib") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("net.researchgate.release") version "2.8.1"
    id("io.codearte.nexus-staging") version "0.21.2"
    id("de.marcphilipp.nexus-publish") version "0.4.0"
    `java-library`
    `maven-publish`
    signing
}

application {
    mainClassName = mainClassKt
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
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
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.natpryce:konfig:$konfigVersion")
    api("com.squareup.okhttp3:mockwebserver:$mockWebServerVersion")
    api("com.nimbusds:oauth2-oidc-sdk:$nimbusSdkVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("org.freemarker:freemarker:$freemarkerVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

nexusStaging {
    username = System.getenv("SONATYPE_USERNAME")
    password = System.getenv("SONATYPE_PASSWORD")
    packageGroup = "no.nav"
    delayBetweenRetriesInMillis = 5000
}

nexusPublishing {
    clientTimeout.set(Duration.ofMinutes(2))
    repositories {
        sonatype()
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
        /*
        maven {
            name = "Sonatype"
            val releasesRepoUrl = uri("$mavenRepoBaseUrl/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("$mavenRepoBaseUrl/content/repositories/snapshots")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = System.getenv("SONATYPE_USERNAME")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }*/
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
        image = "gcr.io/distroless/java:11"
    }
    container {
        ports = listOf("1111")
        mainClass = mainClassKt
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
            jvmTarget = "11"
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    "jibDockerBuild" {
        dependsOn("shadowJar")
    }

    "publish" {
       dependsOn("initializeSonatypeStagingRepository")
    }

    "publishToSonatype" {
        dependsOn("publish")
    }

    withType<Sign>().configureEach {
        onlyIf {
            project.hasProperty("signing.gnupg.keyName")
        }
    }

    /*withType<PublishToMavenRepository> {
        finalizedBy("closeAndReleaseRepository")
    }*/
}

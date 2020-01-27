val assertjVersion = "3.14.0"
val kotlinLoggingVersion = "1.7.8"
val logbackVersion = "1.2.3"
val nimbusSdkVersion = "6.23"
val mockWebServerVersion = "4.3.1"
val jacksonVersion = "2.10.1"
val junitJupiterVersion = "5.5.2"
val kotlinVersion = "1.3.61"

val mavenRepoBaseUrl = "https://oss.sonatype.org"

group = "no.nav.security"
version = "0.1-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.3.61"
    id("org.jmailen.kotlinter") version "2.2.0"
    `java-library`
    `maven-publish`
    signing
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
    api("com.squareup.okhttp3:mockwebserver:$mockWebServerVersion")
    api("com.nimbusds:oauth2-oidc-sdk:$nimbusSdkVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "mock-oauth2-server"
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
                name.set("mock-oauth2-server")
                description.set("A simple mock oauth2 server based on OkHttp MockWebServer")
                url.set("https://github.com/navikt/mock-oauth2-server")

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
                    developer {
                        id.set("tommytroen")
                        name.set("Tommy Trøen")
                        email.set("tommy.troen@nav.no")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/navikt/mock-oauth2-server.git")
                    developerConnection.set("scm:git:ssh://github.com/navikt/mock-oauth2-server.git")
                    url.set("https://github.com/navikt/mock-oauth2-server")
                }
                withXml {
                    //dependencies.
                    /*asNode().dependencies.'*'.findAll() {
                        it.scope.text() == 'runtime' && project.configurations.compile.allDependencies.find { dep ->
                            dep.name == it.artifactId.text()
                        }
                    }.each() {
                        it.scope*.value = 'compile'
                    }*/
                }
            }
        }
    }
    repositories {
        maven {
            // change URLs to point to your repos, e.g. http://my.org/repo
            val releasesRepoUrl = uri("$mavenRepoBaseUrl/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("$mavenRepoBaseUrl/content/repositories/snapshots")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
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
    withType<Sign>().configureEach {
        onlyIf { !version.toString().endsWith("SNAPSHOT") }
    }
}
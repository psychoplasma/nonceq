plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)

    // Apply the java-library plugin for API and implementation separation.
    `java-library`

    // Apply the maven-publish plugin for publishing to Maven repositories.
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers.redis)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers)

    implementation(libs.web3j.core)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.redis.clients)
}

kotlin {
    jvmToolchain(21)
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("stress")
    }

    testLogging {
        events("passed", "failed")
        showStandardStreams = true
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "com.psychoplasma.nonceq"
            artifactId = "nonceq"
            version = project.version.toString()

            pom {
                name = "nonceq"
                description = "Nonce queuing manager for nonce-based transaction. Simple LRU queue with reuse and discarding features."
                url = "https://github.com/psychoplasma/nonceq"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }

                developers {
                    developer {
                        name = "Mustafa Morca"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/psychoplasma/nonceq.git"
                    developerConnection = "scm:git:ssh://git@github.com:psychoplasma/nonceq.git"
                    url = "https://github.com/psychoplasma/nonceq"
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/psychoplasma/nonceq")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as? String
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as? String
            }
        }
    }
}

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)

    // Apply the JReleaser plugin for release management.
    alias(libs.plugins.jreleaser)

    // Apply the java-library plugin for API and implementation separation.
    `java-library`

    // Apply the maven-publish plugin for publishing.
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
    explicitApi()
}

java {
    withJavadocJar()
    withSourcesJar()
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
            groupId = "io.github.psychoplasma"
            artifactId = "nonceq"
            version = "0.1.0"

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
                        id = "psychoplasma"
                        name = "Mustafa Morca"
                        email = "mmorca@gmail.com"
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
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    project {
        description = "Nonce queuing manager for nonce-based transaction. Simple LRU queue with reuse and discarding features."
    }

    signing {
        setActive("ALWAYS")
        armored = true
    }

    deploy {
        maven {
            github {
                create("app") {
                    setActive("ALWAYS")
                    url = "https://maven.pkg.github.com/psychoplasma/nonceq"
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                    stagingRepository("build/staging-deploy")
                }
            }
        }

        maven {
            mavenCentral {
                create("sonatype") {
                    setActive("ALWAYS")
                    url = "https://central.sonatype.com/api/v1/publisher"
                    username = System.getenv("JRELEASER_MAVENCENTRAL_USERNAME")
                    password = System.getenv("JRELEASER_MAVENCENTRAL_PASSWORD")
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }

    release {
        github {
            enabled = false
        }
    }
}

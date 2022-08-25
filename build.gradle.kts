group = "com.github.thake.avro4k"
val javaCompatibility = "1.8"

plugins {
    `java-library`
    idea
    `maven-publish`
    signing
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.versions)
    alias(libs.plugins.release)
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlibJdk8)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.avro)
    implementation(libs.kafka.avro.serializer)
    implementation(libs.kafka.avro.serde)
    implementation(libs.avro4k)
    implementation(libs.classgraph)
    implementation(libs.retry)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.kafka.streams)
    testImplementation(libs.bundles.logging)
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.runtime)

}
// Configure existing Dokka task to output HTML to typical Javadoc directory
tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("javadoc"))
}

// Create dokka Jar task from dokka task output
val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    // dependsOn(tasks.dokka) not needed; dependency automatically inferred by from(tasks.dokka)
    from(tasks.dokkaHtml)
}

tasks {
    compileJava {
        targetCompatibility = javaCompatibility
    }
    compileKotlin {
        kotlinOptions.jvmTarget = javaCompatibility
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
    compileTestJava {
        targetCompatibility = javaCompatibility
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = javaCompatibility
    }
    test {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
            jvmArgs("-DconfluentVersion=${libs.versions.confluent}")
        }

    }

    idea {
        module {
            isDownloadSources = true
            isDownloadJavadoc = false
        }
    }
}

// Create sources Jar from main kotlin sources
val sourcesJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}
publishing{
    repositories{
        maven{
            name = "mavenCentral"
            url = if (project.isSnapshot) {
                uri("https://oss.sonatype.org/content/repositories/snapshots/")
            } else {
                uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            }
            credentials {
                username = project.findProperty("ossrhUsername") as? String
                password = project.findProperty("ossrhPassword") as? String
            }
        }
    }
    publications{
        create<MavenPublication>("mavenJava"){
            from(components["java"])
            artifact(sourcesJar)
            artifact(dokkaJar)
            //artifact(javadocJar.get())
            pom{
                name.set("Kafka serializer using avro4k")
                description.set("Provides Kafka SerDes and Serializer / Deserializer implementations for avro4k")
                url.set("https://github.com/thake/kafka-avro4k-serializer")
                developers {
                    developer {
                        name.set("Thorsten Hake")
                        email.set("mail@thorsten-hake.com")
                    }
                }
                scm {
                    connection.set("https://github.com/thake/kafka-avro4k-serializer.git")
                    developerConnection.set("scm:git:ssh://github.com:thake/kafka-avro4k-serializer.git")
                    url.set("https://github.com/tbroyer/gradle-incap-helper")
                }
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }
}
signing {
    useGpgCmd()
    isRequired = !isSnapshot
    sign(publishing.publications["mavenJava"])
}
tasks.named("afterReleaseBuild") {
    dependsOn("publish")
}

inline val Project.isSnapshot
    get() = version.toString().endsWith("-SNAPSHOT")

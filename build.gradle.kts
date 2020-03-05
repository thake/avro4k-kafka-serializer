group = "com.github.thake.avro4k"
version = "0.1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.3.61"
    kotlin("plugin.serialization") version "1.3.61"
    `java-library`
    idea
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "0.10.1"
    id("net.researchgate.release") version "2.6.0"
}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
}

dependencies{
    val confluentVersion by extra("5.3.2")
    val avroVersion by extra("1.9.1")
    val junitVersion by extra("5.1.0")
    implementation("org.apache.avro:avro:${avroVersion}")
    implementation("io.confluent:kafka-avro-serializer:${confluentVersion}")
    implementation("io.confluent:kafka-streams-avro-serde:5.3.0")
    implementation("com.sksamuel.avro4k:avro4k-core:0.22.0")
    implementation("org.reflections:reflections:0.9.12")
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")

}
// Configure existing Dokka task to output HTML to typical Javadoc directory
tasks.dokka {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
}

// Create dokka Jar task from dokka task output
val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    classifier = "javadoc"
    // dependsOn(tasks.dokka) not needed; dependency automatically inferred by from(tasks.dokka)
    from(tasks.dokka)
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    test {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
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
    dependsOn("publishMavenJavaPublicationToMavenCentralRepository")
}

inline val Project.isSnapshot
    get() = version.toString().endsWith("-SNAPSHOT")

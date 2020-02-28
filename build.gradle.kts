group = "com.github.thake.avro4k"
version = "0.1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.3.61"
    kotlin("plugin.serialization") version "1.3.61"
    `java-library`
    idea
    `maven-publish`
}

repositories {
    mavenCentral()
    mavenLocal()
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
publishing{
    publications{
        create<MavenPublication>("mavenJava"){
            from(components["java"])
            artifact(tasks["kotlinSourcesJar"])
        }
    }
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
    kotlinSourcesJar{
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }
}

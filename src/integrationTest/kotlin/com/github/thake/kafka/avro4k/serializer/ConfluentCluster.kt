package com.github.thake.kafka.avro4k.serializer

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.utility.DockerImageName

class ConfluentCluster(confluentVersion: String) {

    private val kafkaContainer =
        KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:$confluentVersion"))
            .withNetwork(Network.newNetwork())
            .withNetworkAliases("kafka")
            .withEnv("KAFKA_HOST_NAME", "kafka")
            .apply { this.start() }
    private val schemaRegistry =
        GenericContainer("confluentinc/cp-schema-registry:$confluentVersion")
            .withNetwork(kafkaContainer.network)
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:9092")
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "localhost")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
            .withExposedPorts(8081)
            .apply {
                this.start()
            }
    val schemaRegistryUrl = "http://${schemaRegistry.host}:${schemaRegistry.getMappedPort(8081)}"
    val bootstrapServers = kafkaContainer.bootstrapServers

    fun stop() {
        schemaRegistry.stop()
        kafkaContainer.stop()
    }
}
package com.github.thake.kafka.avro4k.serializer

import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.Serializable
import org.apache.kafka.common.errors.SerializationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TestNetworkOutageRecovery {
    private val mockedRegistry = mockk<SchemaRegistryClient>()

    @Serializable
    private data class TestRecord(
        val str: String
    )

    @BeforeEach
    fun resetMocks() {
        clearAllMocks()
    }

    @Test
    fun testSuccessAfterRetry() {
        val serializer = KafkaAvro4kSerializer(mockedRegistry)
        serializer.configure(
            mapOf(
                AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS to "true",
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://registry",
                AbstractKafkaAvro4kSerDeConfig.SCHEMA_REGISTRY_RETRY_ATTEMPTS_CONFIG to "2"
            ),
            false
        )
        val topic = "My-Topic"
        //on first call throw an exception
        every {
            mockedRegistry.register(any(), any<ParsedSchema>())
        }.throws(RestClientException("Could not contact registry", 404, 4))
            .andThen(1)
        serializer.serialize(topic, TestRecord("BLA"))
        verify(exactly = 2) {
            mockedRegistry.register(any(), any<ParsedSchema>())
        }
    }

    @Test
    fun testNoInfiniteRetry() {
        val serializer = KafkaAvro4kSerializer(mockedRegistry)
        serializer.configure(
            mapOf(
                AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS to "true",
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://registry",
                AbstractKafkaAvro4kSerDeConfig.SCHEMA_REGISTRY_RETRY_ATTEMPTS_CONFIG to "2"
            ),
            false
        )
        val topic = "My-Topic"
        //on first call throw an exception
        every {
            mockedRegistry.register(any(), any<ParsedSchema>())
        }.throws(RestClientException("Could not contact registry", 404, 4))
            .andThenThrows(RestClientException("Still no contact", 404, 4))
            .andThen(2)
        assertThrows<SerializationException> {
            serializer.serialize(topic, TestRecord("BLA"))
        }
        verify(exactly = 2) {
            mockedRegistry.register(any(), any<ParsedSchema>())
        }
    }
}
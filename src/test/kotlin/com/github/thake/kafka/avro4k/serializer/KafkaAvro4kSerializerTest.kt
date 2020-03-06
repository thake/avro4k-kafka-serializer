package com.github.thake.kafka.avro4k.serializer

import com.nhaarman.mockitokotlin2.*
import com.sksamuel.avro4k.AvroName
import com.sksamuel.avro4k.AvroNamespace
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import org.apache.avro.Schema
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class KafkaAvro4kSerializerTest {
    val registryMock = mock<SchemaRegistryClient>{
        var storedSchema : Schema? = null
        on{ getId(any(), any())} doAnswer {
            storedSchema = it.getArgument(1)
            1
        } doReturn 1
        on{ register(any(),any())} doAnswer {
            storedSchema = it.getArgument(1)
            1
        }
        on{getById(eq(1))} doAnswer {
            storedSchema
        }
    }
    @Serializable
    private data class TestRecord(
        val str : String
    )
    @Serializable
    private data class TestRecordWithNull(
        val nullableStr : String? = null,
        val intValue : Int
    )
    @Serializable
    @AvroNamespace("custom.namespace")
    private data class TestRecordWithNamespace(
        val float : Double
    )
    @Serializable
    @AvroName("AnotherName")
    private data class TestRecordWithDifferentName(
        val double : Double
    )

    companion object{
        @JvmStatic
        fun createSerializableObjects():Stream<out Any>{
            return Stream.of(
                TestRecord("STTR"),
                TestRecordWithNull(null,2),
                TestRecordWithNull("33",1),
                TestRecordWithNamespace(4.0),
                TestRecordWithDifferentName(2.0)
            )
        }
    }
    @ImplicitReflectionSerializer
    @ParameterizedTest()
    @MethodSource("createSerializableObjects")
    fun testRecordSerDeRoundtrip(toSerialize : Any?){
        val serializer = KafkaAvro4kSerializer(registryMock)
        val topic = "My-Topic"
        val result = serializer.serialize(topic, toSerialize)
        assertNotNull(result)
        result ?: throw Exception("")
        verify(registryMock).getId(eq("$topic-value"), any())
        verify(registryMock, never()).register(any(), any())

        val deserializer = KafkaAvro4kDeserializer(
            registryMock,
            mapOf(
                KafkaAvro4kDeserializerConfig.RECORD_PACKAGES to this::class.java.`package`.name,
                AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://registry"
            )
        )

        val deserializedValue = deserializer.deserialize(topic, result)
        assertEquals(toSerialize, deserializedValue)
    }
}
package com.github.thake.kafka.avro4k.serializer

import com.nhaarman.mockitokotlin2.*
import com.sksamuel.avro4k.AvroName
import com.sksamuel.avro4k.AvroNamespace
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import org.apache.avro.Schema
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class Avro4kSerdeTest {
    val registryMock = mock<SchemaRegistryClient>{
        var storedSchema : Schema? = null
        on{ getId(any(), any())} doAnswer {
            storedSchema = it.getArgument(1)
            1
        } doReturn 1
        on{ register(any(), any())} doAnswer {
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
        val float : Float
    )
    @Serializable
    @AvroName("AnotherName")
    private data class TestRecordWithDifferentName(
        val double : Double
    )

    companion object{
        @JvmStatic
        fun createSerializableObjects(): Stream<out Any> {
            return Stream.of(
                TestRecord("STTR"),
                TestRecordWithNull(null,2),
                TestRecordWithNull("33",1),
                TestRecordWithNamespace(4f),
                TestRecordWithDifferentName(2.0)
            )
        }
    }
    @ImplicitReflectionSerializer
    @ParameterizedTest()
    @MethodSource("createSerializableObjects")
    fun testRecordSerDeRoundtrip(toSerialize : Any){
        val serde = Avro4kSerde<Any>(registryMock)
        val topic = "My-Topic"
        val result = serde.serializer().serialize(topic,toSerialize)
        Assertions.assertNotNull(result)
        result ?: throw Exception("")
        verify(registryMock).getId(eq("$topic-value"), any() )
        verify(registryMock, never()).register(any(), any())

        val deserializer = serde.deserializer()

        val deserializedValue = deserializer.deserialize(topic,result)
        Assertions.assertEquals(toSerialize, deserializedValue)
    }
}
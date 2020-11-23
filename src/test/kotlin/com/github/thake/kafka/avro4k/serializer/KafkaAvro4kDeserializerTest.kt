package com.github.thake.kafka.avro4k.serializer

import com.sksamuel.avro4k.Avro
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.spyk
import kotlinx.serialization.Serializable
import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.avro.io.EncoderFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class KafkaAvro4kDeserializerTest {
    private val registryMock = spyk(MockSchemaRegistryClient())

    @Serializable
    private data class DeserializerTestRecord(
        val str: String
    )

    @Test
    fun testNullRecordInUnion() {
        val deserializer = KafkaAvro4kDeserializer(registryMock)
        deserializer.configure(
            mapOf(
                AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS to "true",
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://registry",
                KafkaAvro4kDeserializerConfig.RECORD_PACKAGES to this::class.java.packageName,
            ),
            false
        )
        val avroSchema = Avro.Companion.default.schema(DeserializerTestRecord.serializer())
        val unionSchema = Schema.createUnion(Schema.create(Schema.Type.NULL), avroSchema)
        val schemaId = 1
        val buffer = ByteBuffer.allocate(8)
        buffer.put(0x00) //Magic byte as in AbstractKafkaSchemaSerDe.MAGIC_BYTE
        buffer.putInt(schemaId) //Schema ID
        buffer.put(0x00) //Actual record. 0x00 indicating the first type of the union. This is "NULL".
        val byteArray: ByteArray = buffer.array()
        every {
            registryMock.getSchemaById(schemaId)
        }.returns(AvroSchema(unionSchema))
        val topic = "My-Topic"
        val result = deserializer.deserialize(topic, byteArray)
        Assertions.assertNull(result)
    }

    @Test
    fun testNonNullRecordInUnion() {
        val deserializer = KafkaAvro4kDeserializer(registryMock)
        deserializer.configure(
            mapOf(
                AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS to "true",
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://registry",
                KafkaAvro4kDeserializerConfig.RECORD_PACKAGES to this::class.java.packageName,
            ),
            false
        )
        val avroSchema = Avro.Companion.default.schema(DeserializerTestRecord.serializer())
        val unionSchema = Schema.createUnion(Schema.create(Schema.Type.NULL), avroSchema)
        val topic = "My-Topic"
        val subject = "$topic-value"
        val schemaId = registryMock.register(subject, AvroSchema(unionSchema))

        val outputStream = ByteArrayOutputStream()
        //Avro Kafka format
        outputStream.write(0x00)//Magic byte as in AbstractKafkaSchemaSerDe.MAGIC_BYTE
        outputStream.write(ByteBuffer.allocate(4).putInt(schemaId).array()) //Schema ID encoded as normal Int
        //Avro data starts
        val serializedRecord = DeserializerTestRecord("test")
        val record =
            GenericRecordBuilder(avroSchema).set(DeserializerTestRecord::str.name, serializedRecord.str).build()
        val datumWriter = GenericDatumWriter<GenericRecord>(unionSchema)
        val encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null)
        datumWriter.write(record, encoder)
        val byteArray: ByteArray = outputStream.toByteArray()
        val result = deserializer.deserialize(topic, byteArray)
        result.shouldBe(serializedRecord)
    }
}
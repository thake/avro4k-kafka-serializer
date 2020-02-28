package com.github.thake.kafka.avro4k.serializer

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.streams.serdes.avro.SpecificAvroDeserializer
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import kotlinx.serialization.ImplicitReflectionSerializer
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import kotlin.reflect.KClass
@ImplicitReflectionSerializer
class Avro4kSerde<T : Any>(client: SchemaRegistryClient? = null) : Serde<T> {
    private val inner: Serde<T>  = Serdes.serdeFrom<T>(
        KafkaAvro4kSerializer(client) as Serializer<T>,
        KafkaAvro4kDeserializer(client) as Deserializer<T>
    )


    override fun serializer(): Serializer<T> {
        return inner.serializer()
    }

    override fun deserializer(): Deserializer<T> {
        return inner.deserializer()
    }

    override fun configure(
        serdeConfig: Map<String?, *>?,
        isSerdeForRecordKeys: Boolean
    ) {
        inner.serializer().configure(serdeConfig, isSerdeForRecordKeys)
        inner.deserializer().configure(serdeConfig, isSerdeForRecordKeys)
    }

    override fun close() {
        inner.serializer().close()
        inner.deserializer().close()
    }
}
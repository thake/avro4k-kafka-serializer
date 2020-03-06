package com.github.thake.kafka.avro4k.serializer

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import kotlinx.serialization.ImplicitReflectionSerializer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer

@ImplicitReflectionSerializer
class Avro4kSerde<T : Any>(client: SchemaRegistryClient? = null) : Serde<T> {
    @Suppress("UNCHECKED_CAST")
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

    override fun configure(configs: Map<String, *>?, isSerdeForRecordKeys: Boolean) {
        inner.serializer().configure(configs, isSerdeForRecordKeys)
        inner.deserializer().configure(configs, isSerdeForRecordKeys)
    }

    override fun close() {
        inner.serializer().close()
        inner.deserializer().close()
    }
}
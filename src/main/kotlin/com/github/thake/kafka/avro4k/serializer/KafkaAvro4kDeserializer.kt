package com.github.thake.kafka.avro4k.serializer

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import kotlinx.serialization.ImplicitReflectionSerializer
import org.apache.kafka.common.serialization.Deserializer
@ImplicitReflectionSerializer
class KafkaAvro4kDeserializer(
    client : SchemaRegistryClient? = null,
    props : Map<String,*>? = null
)  : AbstractKafkaAvro4kDeserializer(), Deserializer<Any> {
    private var isKey = false
    init {
        this.schemaRegistry = client
        props?.let { configure(this.deserializerConfig(it)) }
    }

    override fun configure(configs: Map<String, *>, isKey: Boolean) {
        this.isKey = isKey

        this.configure(KafkaAvro4kDeserializerConfig(configs))
    }


    override fun deserialize(s: String, bytes: ByteArray): Any? {
        return this.deserialize(bytes)
    }

    override fun close() {}
}
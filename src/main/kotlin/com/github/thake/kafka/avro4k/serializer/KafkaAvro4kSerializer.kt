package com.github.thake.kafka.avro4k.serializer

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import kotlinx.serialization.ImplicitReflectionSerializer
import org.apache.kafka.common.serialization.Serializer
@ImplicitReflectionSerializer
class KafkaAvro4kSerializer(
    client : SchemaRegistryClient? = null,
    props : Map<String,*>? = null
) : AbstractKafkaAvro4kSerializer(), Serializer<Any?> {
    private var isKey = false

    init {
        this.schemaRegistry = client
        props?.let { configure(this.serializerConfig(it)) }
    }

    override fun configure(configs: Map<String, *>, isKey: Boolean) {
        this.isKey = isKey
        this.configure(KafkaAvro4kSerializerConfig(configs))
    }


    override fun serialize(topic: String, record: Any?): ByteArray? {
        return this.serializeImpl(this.getSubjectName(topic, isKey, record, Avro4kSchemaUtils.getSchema(record)), record)
    }

    override fun close() {}
}
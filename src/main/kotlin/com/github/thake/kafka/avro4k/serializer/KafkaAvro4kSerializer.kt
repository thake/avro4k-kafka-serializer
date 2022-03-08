package com.github.thake.kafka.avro4k.serializer

import com.github.avrokotlin.avro4k.Avro
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.apache.kafka.common.serialization.Serializer

class KafkaAvro4kSerializer(
    client : SchemaRegistryClient? = null,
    props : Map<String,*>? = null,
    avro: Avro = Avro.default
) : AbstractKafkaAvro4kSerializer(avro), Serializer<Any?> {
    private var isKey = false

    init {
        props?.let { configure(this.serializerConfig(it)) }
        //Set the registry client explicitly after configuration has been applied to override client from configuration
        if (client != null) this.schemaRegistry = client
    }

    override fun configure(configs: Map<String, *>, isKey: Boolean) {
        this.isKey = isKey
        this.configure(KafkaAvro4kSerializerConfig(configs))
    }


    override fun serialize(topic: String?, record: Any?): ByteArray? {
        return record?.let {
            this.serializeImpl(
                this.getSubjectName(
                    topic,
                    isKey,
                    it,
                    AvroSchema(avroSchemaUtils.getSchema(it))
                ), it
            )
        }
    }

    override fun close() {}
}
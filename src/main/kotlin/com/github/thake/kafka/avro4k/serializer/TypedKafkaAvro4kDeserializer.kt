package com.github.thake.kafka.avro4k.serializer

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.apache.avro.Schema
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

class TypedKafkaAvro4kDeserializer<T : Any>(private val type: Class<T>, client : SchemaRegistryClient? = null) : AbstractKafkaAvro4kDeserializer(), Deserializer<T> {
    private val typeNames = type.avroRecordNames
    init {
        this.schemaRegistry = client
    }
    override fun getDeserializedClass(msgSchema: Schema): KClass<*> {
        return if (typeNames.contains(msgSchema.fullName)) {
            Reflection.getOrCreateKotlinClass(type)
        } else {
            throw SerializationException("Could not convert to type $type with schema record name ${msgSchema.fullName}")
        }
    }

    override fun deserialize(topic: String?, data: ByteArray?): T? {
        @Suppress("UNCHECKED_CAST")
        return deserialize(data, avroSchemaUtils.getSchema(type)) as T?
    }
}
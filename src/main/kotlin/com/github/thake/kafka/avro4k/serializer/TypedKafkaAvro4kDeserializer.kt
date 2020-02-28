package com.github.thake.kafka.avro4k.serializer

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.apache.avro.Schema
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

@ImplicitReflectionSerializer
class TypedKafkaAvro4kDeserializer<T : Any>(private val type: Class<T>, client : SchemaRegistryClient? = null) : AbstractKafkaAvro4kDeserializer(), Deserializer<T> {
    private val typeNames = getTypeNames(type)
    private val serializer = Reflection.getOrCreateKotlinClass(type).serializer()
    init {
        this.schemaRegistry = client
    }
    override fun getSerializer(msgSchema: Schema): KSerializer<*> {
        return if (typeNames.contains(msgSchema.fullName)) {
            return serializer
        } else {
            throw SerializationException("Could not convert to type $type with schema record name ${msgSchema.fullName}")
        }
    }

    override fun deserialize(topic: String?, data: ByteArray?): T? {
        return deserialize(data) as T?
    }
}
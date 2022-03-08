package com.github.thake.kafka.avro4k.serializer

import com.github.avrokotlin.avro4k.Avro
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.apache.avro.Schema
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf


class Avro4kSchemaUtils(private val avro: Avro) {
    private val NULL_SCHEMA = Schema.create(Schema.Type.NULL)
    private val BOOLEAN_SCHEMA = Schema.create(Schema.Type.BOOLEAN)
    private val INTEGER_SCHEMA = Schema.create(Schema.Type.INT)
    private val LONG_SCHEMA = Schema.create(Schema.Type.LONG)
    private val FLOAT_SCHEMA = Schema.create(Schema.Type.FLOAT)
    private val DOUBLE_SCHEMA = Schema.create(Schema.Type.DOUBLE)
    private val STRING_SCHEMA = Schema.create(Schema.Type.STRING)
    private val BYTES_SCHEMA = Schema.create(Schema.Type.BYTES)
    private val cachedSchemas = ConcurrentHashMap<KClass<*>, Schema>()


    @OptIn(InternalSerializationApi::class)
    fun getSchema(clazz: KClass<*>): Schema {
        return when {
            Boolean::class.isSuperclassOf(clazz) -> BOOLEAN_SCHEMA
            Int::class.isSuperclassOf(clazz) -> INTEGER_SCHEMA
            Long::class.isSuperclassOf(clazz) -> LONG_SCHEMA
            Float::class.isSuperclassOf(clazz) -> FLOAT_SCHEMA
            Double::class.isSuperclassOf(clazz) -> DOUBLE_SCHEMA
            CharSequence::class.isSuperclassOf(clazz) -> STRING_SCHEMA
            ByteArray::class.isSuperclassOf(clazz) -> BYTES_SCHEMA
            else -> cachedSchemas.computeIfAbsent(clazz) { avro.schema(it.serializer()) }
        }
    }


    fun getSchema(obj: Any?): Schema {
        return if (obj == null) NULL_SCHEMA else getSchema(obj::class)
    }

}
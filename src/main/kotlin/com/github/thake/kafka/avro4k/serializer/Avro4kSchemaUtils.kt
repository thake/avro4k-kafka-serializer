package com.github.thake.kafka.avro4k.serializer

import com.sksamuel.avro4k.Avro
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.apache.avro.Schema
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf


class Avro4kSchemaUtils {

    private val parser = Schema.Parser()
    private val NULL_SCHEMA = createPrimitiveSchema("null")
    private val BOOLEAN_SCHEMA = createPrimitiveSchema("boolean")
    private val INTEGER_SCHEMA = createPrimitiveSchema("int")
    private val LONG_SCHEMA = createPrimitiveSchema("long")
    private val FLOAT_SCHEMA = createPrimitiveSchema("float")
    private val DOUBLE_SCHEMA = createPrimitiveSchema("double")
    private val STRING_SCHEMA = createPrimitiveSchema("string")
    private val BYTES_SCHEMA = createPrimitiveSchema("bytes")
    private val cachedSchemas = ConcurrentHashMap<KClass<*>, Schema>()
    private fun createPrimitiveSchema(type: String): Schema {
        return parser.parse(
            """
            {"type" : "$type"}
            """.trim()
        )
    }

    @OptIn(InternalSerializationApi::class)
    fun getSchema(clazz: KClass<*>): Schema? {
        return when {
            Boolean::class.isSuperclassOf(clazz) -> BOOLEAN_SCHEMA
            Int::class.isSuperclassOf(clazz) -> INTEGER_SCHEMA
            Long::class.isSuperclassOf(clazz) -> LONG_SCHEMA
            Float::class.isSuperclassOf(clazz) -> FLOAT_SCHEMA
            Double::class.isSuperclassOf(clazz) -> DOUBLE_SCHEMA
            CharSequence::class.isSuperclassOf(clazz) -> STRING_SCHEMA
            ByteArray::class.isSuperclassOf(clazz) -> BYTES_SCHEMA
            else -> cachedSchemas.computeIfAbsent(clazz) { Avro.default.schema(it.serializer()) }
        }
    }


    fun getSchema(obj: Any?): Schema? {
        return if (obj == null) NULL_SCHEMA else getSchema(obj::class)
    }

}
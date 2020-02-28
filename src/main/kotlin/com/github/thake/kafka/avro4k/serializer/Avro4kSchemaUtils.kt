
package com.github.thake.kafka.avro4k.serializer

import com.sksamuel.avro4k.Avro
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.serializer
import org.apache.avro.Schema
import org.apache.avro.generic.GenericContainer
import java.util.*

object Avro4kSchemaUtils {

    private val parser = Schema.Parser()
    private val NULL_SCHEMA = createPrimitiveSchema("null")
    private val BOOLEAN_SCHEMA = createPrimitiveSchema("boolean")
    private val INTEGER_SCHEMA = createPrimitiveSchema("int")
    private val LONG_SCHEMA = createPrimitiveSchema("long")
    private val FLOAT_SCHEMA = createPrimitiveSchema("float")
    private val DOUBLE_SCHEMA = createPrimitiveSchema("double")
    private val STRING_SCHEMA = createPrimitiveSchema("string")
    private val BYTES_SCHEMA = createPrimitiveSchema("bytes")
    private fun createPrimitiveSchema(type: String): Schema {
        return parser.parse("""
            {"type" : "$type"}
            """.trim())
    }



    @ImplicitReflectionSerializer
    fun getSchema(obj: Any?): Schema? {
        return when (obj) {
            null -> NULL_SCHEMA
            is Boolean -> BOOLEAN_SCHEMA
            is Int -> INTEGER_SCHEMA
            is Long -> LONG_SCHEMA
            is Float -> FLOAT_SCHEMA
            is Double -> DOUBLE_SCHEMA
            is CharSequence -> STRING_SCHEMA
            is ByteArray -> BYTES_SCHEMA
            else -> Avro.default.schema(obj::class.serializer())
        }
    }

}
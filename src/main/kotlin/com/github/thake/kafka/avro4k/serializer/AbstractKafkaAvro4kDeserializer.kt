package com.github.thake.kafka.avro4k.serializer

import com.sksamuel.avro4k.Avro
import com.sksamuel.avro4k.io.AvroFormat
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.io.BinaryDecoder
import org.apache.avro.io.DecoderFactory
import org.apache.kafka.common.errors.SerializationException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.reflect.KClass

abstract class AbstractKafkaAvro4kDeserializer : AbstractKafkaAvro4kSerDe() {
    companion object {
        private var specificRecordLookupForClassLoader: MutableMap<Pair<List<String>, ClassLoader>, RecordLookup> =
            mutableMapOf()

        private fun getLookup(recordPackages: List<String>, classLoader: ClassLoader) =
            specificRecordLookupForClassLoader.getOrPut(Pair(recordPackages, classLoader),
                { RecordLookup(recordPackages, classLoader) })
    }

    private var recordPackages: List<String> = emptyList()
    private var binaryDecoder: BinaryDecoder? = null
    protected val avroSchemaUtils = Avro4kSchemaUtils()


    protected fun configure(config: KafkaAvro4kDeserializerConfig) {
        val configuredPackages = config.getRecordPackages()
        if (configuredPackages.isEmpty()) {
            throw IllegalArgumentException("${KafkaAvro4kDeserializerConfig.RECORD_PACKAGES} is not set correctly.")
        }
        recordPackages = configuredPackages
        super.configure(config)
    }

    protected fun deserializerConfig(props: Map<String, *>): KafkaAvro4kDeserializerConfig {
        return KafkaAvro4kDeserializerConfig(props)
    }


    @Throws(SerializationException::class)
    protected fun deserialize(
        payload: ByteArray?, readerSchema: Schema?
    ): Any? {

        return if (payload == null) {
            null
        } else {
            var id = -1
            try {
                val buffer = getByteBuffer(payload)
                id = buffer.int
                val writerSchema = getSchemaByIdWithRetry(id)
                    ?: throw SerializationException("Could not find schema with id $id in schema registry")
                val length = buffer.limit() - 1 - 4
                val bytes = ByteArray(length)
                buffer[bytes, 0, length]
                return ByteArrayInputStream(bytes).use {
                    deserialize(writerSchema, readerSchema, it)
                }
            } catch (re: RuntimeException) {
                throw SerializationException("Error deserializing Avro message for schema id $id with avro4k", re)
            } catch (io: IOException) {
                throw SerializationException("Error deserializing Avro message for schema id $id with avro4k", io)
            } catch (registry: RestClientException) {
                throw SerializationException("Error retrieving Avro schema for id $id from schema registry.", registry)
            }
        }
    }

    private fun deserializeUnion(writerSchema: Schema, readerSchema: Schema?, bytes: InputStream): Any? {
        val decoder = DecoderFactory.get().directBinaryDecoder(bytes, binaryDecoder)
        val unionTypeIndex = decoder.readInt()
        val recordSchema = writerSchema.types[unionTypeIndex]
        if (recordSchema.type == Schema.Type.NULL) return null
        binaryDecoder = decoder
        //Decode avro type as record
        return deserialize(recordSchema, readerSchema, decoder.inputStream())
    }


    private fun deserialize(writerSchema: Schema, readerSchema: Schema?, bytes: InputStream) =
        when (writerSchema.type) {
            Schema.Type.BYTES -> bytes.readAllBytes()
            Schema.Type.UNION -> deserializeUnion(writerSchema, readerSchema, bytes)
            Schema.Type.RECORD -> deserializeRecord(writerSchema, readerSchema, bytes)
            else -> {
                val decoder = DecoderFactory.get().directBinaryDecoder(bytes, null)
                val datumReader = GenericDatumReader<Any>(writerSchema, readerSchema ?: writerSchema)
                val deserialized = datumReader.read(null, decoder)
                if (writerSchema.type == Schema.Type.STRING) {
                    deserialized.toString()
                } else {
                    deserialized
                }
            }
        }

    @OptIn(InternalSerializationApi::class)
    private fun deserializeRecord(
        writerSchema: Schema,
        readerSchema: Schema?,
        bytes: InputStream
    ): Any {
        val deserializedClass = getDeserializedClass(writerSchema)
        return Avro.default.openInputStream(deserializedClass.serializer()) {
            format = AvroFormat.BinaryFormat
            this.writerSchema = writerSchema
            this.readerSchema = readerSchema ?: avroSchemaUtils.getSchema(deserializedClass)
        }.from(bytes).nextOrThrow()
    }

    private fun getLookup(contextClassLoader: ClassLoader) = Companion.getLookup(recordPackages, contextClassLoader)

    protected open fun getDeserializedClass(msgSchema: Schema): KClass<*> {
        //First lookup using the context class loader
        val contextClassLoader = Thread.currentThread().contextClassLoader
        var objectClass: Class<*>? = null
        if (contextClassLoader != null) {
            objectClass = getLookup(contextClassLoader).lookupType(msgSchema)
        }
        if (objectClass == null) {
            //Fallback to classloader of this class
            objectClass = getLookup(AbstractKafkaAvro4kDeserializer::class.java.classLoader).lookupType(msgSchema)
                ?: throw SerializationException("Couldn't find matching class for record type ${msgSchema.fullName}. Full schema: $msgSchema")
        }

        return objectClass.kotlin
    }


}
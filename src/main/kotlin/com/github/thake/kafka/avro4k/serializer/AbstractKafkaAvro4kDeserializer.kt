package com.github.thake.kafka.avro4k.serializer

import com.sksamuel.avro4k.*
import com.sksamuel.avro4k.io.AvroFormat
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDe
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.apache.avro.Schema
import org.apache.avro.specific.SpecificData
import org.apache.kafka.common.errors.SerializationException
import org.reflections.Reflections
import org.reflections.scanners.Scanner
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.io.IOException
import java.nio.ByteBuffer
@ImplicitReflectionSerializer
abstract class AbstractKafkaAvro4kDeserializer : AbstractKafkaAvroSerDe() {
    private lateinit var recordPackages : List<String>
    //A map of alternative names for types that are annotated with Avro annotations

    private val recordNameToType : Map<String, Class<*>> by lazy{
        if (recordPackages.isNotEmpty()) {
            var configBuilder = ConfigurationBuilder().
                    addScanners(TypeAnnotationsScanner(), SubTypesScanner()).
                    forPackages(*this.recordPackages.toTypedArray())

            val reflection = Reflections(configBuilder)
            val avroTypes = HashSet<Class<*>>().apply {
                this.addAll(reflection.getTypesAnnotatedWith(AvroName::class.java,true))
                this.addAll(reflection.getTypesAnnotatedWith(AvroNamespace::class.java,true))
                this.addAll(reflection.getTypesAnnotatedWith(AvroAlias::class.java,true))
            }
            avroTypes.flatMap { type ->
                getTypeNames(type).map { Pair(it,type) }
            }.toMap()
        }else{
            emptyMap()
        }
    }

    protected fun getTypeNames(type : Class<*>) : List<String> {
        val descriptor = type.kotlin.serializer().descriptor
        val naming = RecordNaming(descriptor)
        val aliases = AnnotationExtractor(descriptor.getEntityAnnotations()).aliases()
        val normalNameMapping = "${naming.namespace()}.${naming.name()}"
        return if (aliases.isNotEmpty()) {
            val mappings = mutableListOf(normalNameMapping)
            aliases.forEach {alias ->
                mappings.add(if (alias.contains('.')) {
                    alias
                } else {
                    "${naming.namespace()}.$alias"
                })
            }
            mappings
        } else {
            listOf(normalNameMapping)
        }
    }
    protected fun configure(config: KafkaAvro4kDeserializerConfig) {
        recordPackages = config.getRecordPackages()
        configureClientProperties(config)
    }

    protected fun deserializerConfig(props: Map<String, *>): KafkaAvro4kDeserializerConfig {
        return KafkaAvro4kDeserializerConfig(props)
    }


    private fun getByteBuffer(payload: ByteArray): ByteBuffer {
        val buffer = ByteBuffer.wrap(payload)
        return if (buffer.get().toInt() != 0) {
            throw SerializationException("Unknown magic byte!")
        } else {
            buffer
        }
    }

    @Throws(SerializationException::class)
    protected fun deserialize(
        payload: ByteArray?
    ): Any? {
        return if (payload == null) {
            null
        } else {
            var id = -1
            try {
                val buffer = getByteBuffer(payload)
                id = buffer.int
                var schema = schemaRegistry.getById(id)
                val length = buffer.limit() - 1 - 4
                val bytes = ByteArray(length)
                buffer[bytes, 0, length]
                if (schema.type == Schema.Type.BYTES) {
                    bytes
                } else {
                    val reader = getSerializer(schema)
                    Avro.default.openInputStream(reader) {
                        format = AvroFormat.BinaryFormat
                        writerSchema = schema
                    }.from(bytes).nextOrThrow()
                }
            } catch (re: RuntimeException) {
                throw SerializationException("Error deserializing Avro message for schema id $id with avro4k",re)
            } catch (io: IOException) {
                throw SerializationException("Error deserializing Avro message for schema id $id with avro4k",io)
            } catch (registry: RestClientException) {
                throw SerializationException("Error retrieving Avro schema for id $id from schema registry.",registry)
            }
        }
    }


    protected open fun getSerializer(msgSchema: Schema): KSerializer<*> {
        Avro
        var objectClass = SpecificData.get().getClass(msgSchema);
        if(objectClass == null){
           objectClass = recordNameToType[msgSchema.fullName] ?: throw SerializationException("Couldn't find matching class for record type ${msgSchema.fullName}. Full schema: $msgSchema")
        }

        return objectClass.kotlin.serializer()
    }


}
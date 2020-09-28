package com.github.thake.kafka.avro4k.serializer

import com.sksamuel.avro4k.AnnotationExtractor
import com.sksamuel.avro4k.RecordNaming
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

/**
 * A list containing all avro record names that represent this class.
 */
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
val Class<*>.avroRecordNames: List<String>
    get() {
        val descriptor = this.kotlin.serializer().descriptor
        val naming = RecordNaming(descriptor)
        val aliases = AnnotationExtractor(descriptor.annotations).aliases()
        val normalNameMapping = "${naming.namespace()}.${naming.name()}"
        return if (aliases.isNotEmpty()) {
            val mappings = mutableListOf(normalNameMapping)
            aliases.forEach { alias ->
                mappings.add(
                    if (alias.contains('.')) {
                        alias
                    } else {
                        "${naming.namespace()}.$alias"
                    }
                )
            }
            mappings
        } else {
            listOf(normalNameMapping)
        }
    }
package com.github.thake.kafka.avro4k.serializer

import io.confluent.common.config.ConfigDef
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig

class KafkaAvro4kDeserializerConfig(props : Map<String,*>) : AbstractKafkaAvroSerDeConfig(config,props) {
    fun getRecordPackages() = getString(RECORD_PACKAGES)?.split(",")?.map { it.trim() }?.toList() ?: emptyList()
    companion object{
        const val RECORD_PACKAGES ="record.packages"
        private val config: ConfigDef
        init {
            config = baseConfigDef().define(
                RECORD_PACKAGES,
                ConfigDef.Type.STRING,
                null,
                ConfigDef.Importance.LOW,
                "The packages in which record types annotated with @AvroName, @AvroAlias and @AvroNamespace can be found. Packages are separated by a colon ','."
            )
        }
    }
}
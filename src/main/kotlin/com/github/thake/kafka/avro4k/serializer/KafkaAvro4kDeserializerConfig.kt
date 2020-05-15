package com.github.thake.kafka.avro4k.serializer

import io.confluent.common.config.ConfigDef

class KafkaAvro4kDeserializerConfig(props: Map<String, *>) : AbstractKafkaAvro4kSerDeConfig(
    baseConfigDef().define(
        RECORD_PACKAGES,
        ConfigDef.Type.STRING,
        null,
        ConfigDef.Importance.HIGH,
        "The packages in which record types annotated with @AvroName, @AvroAlias and @AvroNamespace can be found. Packages are separated by a colon ','."
    ), props
) {
    fun getRecordPackages() = getString(RECORD_PACKAGES)?.split(",")?.map { it.trim() }?.toList() ?: emptyList()

    companion object {
        const val RECORD_PACKAGES = "record.packages"
    }
}
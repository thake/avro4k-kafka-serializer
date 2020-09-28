package com.github.thake.kafka.avro4k.serializer


import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import org.apache.kafka.common.config.ConfigDef

abstract class AbstractKafkaAvro4kSerDeConfig(configDef: ConfigDef, props: Map<String, Any?>) :
    AbstractKafkaSchemaSerDeConfig(
        configDef.define(
            SCHEMA_REGISTRY_RETRY_ATTEMPTS_CONFIG,
            ConfigDef.Type.INT,
            SCHEMA_REGISTRY_RETRY_ATTEMPTS_DEFAULT,
            ConfigDef.Importance.LOW,
            SCHEMA_REGISTRY_RETRY_ATTEMPTS_DOC
        )
            .define(
                SCHEMA_REGISTRY_RETRY_JITTER_BASE_CONFIG,
                ConfigDef.Type.LONG,
                SCHEMA_REGISTRY_RETRY_JITTER_BASE_DEFAULT,
                ConfigDef.Importance.LOW,
                SCHEMA_REGISTRY_RETRY_JITTER_BASE_DOC
            )
            .define(
                SCHEMA_REGISTRY_RETRY_JITTER_MAX_CONFIG,
                ConfigDef.Type.LONG,
                SCHEMA_REGISTRY_RETRY_JITTER_MAX_DEFAULT,
                ConfigDef.Importance.LOW,
                SCHEMA_REGISTRY_RETRY_JITTER_MAX_DOC
            )
        , props
    ) {
    companion object {
        const val SCHEMA_REGISTRY_RETRY_ATTEMPTS_CONFIG = "schema.registry.retry.attempts"
        const val SCHEMA_REGISTRY_RETRY_ATTEMPTS_DEFAULT = 5
        const val SCHEMA_REGISTRY_RETRY_ATTEMPTS_DOC =
            "Number of retry attempts that will be made if the schema registry seems to have a problem with requesting a schema."
        const val SCHEMA_REGISTRY_RETRY_JITTER_BASE_CONFIG = "schema.registry.retry.jitter.base"
        const val SCHEMA_REGISTRY_RETRY_JITTER_BASE_DOC =
            "Milliseconds that are used as a base for the jitter calculation (sleep = random_between(0, min(max, base * 2 ** attempt)))"
        const val SCHEMA_REGISTRY_RETRY_JITTER_BASE_DEFAULT = 10L
        const val SCHEMA_REGISTRY_RETRY_JITTER_MAX_CONFIG = "schema.registry.retry.jitter.max"
        const val SCHEMA_REGISTRY_RETRY_JITTER_MAX_DOC =
            "Milliseconds that are used as max for the jitter calculation (sleep = random_between(0, min(max, base * 2 ** attempt)))"
        const val SCHEMA_REGISTRY_RETRY_JITTER_MAX_DEFAULT = 5000L
    }

    val schemaRegistryRetryAttempts: Int
        get() = this.get(SCHEMA_REGISTRY_RETRY_ATTEMPTS_CONFIG) as Int
    val schemaRegistryRetryJitterBase: Long
        get() = this.get(SCHEMA_REGISTRY_RETRY_JITTER_BASE_CONFIG) as Long
    val schemaRegistryRetryJitterMax: Long
        get() = this.get(SCHEMA_REGISTRY_RETRY_JITTER_MAX_CONFIG) as Long
}
package com.github.thake.kafka.avro4k.serializer

import com.github.michaelbull.retry.ContinueRetrying
import com.github.michaelbull.retry.StopRetrying
import com.github.michaelbull.retry.context.retryStatus
import com.github.michaelbull.retry.policy.RetryPolicy
import com.github.michaelbull.retry.policy.fullJitterBackoff
import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDe
import kotlinx.coroutines.runBlocking
import org.apache.avro.Schema
import org.apache.kafka.common.errors.SerializationException
import org.slf4j.LoggerFactory
import kotlin.coroutines.coroutineContext

private val logger = LoggerFactory.getLogger(AbstractKafkaAvro4kSerDe::class.java)

abstract class AbstractKafkaAvro4kSerDe : AbstractKafkaSchemaSerDe() {
    var retryAttempts = AbstractKafkaAvro4kSerDeConfig.SCHEMA_REGISTRY_RETRY_ATTEMPTS_DEFAULT
        private set
    var retryJitterBase = AbstractKafkaAvro4kSerDeConfig.SCHEMA_REGISTRY_RETRY_JITTER_BASE_DEFAULT
        private set
    var retryJitterMax = AbstractKafkaAvro4kSerDeConfig.SCHEMA_REGISTRY_RETRY_JITTER_MAX_DEFAULT
        private set

    private val retryRestClientException: RetryPolicy<Throwable> = {
        if (reason is RestClientException) {
            val retryStatus = coroutineContext.retryStatus
            logger.warn(
                "Caught exception while trying to talk to the schema registry. Retry attempt ${retryStatus.attempt}",
                reason
            )
            ContinueRetrying
        } else StopRetrying
    }
    var retryPolicy = calcRetryPolicy()
        private set

    private fun calcRetryPolicy(): RetryPolicy<Throwable> {
        return retryRestClientException + limitAttempts(retryAttempts) + fullJitterBackoff(
            base = retryJitterBase,
            max = retryJitterMax
        )
    }

    fun configure(config: AbstractKafkaAvro4kSerDeConfig) {
        this.retryAttempts = config.schemaRegistryRetryAttempts
        this.retryJitterBase = config.schemaRegistryRetryJitterBase
        this.retryJitterMax = config.schemaRegistryRetryJitterMax
        this.retryPolicy = calcRetryPolicy()
        this.configureClientProperties(config, AvroSchemaProvider())
    }

    private fun <T> doCallToSchemaRegistry(
        errMsgProvider: (e: RestClientException) -> String = { "Error calling schema registry" },
        block: () -> T
    ): T {
        return try {
            runBlocking {
                retry(retryPolicy) {
                    block.invoke()
                }
            }
        } catch (e: RestClientException) {
            throw SerializationException(errMsgProvider.invoke(e), e)
        }
    }

    fun registerWithRetry(subject: String?, schema: Schema?): Int {
        return doCallToSchemaRegistry({ "Error registering Avro schema in schema registry for subject '$subject': $schema" }) {
            register(subject, AvroSchema(schema))
        }
    }

    fun getSchemaIdWithRetry(subject: String?, schema: Schema?): Int {
        return doCallToSchemaRegistry({ "Error retrieving Avro schema id from schema registry for subject '$subject' and schema: $schema" }) {
            schemaRegistry.getId(subject, AvroSchema(schema) as ParsedSchema?)
        }
    }

    fun getSchemaByIdWithRetry(id: Int): Schema? {
        return doCallToSchemaRegistry({ "Error retrieving Avro schema by id $id from schema registry." }) {
            (schemaRegistry.getSchemaById(id) as? AvroSchema)?.rawSchema()
        }
    }


}
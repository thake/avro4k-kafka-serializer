package com.github.thake.kafka.avro4k.serializer

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig

class KafkaAvro4kSerializerConfig(props : Map<String,Any?>) : AbstractKafkaAvroSerDeConfig(baseConfigDef(),props) {
}
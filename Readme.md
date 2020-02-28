# Kafka avro4k serializer / deserializer

This project implements a Kafka serializer / deserializer that integrates with the confluent schema registry and
leverages [avro4k](https://github.com/sksamuel/avro4k). It is based on confluent's [Kafka Serializer]((https://github.com/confluentinc/schema-registry/tree/master/avro-serializer)).
As such, this implementations can be used to in several projects (i.e. spring)

### Example configuration

Spring Boot with kafka integration:

Serializer:


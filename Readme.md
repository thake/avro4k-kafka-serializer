# Kafka avro4k serializer / deserializer

This project implements a Kafka serializer / deserializer that integrates with the confluent schema registry and
leverages [avro4k](https://github.com/sksamuel/avro4k). It is based on confluent's [Kafka Serializer]((https://github.com/confluentinc/schema-registry/tree/master/avro-serializer)).
As such, this implementations can be used to in several projects (i.e. spring)

## Example configuration

### Spring Cloud Stream with Kafka

```javascript
spring:
    application:
        name: <your-application>
    kafka:
        bootstrap-servers:
            - <your-kafka-bootstrap-server>
    cloud:
        stream:
            default:
                consumer:
                    useNativeDecoding: true
                producer:
                    useNativeEncoding: true
            kafka:
                streams:
                    binder:
                        brokers: <your-broker>
                        configuration:
                            schema.registry.url: <your-registry>
                            record.packages: <packages-of-avro4k-classes>
                            default.key.serde: com.github.thake.kafka.avro4k.serializer.Avro4kSerde
                            default.value.serde: com.github.thake.kafka.avro4k.serializer.Avro4kSerde
                    default:
                        producer:
                            keySerde: com.github.thake.kafka.avro4k.serializer.Avro4kSerde
                            valueSerde: com.github.thake.kafka.avro4k.serializer.Avro4kSerde
                        consumer:
                            keySerde: com.github.thake.kafka.avro4k.serializer.Avro4kSerde
                            valueSerde: com.github.thake.kafka.avro4k.serializer.Avro4kSerde
...
```
### Maven
```xml
<dependency>
    <groupId>com.github.thake.avro4k</groupId>
    <artifactId>avro4k-kafka-serializer</artifactId>
    <version>0.2.0</version>
</dependency>
```

# Kafka avro4k serializer / deserializer
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.thake.avro4k/avro4k-kafka-serializer/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.thake.avro4k/avro4k-kafka-serializer)

This project implements a Kafka serializer / deserializer that integrates with the confluent schema registry and
leverages [avro4k](https://github.com/avro-kotlin/avro4k). It is based on confluent's [Kafka Serializer]((https://github.com/confluentinc/schema-registry/tree/master/avro-serializer)).
As such, this implementations can be used to in several projects (i.e. spring)

This SerDe supports retrying of failed calls to the schema registry (i.e. due to flaky network). Confluent's Serde does not implement this yet.
See https://github.com/confluentinc/schema-registry/issues/928.

## Confluent Versions

Version 0.10.x is compatible with Apache Kafka 2.5.x / Confluent 5.5.x and avro4k < 1.0

Version 0.11.x+ is compatible with Apache Kafka 2.6.x / Confluent 6.0.0 and avro4k < 1.0

Version > 0.13 is compatible with Apache Kafka 2.6.x/3.x (Confluent 6.x/7.x) and avro4k 1.x

## Example usage

You can find an example configuration of a Kafka Consumer, Kafka Producer and Kafka Streams application in the [ConfluentIT](./src/integrationTest/kotlin/com/github/thake/kafka/avro4k/serializer/ConfluentIT.kt) integration test.

### Spring Cloud Stream with Kafka

```javascript
spring:
    application:
        name: <your-application>
            kafka:
            bootstrap-servers:
            -
            <your-kafka-bootstrap-server>
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
                            schema.registry.retry.attemps: 3
                            schema.registry.retry.jitter.base: 10
                            schema.registry.retry.jitter.max: 5000
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
    <version>VERSION</version>
</dependency>
```

### Configuration options

- `schema.registry.url`  
  Comma-separated list of URLs for schema registry instances that can
  be used to register or look up schemas. If you wish to get a
  connection to a mocked schema registry for testing, you can specify
  a scope using the `mock://` pseudo-protocol. For example,
  `mock://my-scope-name` corresponds to
  `MockSchemaRegistry.getClientForScope("my-scope-name")`.

  - Type: list
  - Importance: high

- `record.packages`  
  The packages in which record types annotated with `@AvroName`,
  `@AvroAlias` and `@AvroNamespace` can be found. Packages are separated
  by a colon `,`. Only needed for deserialization.

  - Type: string
  - Default: null
  - Importance: high

- `schema.registry.ssl.key.password`  
  The password of the private key in the key store file or the PEM key
  specified in `ssl.keystore.key`. This is required for clients only
  if two-way authentication is configured.

  - Type: password
  - Default: null
  - Importance: high

- `schema.registry.ssl.keystore.certificate.chain`  
  Certificate chain in the format specified by `ssl.keystore.type`.
  Default SSL engine factory supports only PEM format with a list of
  X.509 certificates

  - Type: password
  - Default: null
  - Importance: high

- `schema.registry.ssl.keystore.key`  
  Private key in the format specified by `ssl.keystore.type`. Default
  SSL engine factory supports only PEM format with PKCS\#8 keys. If
  the key is encrypted, key password must be specified using
  `ssl.key.password`

  - Type: password
  - Default: null
  - Importance: high

- `schema.registry.ssl.keystore.location`  
  The location of the key store file. This is optional for client and
  can be used for two-way authentication for client.

  - Type: string
  - Default: null
  - Importance: high

- `schema.registry.ssl.keystore.password`  
  The store password for the key store file. This is optional for
  client and only needed if `ssl.keystore.location` is configured. Key
  store password is not supported for PEM format.

  - Type: password
  - Default: null
  - Importance: high

- `schema.registry.ssl.truststore.certificates`  
  Trusted certificates in the format specified by
  `ssl.truststore.type`. Default SSL engine factory supports only PEM
  format with X.509 certificates.

  - Type: password
  - Default: null
  - Importance: high

- `schema.registry.ssl.truststore.location`  
  The location of the trust store file.

  - Type: string
  - Default: null
  - Importance: high

- `schema.registry.ssl.truststore.password`  
  The password for the trust store file. If a password is not set,
  trust store file configured will still be used, but integrity
  checking is disabled. Trust store password is not supported for PEM
  format.

  - Type: password
  - Default: null
  - Importance: high

- `auto.register.schemas`  
  Specify if the Serializer should attempt to register the Schema with
  Schema Registry

  - Type: boolean
  - Default: true
  - Importance: medium

- `basic.auth.credentials.source`  
  Specify how to pick the credentials for Basic Auth header. The
  supported values are URL, USER\_INFO and SASL\_INHERIT

  - Type: string
  - Default: URL
  - Importance: medium

- `basic.auth.user.info`  
  Specify the user info for Basic Auth in the form of
  {username}:{password}

  - Type: password
  - Default: \[hidden\]
  - Importance: medium

- `bearer.auth.credentials.source`  
  Specify how to pick the credentials for Bearer Auth header.

  - Type: string
  - Default: STATIC\_TOKEN
  - Importance: medium

- `bearer.auth.token`  
  Specify the Bearer token to be used for authentication

  - Type: password
  - Default: \[hidden\]
  - Importance: medium

- `context.name.strategy`  
  A class used to determine the schema registry context.

  - Type: class
  - Default:
    io.confluent.kafka.serializers.context.NullContextNameStrategy
  - Importance: medium

- `key.subject.name.strategy`  
  Determines how to construct the subject name under which the key
  schema is registered with the schema registry. By default,
  \<topic\>-key is used as subject.

  - Type: class
  - Default:
    io.confluent.kafka.serializers.subject.TopicNameStrategy
  - Importance: medium

- `normalize.schemas`  
  Whether to normalize schemas, which generally ignores ordering when
  it is not significant

  - Type: boolean
  - Default: false
  - Importance: medium

- `schema.registry.basic.auth.user.info`  
  Specify the user info for Basic Auth in the form of
  {username}:{password}

  - Type: password
  - Default: \[hidden\]
  - Importance: medium

- `schema.registry.ssl.enabled.protocols`  
  The list of protocols enabled for SSL connections. The default is
  `TLSv1.2,TLSv1.3` when running with Java 11 or newer, `TLSv1.2`
  otherwise. With the default value for Java 11, clients and servers
  will prefer TLSv1.3 if both support it and fallback to TLSv1.2
  otherwise (assuming both support at least TLSv1.2). This default
  should be fine for most cases. Also see the config documentation for
  <span class="title-ref">ssl.protocol</span>.

  - Type: list
  - Default: TLSv1.2,TLSv1.3
  - Importance: medium

- `schema.registry.ssl.keystore.type`  
  The file format of the key store file. This is optional for client.

  - Type: string
  - Default: JKS
  - Importance: medium

- `schema.registry.ssl.protocol`  
  The SSL protocol used to generate the SSLContext. The default is
  `TLSv1.3` when running with Java 11 or newer, `TLSv1.2` otherwise.
  This value should be fine for most use cases. Allowed values in
  recent JVMs are `TLSv1.2` and `TLSv1.3`. `TLS`, `TLSv1.1`, `SSL`,
  `SSLv2` and `SSLv3` may be supported in older JVMs, but their usage
  is discouraged due to known security vulnerabilities. With the
  default value for this config and `ssl.enabled.protocols`, clients
  will downgrade to `TLSv1.2` if the server does not support
  `TLSv1.3`. If this config is set to `TLSv1.2`, clients will not use
  `TLSv1.3` even if it is one of the values in ssl.enabled.protocols
  and the server only supports `TLSv1.3`.

  - Type: string
  - Default: TLSv1.3
  - Importance: medium

- `schema.registry.ssl.provider`  
  The name of the security provider used for SSL connections. Default
  value is the default security provider of the JVM.

  - Type: string
  - Default: null
  - Importance: medium

- `schema.registry.ssl.truststore.type`  
  The file format of the trust store file.

  - Type: string
  - Default: JKS
  - Importance: medium

- `value.subject.name.strategy`  
  Determines how to construct the subject name under which the value
  schema is registered with the schema registry. By default,
  \<topic\>-value is used as subject.

  - Type: class
  - Default:
    io.confluent.kafka.serializers.subject.TopicNameStrategy
  - Importance: medium

- `id.compatibility.strict`  
  Whether to check for backward compatibility between the schema with
  the given ID and the schema of the object to be serialized

  - Type: boolean
  - Default: true
  - Importance: low

- `latest.compatibility.strict`  
  Whether to check for backward compatibility between the latest
  subject version and the schema of the object to be serialized

  - Type: boolean
  - Default: true
  - Importance: low

- `max.schemas.per.subject`  
  Maximum number of schemas to create or cache locally.

  - Type: int
  - Default: 1000
  - Importance: low

- `proxy.host`  
  The hostname, or address, of the proxy server that will be used to
  connect to the schema registry instances.

  - Type: string
  - Default: ""
  - Importance: low

- `proxy.port`  
  The port number of the proxy server that will be used to connect to
  the schema registry instances.

  - Type: int
  - Default: -1
  - Importance: low

- `schema.reflection`  
  If true, uses the reflection API when serializing/deserializing

  - Type: boolean
  - Default: false
  - Importance: low

- `schema.registry.retry.attempts`  
  Number of retry attempts that will be made if the schema registry
  seems to have a problem with requesting a schema.

  - Type: int
  - Default: 5
  - Importance: low

- `schema.registry.retry.jitter.base`  
  Milliseconds that are used as a base for the jitter calculation
  (sleep = random\_between(0, min(max, base \* 2 \*\* attempt)))

  - Type: long
  - Default: 10
  - Importance: low

- `schema.registry.retry.jitter.max`  
  Milliseconds that are used as max for the jitter calculation (sleep
  = random\_between(0, min(max, base \* 2 \*\* attempt)))

  - Type: long
  - Default: 5000
  - Importance: low

- `schema.registry.ssl.cipher.suites`  
  A list of cipher suites. This is a named combination of
  authentication, encryption, MAC and key exchange algorithm used to
  negotiate the security settings for a network connection using TLS
  or SSL network protocol. By default all the available cipher suites
  are supported.

  - Type: list
  - Default: null
  - Importance: low

- `schema.registry.ssl.endpoint.identification.algorithm`  
  The endpoint identification algorithm to validate server hostname
  using server certificate.

  - Type: string
  - Default: https
  - Importance: low

- `schema.registry.ssl.engine.factory.class`  
  The class of type
  org.apache.kafka.common.security.auth.SslEngineFactory to provide
  SSLEngine objects. Default value is
  org.apache.kafka.common.security.ssl.DefaultSslEngineFactory

  - Type: class
  - Default: null
  - Importance: low

- `schema.registry.ssl.keymanager.algorithm`  
  The algorithm used by key manager factory for SSL connections.
  Default value is the key manager factory algorithm configured for
  the Java Virtual Machine.

  - Type: string
  - Default: SunX509
  - Importance: low

- `schema.registry.ssl.secure.random.implementation`  
  The SecureRandom PRNG implementation to use for SSL cryptography
  operations.

  - Type: string
  - Default: null
  - Importance: low

- `schema.registry.ssl.trustmanager.algorithm`  
  The algorithm used by trust manager factory for SSL connections.
  Default value is the trust manager factory algorithm configured for
  the Java Virtual Machine.

  - Type: string
  - Default: PKIX
  - Importance: low

- `use.latest.version`  
  Specify if the Serializer should use the latest subject version for
  serialization

  - Type: boolean
  - Default: false
  - Importance: low

- `use.schema.id`
  Schema ID to use for serialization
  - Type: int
  - Default: -1
  - Importance: low

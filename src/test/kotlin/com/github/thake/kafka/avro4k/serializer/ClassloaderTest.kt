package com.github.thake.kafka.avro4k.serializer

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch

@Serializable
data class SimpleTest(
    val str: String
)

@ImplicitReflectionSerializer
class ClassloaderTest {
    private val config: Map<String, String> = mapOf(
        KafkaAvro4kDeserializerConfig.RECORD_PACKAGES to this::class.java.packageName,
        AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://registry"
    )
    private val serializer = KafkaAvro4kSerializer(null, config).apply { configure(config, false) }
    private val deserializer = KafkaAvro4kDeserializer(null, config).apply { configure(config, false) }

    @Test
    @Disabled
    fun deserializeWithDifferentClassloader() {
        val byteArray = serializer.serialize("A", SimpleTest("AAA"))
        val newClassLoader = object : ClassLoader() {

            override fun loadClass(name: String): Class<*> {
                return if (name.contains("com.github.thake.kafka.avro4k.serializer.SimpleTest")) findClass(name)
                else super.loadClass(name)
            }

            @Throws(ClassNotFoundException::class)
            override fun findClass(name: String): Class<*> {
                val b = loadClassFromFile(name)
                return defineClass(name, b, 0, b.size, null)
            }

            private fun loadClassFromFile(fileName: String): ByteArray {
                val inputStream = javaClass.classLoader
                    .getResourceAsStream(fileName.replace('.', File.separatorChar) + ".class")
                val buffer: ByteArray
                val byteStream = ByteArrayOutputStream()
                var nextValue = 0
                try {
                    while (inputStream.read().also { nextValue = it } != -1) {
                        byteStream.write(nextValue)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                buffer = byteStream.toByteArray()
                return buffer
            }
        }
        val countDown = CountDownLatch(1)
        var result: Any? = null
        val testThread = Thread {
            try {
                assertEquals(newClassLoader, Thread.currentThread().contextClassLoader)
                result = deserializer.deserialize("s", byteArray)
            } finally {
                countDown.countDown()
            }
        }
        testThread.contextClassLoader = newClassLoader
        testThread.start()
        countDown.await()

        assertNotNull(result)
        assertEquals(newClassLoader, result!!.javaClass.classLoader)
    }

    @Test
    fun deserializeWithoutContextClassloader() {
        val byteArray = serializer.serialize("A", SimpleTest("AAA"))
        val countDown = CountDownLatch(1)
        var result: Any? = null
        val testThread = Thread {
            result = deserializer.deserialize("s", byteArray)
            countDown.countDown()
        }
        testThread.contextClassLoader = null
        testThread.start()
        countDown.await()

        assertNotNull(result)
        assertEquals(ClassloaderTest::class.java.classLoader, result!!.javaClass.classLoader)
    }


    @Test
    fun deserializeWithNormalClassloader() {
        val byteArray = serializer.serialize("A", SimpleTest("AAA"))
        var result = deserializer.deserialize("s", byteArray)

        assertNotNull(result)
        assertEquals(ClassloaderTest::class.java.classLoader, result!!.javaClass.classLoader)
    }
}
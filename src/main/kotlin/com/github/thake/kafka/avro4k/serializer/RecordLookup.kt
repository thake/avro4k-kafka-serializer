package com.github.thake.kafka.avro4k.serializer

import com.sksamuel.avro4k.AvroAlias
import com.sksamuel.avro4k.AvroAliases
import com.sksamuel.avro4k.AvroName
import com.sksamuel.avro4k.AvroNamespace
import io.github.classgraph.ClassGraph
import org.apache.avro.Schema
import org.apache.avro.specific.SpecificData

class RecordLookup(
    recordPackages: List<String>,
    private val classLoader: ClassLoader
) {
    private val specificData = SpecificData(classLoader)
    private val recordNameToType: Map<String, Class<*>> by lazy {
        if (recordPackages.isNotEmpty()) {
            val annotationNames = arrayOf(
                AvroName::class,
                AvroNamespace::class,
                AvroAlias::class,
                AvroAliases::class
            ).map { it.java.name }
            val avroTypes = ClassGraph().enableClassInfo().enableAnnotationInfo().ignoreClassVisibility()
                .acceptClasses(*annotationNames.toTypedArray())
                .acceptPackages(*recordPackages.toTypedArray())
                .addClassLoader(classLoader)
                .scan().use { scanResult ->
                    annotationNames
                        .flatMap { scanResult.getClassesWithAnnotation(it) }
                        .map { it.loadClass() }
                        .toSet()
                }
            avroTypes.flatMap { type ->
                type.avroRecordNames.map { Pair(it, type) }
            }.toMap()
        } else {
            emptyMap()
        }
    }

    fun lookupType(msgSchema: Schema): Class<*>? {
        //Use the context classloader to not run into https://github.com/spring-projects/spring-boot/issues/14622 when
        //using Spring boot devtools
        var objectClass: Class<*>? = specificData.getClass(msgSchema)
        if (objectClass == null) {
            objectClass = recordNameToType[msgSchema.fullName]
        }
        return objectClass
    }
}
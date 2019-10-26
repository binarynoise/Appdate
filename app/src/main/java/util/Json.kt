package de.binarynoise.appdate

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.Semver.SemverType.LOOSE

val mapper: ObjectMapper = ObjectMapper().registerKotlinModule().setSerializationInclusion(JsonInclude.Include.NON_NULL)
	.setSerializationInclusion(JsonInclude.Include.NON_EMPTY).registerModule(SimpleModule().apply {
		addSerializer(Semver::class.java, SemverSerializer())
		addDeserializer(Semver::class.java, SemverDeserializer())
	}).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

fun Any?.toJson(): String = mapper.writeValueAsString(this)

fun Any?.toPrettyJson(): String = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

inline fun <reified T> fromJson(json: String?): T? = mapper.readValue(json ?: "null")

private class SemverSerializer : JsonSerializer<Semver>() {
	override fun serialize(value: Semver, gen: JsonGenerator, serializers: SerializerProvider) {
		gen.writeString(value.toString())
	}
}

private class SemverDeserializer : JsonDeserializer<Semver>() {
	override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Semver {
		return Semver(p.codec.readTree<TextNode>(p).textValue(), LOOSE)
	}
}

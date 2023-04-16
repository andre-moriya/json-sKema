package com.github.erosb.jsonsKema

abstract class ValidationFailure(
    open val message: String,
    open val schema: Schema,
    open val instance: IJsonValue,
    val keyword: Keyword? = null,
    open val causes: Set<ValidationFailure> = setOf()
) {

    private fun appendTo(sb: StringBuilder, linePrefix: String) {
        sb.append("${linePrefix}${instance.location.getLocation()}: $message\n" +
                "${linePrefix}Keyword: ${keyword?.value}\n"  +
                "${linePrefix}Schema pointer: ${schema.location.pointer}\n" +
                "${linePrefix}Schema location: Line ${schema.location.lineNumber}, character ${schema.location.position}\n" +
                "${linePrefix}Instance pointer: ${instance.location.pointer}\n" +
                "${linePrefix}Instance location: ${instance.location.getLocation()}")
        if (causes.isNotEmpty()) {
            sb.append("\nCauses:")
            for (cause in causes) {
                sb.append("\n\n")
                cause.appendTo(sb, linePrefix + "\t")
            }
        }
    }
    final override fun toString(): String {
        val sb = StringBuilder()
        appendTo(sb, "")
        return sb.toString()
    }

    fun toJSON(): JsonObject {
        val instanceRef = JsonString(instance.location.pointer.toString())
        val json = mutableMapOf<JsonString, JsonValue>(
            JsonString("instanceRef") to instanceRef,
            JsonString("schemaRef") to JsonString(schema.location.pointer.toString()),
            JsonString("message") to JsonString(message)
        )
        keyword?.let { json[JsonString("keyword")] = JsonString(it.value) }
        if (causes.isNotEmpty()) {
            json[JsonString("causes")] = JsonArray(causes.map { failure -> failure.toJSON() })
        }
        return JsonObject(
            properties = json.toMap()
        )
    }

    internal open fun join(parent: Schema, instance: IJsonValue, other: ValidationFailure): ValidationFailure {
        return AggregatingValidationFailure(parent, instance, setOf(this, other))
    }
}

data class MinimumValidationFailure(
    override val schema: MinimumSchema,
    override val instance: IJsonNumber
) : ValidationFailure("${instance.value} is lower than minimum ${schema.minimum}", schema, instance, Keyword.MINIMUM)

data class MaximumValidationFailure(
    override val schema: MaximumSchema,
    override val instance: IJsonNumber
) : ValidationFailure("${instance.value} is greater than maximum ${schema.maximum}", schema, instance, Keyword.MAXIMUM)

data class ExclusiveMinimumValidationFailure(
    override val schema: ExclusiveMinimumSchema,
    override val instance: IJsonNumber
) : ValidationFailure("${instance.value} is lower than or equal to minimum ${schema.minimum}", schema, instance, Keyword.EXCLUSIVE_MINIMUM)

data class ExclusiveMaximumValidationFailure(
    override val schema: ExclusiveMaximumSchema,
    override val instance: IJsonNumber
) : ValidationFailure("${instance.value} is greater than or equal to maximum ${schema.maximum}", schema, instance, Keyword.EXCLUSIVE_MAXIMUM)

data class MultipleOfValidationFailure(
    override val schema: MultipleOfSchema,
    override val instance: IJsonNumber
) : ValidationFailure("${instance.value} is not a multiple of ${schema.denominator}", schema, instance, Keyword.MULTIPLE_OF)

data class TypeValidationFailure(
    val actualInstanceType: String,
    override val schema: TypeSchema,
    override val instance: IJsonValue
) : ValidationFailure("expected type: ${schema.type.value}, actual: $actualInstanceType", schema, instance, Keyword.TYPE)

data class MultiTypeValidationFailure(
    val actualInstanceType: String,
    override val schema: MultiTypeSchema,
    override val instance: IJsonValue
) : ValidationFailure(
    "expected type: one of ${schema.types.elements.joinToString { ", " }}, actual: $actualInstanceType",
    schema,
    instance,
    Keyword.TYPE
)

data class FalseValidationFailure(
    override val schema: FalseSchema,
    override val instance: IJsonValue
) : ValidationFailure("false schema always fails", schema, instance, Keyword.FALSE)

data class RequiredValidationFailure(
    val missingProperties: List<String>,
    override val schema: RequiredSchema,
    override val instance: IJsonObj
) : ValidationFailure(
    "required properties are missing: " + missingProperties.joinToString(),
    schema,
    instance,
    Keyword.REQUIRED
)

data class NotValidationFailure(
    override val schema: Schema,
    override val instance: IJsonValue
) : ValidationFailure("negated subschema did not fail", schema, instance, Keyword.NOT)

data class MaxLengthValidationFailure(
    override val schema: MaxLengthSchema,
    override val instance: IJsonString
) : ValidationFailure(
    "actual string length ${instance.value.length} exceeds maxLength ${schema.maxLength}",
    schema,
    instance,
    Keyword.MAX_LENGTH
)

data class MinLengthValidationFailure(
    override val schema: MinLengthSchema,
    override val instance: IJsonString
) : ValidationFailure(
    "actual string length ${instance.value.length} is lower than minLength ${schema.minLength}",
    schema,
    instance,
    Keyword.MIN_LENGTH
)

data class ConstValidationFailure(
    override val schema: ConstSchema,
    override val instance: IJsonValue
) : ValidationFailure(
    "actual instance is not the same as expected constant value",
    schema,
    instance,
    Keyword.CONST
)

data class UniqueItemsValidationFailure(
    val arrayPositions: List<Int>,
    override val schema: UniqueItemsSchema,
    override val instance: IJsonArray<*>
) : ValidationFailure("the same array element occurs at positions " + arrayPositions.joinToString(", "), schema, instance, Keyword.UNIQUE_ITEMS)

internal class AggregatingValidationFailure(
    schema: Schema,
    instance: IJsonValue,
    causes: Set<ValidationFailure>
) : ValidationFailure("multiple validation failures", schema, instance, null, causes) {

    private var _causes = causes.toMutableSet()
    override val causes: Set<ValidationFailure>
        get() {
            return _causes
        }

    override fun join(parent: Schema, instance: IJsonValue, other: ValidationFailure): ValidationFailure {
        if (parent != schema) {
            TODO("something went wrong")
        }
        if (instance != this.instance) {
            TODO("something went wrong: $instance vs ${this.instance}")
        }
        _causes.add(other)
        return this
    }
}

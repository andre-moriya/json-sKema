package com.github.erosb.jsonschema

abstract class Schema(open val location: SourceLocation) {
    abstract fun <P> accept(visitor: Visitor<P>): P?
    open fun subschemas(): Collection<Schema> = emptyList()
}

data class CompositeSchema(
        val subschemas: Set<Schema>,
        override val location: SourceLocation,
        val id: IJsonString? = null,
        val title: IJsonString? = null,
        val description: IJsonString? = null,
        val deprecated: IJsonBoolean? = null,
        val readOnly: IJsonBoolean? = null,
        val writeOnly: IJsonBoolean? = null,
        val default: IJsonValue? = null,
        val dynamicRef: IJsonString? = null,
        val dynamicAnchor: IJsonString? = null,
        val propertySchemas: Map<String, Schema> = emptyMap()): Schema(location) {
    override fun <P> accept(visitor: Visitor<P>) = visitor.internallyVisitCompositeSchema(this)
    override fun subschemas() = subschemas
}

data class AllOfSchema(
        val subschemas: List<Schema>,
        override val location: SourceLocation
): Schema(location) {
    override fun <P> accept(visitor: Visitor<P>) = visitor.visitAllOfSchema(this)
    override fun subschemas(): Collection<Schema> = subschemas
}

data class ReferenceSchema(var referredSchema: Schema?, val ref: String, override val location: SourceLocation): Schema(location) {
    override fun <P> accept(visitor: Visitor<P>) = visitor.visitReferenceSchema(this)
    override fun subschemas() = referredSchema?.let { listOf(it) } ?: emptyList()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReferenceSchema) return false

        if (location != other.location) return false

        return referredSchema === other.referredSchema
    }

    override fun hashCode(): Int {
        return location.hashCode()
    }

    override fun toString(): String {
        return "{\"\$ref\": \"${ref}\", \"resolved\":\"${referredSchema !== null}\"}"
    }
}

data class DynamicRefSchema(var referredSchema: Schema?, val dynamicRef: String, override val location: SourceLocation): Schema(location) {
    override fun <P> accept(visitor: Visitor<P>): P? = visitor.visitDynamicRefSchema(this)
    override fun subschemas() = referredSchema?.let {listOf(it)} ?: emptyList()
}

data class TrueSchema(override val location: SourceLocation): Schema(location) {
    override fun <P> accept(visitor: Visitor<P>) = visitor.visitTrueSchema(this)
}

data class FalseSchema(override val location: SourceLocation): Schema(location) {
    override fun <P> accept(visitor: Visitor<P>) = visitor.visitFalseSchema(this)
}

data class MinLengthSchema(val minLength: Int, override val location: SourceLocation): Schema(location){
    override fun <P> accept(visitor: Visitor<P>) = visitor.visitMinLengthSchema(this)
}

data class MaxLengthSchema(val maxLength: Int, override val location: SourceLocation): Schema(location) {
    override fun <P> accept(visitor: Visitor<P>) = visitor.visitMaxLengthSchema(this)
}

data class AdditionalPropertiesSchema(val subschema: Schema, override val location: SourceLocation): Schema(location) {
    override fun <P> accept(visitor: Visitor<P>) = visitor.visitAdditionalPropertiesSchema(this)
    override fun subschemas() = listOf(subschema)
}

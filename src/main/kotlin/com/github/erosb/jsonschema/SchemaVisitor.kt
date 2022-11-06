package com.github.erosb.jsonschema

import java.lang.RuntimeException

abstract class SchemaVisitor<P> {

    private val anchors: MutableMap<String, CompositeSchema> = mutableMapOf()

    private val visitedReferences: MutableList<ReferenceSchema> = mutableListOf()

    internal fun internallyVisitCompositeSchema(schema: CompositeSchema): P? {
        val wasDynamicAnchorChange: Boolean = schema.dynamicAnchor?.let {
            if (!anchors.containsKey(it)) {
                anchors[it] = schema
                true
            }
            false
        } ?: false
        var product: P?
        if (schema.dynamicRef != null) {
            val referred = anchors[schema.dynamicRef]
            if (referred === null) {
                TODO("not implemented (no matching dynamicAnchor for dynamicRef ${schema.dynamicRef}")
            }
            val merged = CompositeSchema(
                location = schema.location,
                default = schema.default ?: referred.default,
                subschemas = schema.subschemas ?: referred.subschemas,
                title = schema.title ?: referred.title
            )
            product = visitCompositeSchema(merged)
        } else {
            product = visitCompositeSchema(schema)
        }
        if (wasDynamicAnchorChange) {
            anchors.remove(schema.dynamicAnchor)
        }
        return product
    }

    internal fun visitReferenceSchemaUnlessAlreadyVisited(schema: ReferenceSchema): P? {
        if (visitedReferences.contains(schema)) {
            return null
        }
        visitedReferences.add(schema)
        try {
            return visitReferenceSchema(schema)
        } finally {
            visitedReferences.remove(schema)
        }
    }

    open fun visitCompositeSchema(schema: CompositeSchema): P? {
        val subschemaProduct = visitChildren(schema)
        val propSchemaProduct: P? = if (schema.propertySchemas.isEmpty()) {
            null
        } else schema.propertySchemas
            .map { visitPropertySchema(it.key, it.value) }
            .reduce { a, b -> accumulate(schema, a, b) }
        return accumulate(schema, subschemaProduct, propSchemaProduct)
    }

    open fun visitTrueSchema(schema: TrueSchema): P? = visitChildren(schema)
    open fun visitFalseSchema(schema: FalseSchema): P? = visitChildren(schema)
    open fun visitMinLengthSchema(schema: MinLengthSchema): P? = visitChildren(schema)
    open fun visitMaxLengthSchema(schema: MaxLengthSchema): P? = visitChildren(schema)
    open fun visitAllOfSchema(schema: AllOfSchema): P? = visitChildren(schema)
    open fun visitReferenceSchema(schema: ReferenceSchema): P? = visitChildren(schema)
    open fun visitDynamicRefSchema(schema: DynamicRefSchema): P? = visitChildren(schema)
    open fun visitAdditionalPropertiesSchema(schema: AdditionalPropertiesSchema): P? = visitChildren(schema)
    open fun visitConstSchema(schema: ConstSchema): P? = visitChildren(schema)
    open fun visitTypeSchema(schema: TypeSchema): P? = visitChildren(schema)
    open fun visitMultiTypeSchema(schema: MultiTypeSchema): P? = visitChildren(schema)
    open fun visitPropertySchema(property: String, schema: Schema): P? = visitChildren(schema)
    open fun visitNotSchema(schema: NotSchema): P? = visitChildren(schema)
    open fun visitRequiredSchema(schema: RequiredSchema): P? = visitChildren(schema)
    open fun visitMaximumSchema(schema: MaximumSchema): P? = visitChildren(schema)
    open fun visitMinimumSchema(schema: MinimumSchema): P? = visitChildren(schema)
    open fun visitMultipleOfSchema(schema: MultipleOfSchema): P? = visitChildren(schema)
    open fun visitUniqueItemsSchema(schema: UniqueItemsSchema): P? = visitChildren(schema)
    open fun visitItemsSchema(schema: ItemsSchema): P? = visitChildren(schema)
    open fun visitContainsSchema(schema: ContainsSchema): P? = visitChildren(schema)

    open fun identity(): P? = null
    open fun accumulate(parent: Schema, previous: P?, current: P?): P? = current ?: previous
    open fun visitChildren(parent: Schema): P? {
        var product: P? = identity()
        for (subschema in parent.subschemas()) {
            val current = subschema.accept(this)
            product = accumulate(parent, product, current)
        }
        return product
    }
}

internal class SchemaNotFoundException(expectedKey: String, actualKey: String) :
    RuntimeException("expected key: $expectedKey, but found: $actualKey")

internal class TraversingSchemaVisitor<P>(vararg keys: String) : SchemaVisitor<P>() {

    private val remainingKeys = keys.asList().toMutableList()

    private fun consume(schema: Schema, key: String, cb: () -> P?): P? {
        if (remainingKeys[0] == key) {
            remainingKeys.removeAt(0)
            if (remainingKeys.isEmpty()) {
                return schema as P
            }
            return cb()
        }
        throw SchemaNotFoundException(key, remainingKeys[0])
    }

    override fun visitCompositeSchema(schema: CompositeSchema): P? {
        if (remainingKeys.isEmpty()) {
            return schema as P
        }
        if (remainingKeys[0] == "title") {
            remainingKeys.removeAt(0)
            if (remainingKeys.isEmpty()) {
                return schema.title!!.value as P
            }
            throw SchemaNotFoundException("cannot traverse keys of string 'title'", "")
        } else if (remainingKeys[0] == "properties") {
            remainingKeys.removeAt(0)
            val propName = remainingKeys.removeAt(0)
            return schema.propertySchemas[propName]?.accept(this)
        }
        return super.visitCompositeSchema(schema)
    }

    override fun visitDynamicRefSchema(schema: DynamicRefSchema): P? = consume(schema, "\$dynamicRef") {
        schema.referredSchema?.accept(this)
    }

    override fun visitAdditionalPropertiesSchema(schema: AdditionalPropertiesSchema): P? =
        consume(schema, "additionalProperties") { super.visitAdditionalPropertiesSchema(schema) }

    override fun visitReferenceSchema(schema: ReferenceSchema): P? {
        if (remainingKeys[0] == "\$ref") {
            remainingKeys.removeAt(0)
            if (remainingKeys.isEmpty()) {
                return schema.referredSchema as P
            }
            return schema.referredSchema!!.accept(this)
        }
        throw SchemaNotFoundException("\$ref", remainingKeys[0])
    }
}

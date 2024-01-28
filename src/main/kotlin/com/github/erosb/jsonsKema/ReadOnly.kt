package com.github.erosb.jsonsKema

data class ReadOnlyValidationFailure(
    override val schema: Schema,
    override val instance: IJsonValue,
) : ValidationFailure(
    message = "read-only property \"${instance.location.pointer.segments.last()}\" should not be present in write context",
    schema = schema,
    instance = instance,
    keyword = Keyword.READ_ONLY,
    causes = setOf()
)

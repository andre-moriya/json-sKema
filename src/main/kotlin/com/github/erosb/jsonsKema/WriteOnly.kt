package com.github.erosb.jsonsKema

data class WriteOnlyValidationFailure(
    override val schema: Schema,
    override val instance: IJsonValue,
) : ValidationFailure(
    message = "write-only property \"${instance.location.pointer.segments.last()}\" should not be present in read context",
    schema = schema,
    instance = instance,
    keyword = Keyword.WRITE_ONLY,
    causes = setOf()
)

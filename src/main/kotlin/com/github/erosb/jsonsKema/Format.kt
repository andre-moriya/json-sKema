package com.github.erosb.jsonsKema

import org.apache.commons.validator.routines.EmailValidator
import org.apache.commons.validator.routines.InetAddressValidator
import java.lang.IllegalArgumentException
import java.net.URI
import java.net.URISyntaxException
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField
import java.util.UUID
import java.util.regex.Pattern

typealias FormatValidator = (instance: IJsonValue, schema: FormatSchema) -> ValidationFailure?

internal val dateFormatValidator: FormatValidator = { inst, schema -> inst.maybeString { str ->
    try {
        DateTimeFormatter.ISO_LOCAL_DATE.parse(str.value)
        null
    } catch (e: DateTimeParseException) {
        FormatValidationFailure(schema, str)
    }
}}

private val DATE_TIME_FORMATTER: DateTimeFormatter = run {
    val secondsFractionFormatter = DateTimeFormatterBuilder()
        .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
        .toFormatter()
    DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
        .appendOptional(secondsFractionFormatter)
        .appendPattern("XXX")
        .toFormatter()
        .withResolverStyle(ResolverStyle.STRICT)
}

private fun validateDateTime(str: IJsonString, schema: FormatSchema): FormatValidationFailure? {
    try {
        DATE_TIME_FORMATTER.parse(str.value.uppercase())
        ZonedDateTime.parse(str.value)
    } catch (e: DateTimeParseException) {
        if ((e.message?.indexOf("Invalid value for SecondOfMinute") ?: -1) > -1) {
            // handle leap second
            if (str.value.indexOf("23:59:60") > -1) {
                val sanitized = JsonString(str.value.replace("23:59:60", "23:59:59"), str.location)
                return validateDateTime(sanitized, schema)
            }
        }
        return FormatValidationFailure(schema, str)
    }
    return null
}

internal val dateTimeFormatValidator: FormatValidator = {inst, schema -> inst.maybeString { str ->
    validateDateTime(str, schema)
}}

internal val uriFormatValidator: FormatValidator = {inst, schema -> inst.maybeString { str ->
    try {
        if (URI(str.value).scheme == null) {
            FormatValidationFailure(schema, str)
        } else {
            null
        }
    } catch (e: URISyntaxException) {
        FormatValidationFailure(schema, str)
    }
}}

internal val emailFormatValidator: FormatValidator = {inst, schema -> inst.maybeString { str ->
    if (EmailValidator.getInstance(false, true).isValid(str.value)) {
        null
    } else {
        FormatValidationFailure(schema, str)
    }
}}

internal val ipv4FormatValidator: FormatValidator = {inst, schema -> inst.maybeString { str ->
    if (InetAddressValidator.getInstance().isValidInet4Address(str.value)) {
        null
    } else {
        FormatValidationFailure(schema, str)
    }
}}

private val allowedIpv6Chars = setOf('.', ':') + ('0'..'9') + ('a'..'f') + ('A'..'F')

internal val ipv6FormatValidator: FormatValidator = {inst, schema -> inst.maybeString { str ->
    if (InetAddressValidator.getInstance().isValidInet6Address(str.value)
        && str.value.toCharArray().all { it in allowedIpv6Chars }) {
        null
    } else {
        FormatValidationFailure(schema, str)
    }
}}

internal val timeFormatValidator: FormatValidator = {inst, schema -> inst.maybeString { str ->
    try {
        DateTimeFormatter.ISO_OFFSET_TIME.parse(str.value)
        null
    } catch (e: DateTimeParseException) {
        FormatValidationFailure(schema, str)
    }
}}

internal val uuidFormatValidator: FormatValidator = {inst, schema -> inst.maybeString { str ->
    if (str.value.length == 36)
    try {
        UUID.fromString(str.value)
        null
    } catch (e: IllegalArgumentException) {
        FormatValidationFailure(schema, str)
    } else {
        FormatValidationFailure(schema, str)
    }
}}

internal val durationFormatValidator: FormatValidator = {inst, schema -> inst.maybeString { str ->
    val regex = Pattern.compile("^P(?=\\d|T\\d)(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)([DW]))?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+(?:\\.\\d+)?)S)?)?$")
    if (!regex.matcher(str.value).matches()) {
        return@maybeString FormatValidationFailure(schema, str)
    }
    return@maybeString null
}}

data class FormatSchema(
    val format: String,
    override val location: SourceLocation
) : Schema(location) {
    override fun <P> accept(visitor: SchemaVisitor<P>): P? = visitor.visitFormatSchema(this)
}

internal val formatLoader: KeywordLoader = { ctx ->
    FormatSchema(ctx.keywordValue.requireString().value, ctx.location)
}
data class FormatValidationFailure(
    override val schema: FormatSchema,
    override val instance: IJsonValue
) : ValidationFailure(
    message = "instance does not match format '${schema.format}'",
    keyword = Keyword.FORMAT,
    causes = setOf(),
    schema = schema,
    instance = instance
)

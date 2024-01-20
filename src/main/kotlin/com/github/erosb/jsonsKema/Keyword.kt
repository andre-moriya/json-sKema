package com.github.erosb.jsonsKema

enum class SpecificationVersion {
    DRAFT_2020_12,
}

enum class Keyword(
    val value: String,
    internal val hasMapLikeSemantics: Boolean = false,
    internal val specificationVersion: SpecificationVersion = SpecificationVersion.DRAFT_2020_12,
) {
    ID("\$id"),
    ANCHOR("\$anchor"),
    DYNAMIC_REF("\$dynamicRef"),
    DYNAMIC_ANCHOR("\$dynamicAnchor"),
    REF("\$ref"),
    DEFS("\$defs", true),
    SCHEMA("\$schema"),
    VOCABULARY("\$vocabulary"),
    MIN_LENGTH("minLength"),
    MAX_LENGTH("maxLength"),
    ALL_OF("allOf", true),
    ANY_OF("anyOf", true),
    ONE_OF("oneOf", true),
    ADDITIONAL_PROPERTIES("additionalProperties"),
    PROPERTIES("properties", true),
    PATTERN_PROPERTIES("patternProperties", true),
    TITLE("title"),
    DESCRIPTION("description"),
    READ_ONLY("readOnly"),
    WRITE_ONLY("writeOnly"),
    DEPRECATED("deprecated"),
    DEFAULT("default"),
    ENUM("enum"),
    CONST("const"),
    FALSE("false"),
    TRUE("true"),
    TYPE("type"),
    NOT("not"),
    REQUIRED("required"),
    MAXIMUM("maximum"),
    MINIMUM("minimum"),
    EXCLUSIVE_MAXIMUM("exclusiveMaximum"),
    EXCLUSIVE_MINIMUM("exclusiveMinimum"),
    MULTIPLE_OF("multipleOf"),
    UNIQUE_ITEMS("uniqueItems"),
    ITEMS("items"),
    PREFIX_ITEMS("prefixItems"),
    CONTAINS("contains"),
    MIN_CONTAINS("minContains"),
    MAX_CONTAINS("maxContains"),
    IF("if"),
    THEN("then"),
    ELSE("else"),
    DEPENDENT_SCHEMAS("dependentSchemas"),
    DEPENDENT_REQUIRED("dependentRequired"),
    UNEVALUATED_ITEMS("unevaluatedItems"),
    UNEVALUATED_PROPERTIES("unevaluatedProperties"),
    MIN_ITEMS("minItems"),
    MAX_ITEMS("maxItems"),
    MIN_PROPERTIES("minProperties"),
    MAX_PROPERTIES("maxProperties"),
    PROPERTY_NAMES("propertyNames"),
    PATTERN("pattern"),
    FORMAT("format"),
}

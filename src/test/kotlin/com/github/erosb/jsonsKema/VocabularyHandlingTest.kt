package com.github.erosb.jsonsKema

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VocabularyHandlingTest {

    @Test
    fun `format validation is disabled by default`() {
        val schema = SchemaLoader(JsonParser("""
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema"
                "format": "email"
            }
        """)()
        )() as CompositeSchema

        val actual = Validator.forSchema(schema).validate(JsonParser("""
           "not-an-email" 
        """)())

        assertNull(actual)
    }

    @Test
    fun `format validation is enabled by meta-schema`() {

    }

    @Test
    fun `format validation is enabled if meta-schema is missing`() {

    }

    @Test
    fun `vocab loading only true valued vocabs are loaded`() {

    }

    @Test
    fun `vocabularies is not an object`() {

    }
}

package com.github.erosb.jsonsKema

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReadWriteContextTest {

    @Test
    fun `validation in write context fails on readonly prop`() {
        val schema = SchemaLoader(JsonParser("""
            {
                "properties": {
                    "id": {
                        "type": "integer",
                        "readOnly": true
                    }
                }
            }
        """)())()

        val validator = Validator.create(schema, ValidatorConfig(
            readWriteContext = ReadWriteContext.WRITE
        ))

        val actual = validator.validate(JsonParser("""
           {
            "id": 3
           } 
        """)())

        println(actual?.toJSON())
        assertEquals("read-only property \"id\" should not be present in write context", actual!!.message)
    }

    
}

package com.asadraza.streamflix.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates the exported schema for the current version against the Room entities.
 * Add a case per Migration here whenever DatabaseConfig.VERSION is bumped.
 */
@RunWith(AndroidJUnit4::class)
class SchemaTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StreamFlixDatabase::class.java
    )

    @Test
    fun `current schema matches the exported json`() {
        helper.createDatabase("schema-test", DatabaseConfig.VERSION).close()
        helper.runMigrationsAndValidate("schema-test", DatabaseConfig.VERSION, true)
    }
}

package org.jetbrains.exposed.sql.tests.shared

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.tests.DatabaseTestsBase
import org.jetbrains.exposed.sql.tests.TestDB
import org.jetbrains.exposed.sql.vendors.ColumnMetadata
import org.jetbrains.exposed.sql.vendors.H2Dialect
import org.junit.Test
import java.sql.Types

class ConnectionTests : DatabaseTestsBase() {

    object People : LongIdTable() {
        val firstName = varchar("firstname", 80).nullable()
        val lastName = varchar("lastname", 42).default("Doe")
        val age = integer("age").default(18)
    }

    @Test
    fun testGettingColumnMetadata() {
        withTables(excludeSettings = TestDB.ALL - TestDB.H2_V2, People) {
            val columnMetadata = connection.metadata {
                requireNotNull(columns(People)[People])
            }.toSet()
            val expected = when ((db.dialect as H2Dialect).isSecondVersion) {
                false -> setOf(
                    ColumnMetadata("ID", Types.BIGINT, false, 19, null, true, null),
                    ColumnMetadata("FIRSTNAME", Types.VARCHAR, true, 80, null, false, null),
                    ColumnMetadata("LASTNAME", Types.VARCHAR, false, 42, null, false, "Doe"),
                    ColumnMetadata("AGE", Types.INTEGER, false, 10, null, false, "18"),
                )
                true -> setOf(
                    ColumnMetadata("ID", Types.BIGINT, false, 64, null, true, null),
                    ColumnMetadata("FIRSTNAME", Types.VARCHAR, true, 80, null, false, null),
                    ColumnMetadata("LASTNAME", Types.VARCHAR, false, 42, null, false, "Doe"),
                    ColumnMetadata("AGE", Types.INTEGER, false, 32, null, false, "18"),
                )
            }
            assertEquals(expected, columnMetadata)
        }
    }

    // GitHub issue #838
    @Test
    fun testTableConstraints() {
        val parent = object : LongIdTable("parent") {
            val scale = integer("scale").uniqueIndex()
        }
        val child = object : LongIdTable("child") {
            val scale = reference("scale", parent.scale)
        }
        withTables(listOf(TestDB.MYSQL_V5), child, parent) {
            val constraints = connection.metadata {
                tableConstraints(listOf(child))
            }
            assertEquals(2, constraints.keys.size)
        }
    }
}

/*
 *  This file is part of Track & Graph
 */
package com.samco.trackandgraph.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Add segment_colors column to pie_charts_table2
val MIGRATION_58_59 = object : Migration(58, 59) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Prüfe, ob die Spalte bereits existiert
        val cursor = database.query("PRAGMA table_info(pie_charts_table2)")
        var hasColumn = false
        while (cursor.moveToNext()) {
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex >= 0 && cursor.getString(nameIndex) == "segment_colors") {
                hasColumn = true
                break
            }
        }
        cursor.close()
        if (!hasColumn) {
            database.execSQL(
                "ALTER TABLE pie_charts_table2 ADD COLUMN segment_colors TEXT"
            )
        }
    }
}

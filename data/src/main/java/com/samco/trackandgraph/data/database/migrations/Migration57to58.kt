/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the new column color_index_start (default 0) to pie_charts_table2.
 * This migration is idempotent: it checks if the column already exists before altering.
 */
val MIGRATION_57_58 = object : Migration(57, 58) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Prüfe, ob die Spalte bereits existiert
        val cursor = database.query("PRAGMA table_info(pie_charts_table2)")
        var hasColorIndexStart = false
        while (cursor.moveToNext()) {
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex >= 0 && cursor.getString(nameIndex) == "color_index_start") {
                hasColorIndexStart = true
                break
            }
        }
        cursor.close()
        if (!hasColorIndexStart) {
            database.execSQL(
                "ALTER TABLE pie_charts_table2 ADD COLUMN color_index_start INTEGER NOT NULL DEFAULT 0"
            )
        }
    }
}

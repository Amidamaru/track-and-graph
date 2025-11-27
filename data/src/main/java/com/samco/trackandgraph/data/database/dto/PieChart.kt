/*
* This file is part of Track & Graph
* 
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.data.database.dto

import com.samco.trackandgraph.data.database.entity.PieChart
import org.threeten.bp.temporal.TemporalAmount

data class PieChart(
    val id: Long,
    val graphStatId: Long,
    val featureId: Long,
    val sampleSize: TemporalAmount?,
    val endDate: GraphEndDate,
    val sumByCount: Boolean,
    val colorIndexStart: Int = 0,
    val segmentColors: Map<String, Int>? = null
) {
    internal fun toEntity() = com.samco.trackandgraph.data.database.entity.PieChart(
        id = id,
        graphStatId = graphStatId,
        featureId = featureId,
        sampleSize = sampleSize,
        endDate = endDate,
        sumByCount = sumByCount,
        colorIndexStart = colorIndexStart,
        segmentColorsJson = segmentColors?.let { encodeSegmentColors(it) }
    )

    companion object {
        fun encodeSegmentColors(map: Map<String, Int>): String = map.entries.joinToString(separator = "\n") { (k, v) -> "$k=$v" }
        fun decodeSegmentColors(value: String): Map<String, Int> = value
            .lineSequence()
            .mapNotNull {
                val idx = it.indexOf('=')
                if (idx <= 0) null else it.substring(0, idx) to it.substring(idx + 1).toIntOrNull()
            }
            .filter { it.second != null }
            .associate { it.first to it.second!! }
    }
}

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

package com.samco.trackandgraph.data.dependencyanalyser.queryresponse

import androidx.room.ColumnInfo

/**
 * Represents a dependency of a function feature on another feature.
 */
data class FunctionDependency(
    @ColumnInfo(name = "function_feature_id")
    val functionFeatureId: Long,
    @ColumnInfo(name = "input_feature_id")
    val inputFeatureId: Long
)

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
package com.samco.trackandgraph.data.database

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.samco.trackandgraph.data.database.entity.AverageTimeBetweenStat
import com.samco.trackandgraph.data.database.entity.BarChart
import com.samco.trackandgraph.data.database.entity.DataPoint
import com.samco.trackandgraph.data.database.entity.Feature
import com.samco.trackandgraph.data.database.entity.FeatureTimer
import com.samco.trackandgraph.data.database.entity.Function
import com.samco.trackandgraph.data.database.entity.FunctionInputFeature
import com.samco.trackandgraph.data.database.entity.GlobalNote
import com.samco.trackandgraph.data.database.entity.GraphOrStat
import com.samco.trackandgraph.data.database.entity.Group
import com.samco.trackandgraph.data.database.entity.LastValueStat
import com.samco.trackandgraph.data.database.entity.LineGraph
import com.samco.trackandgraph.data.database.entity.LineGraphFeature
import com.samco.trackandgraph.data.database.entity.LuaGraph
import com.samco.trackandgraph.data.database.entity.LuaGraphFeature
import com.samco.trackandgraph.data.database.entity.PieChart
import com.samco.trackandgraph.data.database.entity.Reminder
import com.samco.trackandgraph.data.database.entity.TimeHistogram
import com.samco.trackandgraph.data.database.entity.Tracker
import com.samco.trackandgraph.data.database.entity.queryresponse.DisplayNote
import com.samco.trackandgraph.data.database.entity.queryresponse.DisplayTracker
import com.samco.trackandgraph.data.database.entity.queryresponse.FunctionWithFeature
import com.samco.trackandgraph.data.database.entity.queryresponse.LineGraphWithFeatures
import com.samco.trackandgraph.data.database.entity.queryresponse.LuaGraphWithFeatures
import com.samco.trackandgraph.data.database.entity.queryresponse.TrackerWithFeature
import com.samco.trackandgraph.data.dependencyanalyser.queryresponse.FunctionDependency
import com.samco.trackandgraph.data.dependencyanalyser.queryresponse.GraphDependency
import kotlinx.coroutines.flow.Flow

private const val getTrackersQuery = """
    SELECT 
        features_table.name as name,
        features_table.group_id as group_id,
        features_table.display_index as display_index,
        features_table.feature_description as feature_description,
        trackers_table.id as id,
        trackers_table.feature_id as feature_id,
        trackers_table.type as type,
        trackers_table.has_default_value as has_default_value,
        trackers_table.default_value as default_value,
        trackers_table.default_label as default_label,
        trackers_table.suggestion_type as suggestion_type,
        trackers_table.suggestion_order as suggestion_order,
        trackers_table.warning_threshold as warning_threshold,
        trackers_table.error_threshold as error_threshold,
        trackers_table.notification_title_template as notification_title_template,
        trackers_table.notification_body_template as notification_body_template
    FROM trackers_table
    LEFT JOIN features_table ON trackers_table.feature_id = features_table.id
            """

private const val getDisplayTrackersQuery = """ 
    SELECT
        features_table.name as name,
        features_table.group_id as group_id,
        features_table.display_index as display_index,
        features_table.feature_description as feature_description,
        trackers_table.id as id,
        trackers_table.feature_id as feature_id,
        trackers_table.type as type,
        trackers_table.has_default_value as has_default_value,
        trackers_table.default_value as default_value,
        trackers_table.default_label as default_label,
        trackers_table.warning_threshold as warning_threshold,
        trackers_table.error_threshold as error_threshold,
        trackers_table.notification_title_template as notification_title_template,
        trackers_table.notification_body_template as notification_body_template,
        last_epoch_milli,
        last_utc_offset_sec,
        start_instant 
        FROM (
            trackers_table
            LEFT JOIN features_table ON trackers_table.feature_id = features_table.id
            LEFT JOIN (
                SELECT feature_id, epoch_milli as last_epoch_milli, utc_offset_sec as last_utc_offset_sec
                FROM data_points_table as dpt
                INNER JOIN (
                    SELECT feature_id as fid, MAX(epoch_milli) as max_epoch_milli
                    FROM data_points_table 
                    GROUP BY feature_id
                ) as max_data ON max_data.fid = dpt.feature_id AND dpt.epoch_milli = max_data.max_epoch_milli
            ) as last_data ON last_data.feature_id = trackers_table.feature_id
            LEFT JOIN (
                SELECT * FROM feature_timers_table
            ) as timer_data ON timer_data.feature_id = trackers_table.feature_id
        )
    """

private const val getFunctionsQuery = """
    SELECT 
        functions_table.id as id,
        functions_table.feature_id as feature_id,
        functions_table.function_graph as function_graph,
        features_table.name as name,
        features_table.group_id as group_id,
        features_table.display_index as display_index,
        features_table.feature_description as feature_description
    FROM functions_table
    LEFT JOIN features_table ON functions_table.feature_id = features_table.id
            """

@Dao
internal interface TrackAndGraphDatabaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGroup(group: Group): Long

    @Query("DELETE FROM groups_table WHERE id = :id")
    fun deleteGroup(id: Long)

    @Update
    fun updateGroup(group: Group)

    @Update
    fun updateGroups(groups: List<Group>)

    @Query("""SELECT * FROM reminders_table ORDER BY display_index ASC, id DESC""")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("""SELECT * FROM reminders_table ORDER BY display_index ASC, id DESC""")
    fun getAllRemindersSync(): List<Reminder>

    @Query("""SELECT groups_table.* FROM groups_table ORDER BY display_index ASC, id DESC""")
    fun getAllGroups(): Flow<List<Group>>

    @Query("""SELECT groups_table.* FROM groups_table ORDER BY display_index ASC, id DESC""")
    fun getAllGroupsSync(): List<Group>

    @Query("""SELECT features_table.* FROM features_table ORDER BY display_index ASC, id DESC""")
    fun getAllFeaturesSync(): List<Feature>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReminder(reminder: Reminder)

    @Query("DELETE FROM reminders_table")
    fun deleteReminders()

    @Query("SELECT * FROM groups_table WHERE id = :id LIMIT 1")
    fun getGroupById(id: Long): Group

    @Update
    fun updateFeatures(features: List<Feature>)

    @Query("$getDisplayTrackersQuery WHERE group_id = :groupId ORDER BY features_table.display_index ASC, id DESC")
    fun getDisplayTrackersForGroupSync(groupId: Long): List<DisplayTracker>

    @Query("SELECT features_table.* FROM features_table WHERE group_id = :groupId ORDER BY features_table.display_index ASC")
    fun getFeaturesForGroupSync(groupId: Long): List<Feature>

    @Query(
        """
            SELECT
                features_table.name as name,
                features_table.group_id as group_id,
                features_table.display_index as display_index,
                features_table.feature_description as feature_description,
                trackers_table.id as id,
                trackers_table.feature_id as feature_id,
                trackers_table.type as type,
                trackers_table.has_default_value as has_default_value,
                trackers_table.default_value as default_value,
                trackers_table.default_label as default_label,
                trackers_table.suggestion_order as suggestion_order,
                trackers_table.suggestion_type as suggestion_type,
                trackers_table.warning_threshold AS warning_threshold,
                trackers_table.error_threshold AS error_threshold
            FROM trackers_table
            LEFT JOIN features_table ON features_table.id = trackers_table.feature_id
            WHERE features_table.group_id = :groupId ORDER BY features_table.display_index ASC
        """
    )
    fun getTrackersForGroupSync(groupId: Long): List<TrackerWithFeature>

    @Query("SELECT * FROM features_table WHERE id = :featureId LIMIT 1")
    fun getFeatureById(featureId: Long): Feature?

    @Query("""SELECT * from features_table WHERE id IN (:featureIds) ORDER BY display_index ASC, id DESC""")
    fun getFeaturesByIdsSync(featureIds: List<Long>): List<Feature>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFeature(feature: Feature): Long

    @Update
    fun updateFeature(feature: Feature)

    @Delete
    fun deleteDataPoint(dataPoint: DataPoint)

    @Query("DELETE FROM data_points_table WHERE feature_id = :featureId AND value = :index")
    fun deleteAllDataPointsForDiscreteValue(featureId: Long, index: Double)

    @Query("DELETE FROM graphs_and_stats_table2 WHERE id = :id")
    fun deleteGraphOrStat(id: Long)

    @Delete
    fun deleteGraphOrStat(graphOrStat: GraphOrStat)

    @Query("DELETE FROM features_table WHERE id = :id")
    fun deleteFeature(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDataPoint(dataPoint: DataPoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDataPoints(dataPoint: List<DataPoint>)

    @Update
    fun updateDataPoints(dataPoint: List<DataPoint>)

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY epoch_milli DESC")
    fun getDataPointsForFeatureSync(featureId: Long): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId AND epoch_milli = :epochMilli")
    fun getDataPointByTimestampAndFeatureSync(featureId: Long, epochMilli: Long): DataPoint?

    @Query("SELECT COUNT(*) FROM data_points_table WHERE feature_id = :featureId")
    fun getDataPointCount(featureId: Long): Int

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY epoch_milli DESC LIMIT :limit OFFSET :offset")
    fun getDataPoints(featureId: Long, limit: Int, offset: Int): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY epoch_milli DESC")
    fun getDataPointsCursor(featureId: Long): Cursor

    @Query("SELECT * FROM graphs_and_stats_table2 WHERE id = :graphStatId LIMIT 1")
    fun getGraphStatById(graphStatId: Long): GraphOrStat

    @Query("SELECT * FROM graphs_and_stats_table2 WHERE id = :graphStatId LIMIT 1")
    fun tryGetGraphStatById(graphStatId: Long): GraphOrStat?

    @Query("SELECT * FROM line_graphs_table3 WHERE graph_stat_id = :graphStatId LIMIT 1")
    @Transaction
    fun getLineGraphByGraphStatId(graphStatId: Long): LineGraphWithFeatures?

    @Query("SELECT * FROM lua_graphs_table WHERE graph_stat_id = :graphStatId LIMIT 1")
    @Transaction
    fun getLuaGraphByGraphStatId(graphStatId: Long): LuaGraphWithFeatures?

    @Query("SELECT * FROM pie_charts_table2 WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getPieChartByGraphStatId(graphStatId: Long): PieChart?

    @Query("SELECT * FROM average_time_between_stat_table4 WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getAverageTimeBetweenStatByGraphStatId(graphStatId: Long): AverageTimeBetweenStat?

    @Query("SELECT * FROM graphs_and_stats_table2 WHERE group_id = :groupId ORDER BY display_index ASC, id DESC")
    fun getGraphsAndStatsByGroupIdSync(groupId: Long): List<GraphOrStat>

    @Query("SELECT * FROM graphs_and_stats_table2 ORDER BY display_index ASC, id DESC")
    fun getAllGraphStatsSync(): List<GraphOrStat>

    @Query(
        """
        SELECT * FROM (
            SELECT dp.epoch_milli as epoch_milli, dp.utc_offset_sec as utc_offset_sec, t.id as tracker_id, dp.feature_id as feature_id, f.name as feature_name, g.id as group_id, dp.note as note
            FROM data_points_table as dp
            LEFT JOIN features_table as f ON dp.feature_id = f.id
            LEFT JOIN trackers_table as t ON dp.feature_id = t.feature_id
            LEFT JOIN groups_table as g ON f.group_id = g.id
            WHERE dp.note IS NOT NULL AND dp.note != ""
        ) UNION SELECT * FROM (
            SELECT n.epoch_milli as epoch_milli, n.utc_offset_sec as utc_offset_sec, NULL as tracker_id, NULL as feature_id, NULL as feature_name, NULL as group_id, n.note as note
            FROM notes_table as n
        ) ORDER BY epoch_milli DESC
        """
    )
    fun getAllDisplayNotes(): Flow<List<DisplayNote>>

    @Query("UPDATE data_points_table SET note = '' WHERE epoch_milli = :epochMilli AND feature_id = :featureId")
    fun removeNote(epochMilli: Long, featureId: Long)

    @Delete
    fun deleteGlobalNote(note: GlobalNote)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGlobalNote(note: GlobalNote): Long

    @Query("SELECT * FROM notes_table WHERE epoch_milli = :epochMilli LIMIT 1")
    fun getGlobalNoteByTimeSync(epochMilli: Long): GlobalNote?

    @Query("SELECT * FROM notes_table")
    fun getAllGlobalNotesSync(): List<GlobalNote>

    @Query("DELETE FROM line_graph_features_table2 WHERE line_graph_id = :lineGraphId")
    fun deleteFeaturesForLineGraph(lineGraphId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLineGraphFeatures(lineGraphFeatures: List<LineGraphFeature>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLineGraph(lineGraph: LineGraph): Long

    @Update
    fun updateLineGraph(lineGraph: LineGraph)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPieChart(pieChart: PieChart): Long

    @Update
    fun updatePieChart(pieChart: PieChart)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat): Long

    @Update
    fun updateAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGraphOrStat(graphOrStat: GraphOrStat): Long

    @Update
    fun updateGraphOrStat(graphOrStat: GraphOrStat)

    @Update
    fun updateGraphStats(graphStat: List<GraphOrStat>)

    @Update
    fun updateTimeHistogram(timeHistogram: TimeHistogram)

    @Update
    fun updateLastValueStat(lastValueStat: LastValueStat)

    @Update
    fun updateBarChart(barChart: BarChart)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTimeHistogram(timeHistogram: TimeHistogram): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLastValueStat(lastValueStat: LastValueStat): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBarChart(barChart: BarChart): Long

    @Query("SELECT * FROM time_histograms_table WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getTimeHistogramByGraphStatId(graphStatId: Long): TimeHistogram?

    @Query("SELECT * FROM last_value_stats_table WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getLastValueStatByGraphStatId(graphStatId: Long): LastValueStat?

    @Query("SELECT * FROM bar_charts_table WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getBarChartByGraphStatId(graphStatId: Long): BarChart?

    @Query("SELECT * FROM groups_table WHERE parent_group_id = :id")
    fun getGroupsForGroupSync(id: Long): List<Group>

    @Query("SELECT * FROM groups_table WHERE parent_group_id IS NULL")
    fun getGroupsForGroupSync(): List<Group>

    @Query("SELECT * FROM groups_table WHERE parent_group_id IS NULL LIMIT 1")
    fun getRootGroupSync(): Group?

    @Query("$getTrackersQuery WHERE features_table.group_id = :groupId ORDER BY features_table.display_index ASC, id DESC")
    fun getTrackersForGroupSyncInternal(groupId: Long): List<TrackerWithFeature>

    @Query(getTrackersQuery)
    fun getAllTrackersSync(): List<TrackerWithFeature>

    @Query("$getTrackersQuery WHERE trackers_table.id = :id LIMIT 1")
    fun getTrackerById(id: Long): TrackerWithFeature?

    @Query("$getTrackersQuery WHERE trackers_table.feature_id = :featureId LIMIT 1")
    fun getTrackerByFeatureId(featureId: Long): TrackerWithFeature?

    @Query("$getDisplayTrackersQuery WHERE trackers_table.feature_id = :featureId LIMIT 1")
    fun getDisplayTrackerByFeatureIdSync(featureId: Long): DisplayTracker?

    @Query("SELECT COUNT(*) FROM trackers_table")
    fun numTrackers(): Int

    @Query("SELECT COUNT(*) FROM data_points_table")
    fun hasAtLeastOneDataPoint(): Boolean

    @Query("$getDisplayTrackersQuery WHERE trackers_table.feature_id IN (SELECT feature_id FROM feature_timers_table)")
    fun getAllActiveTimerTrackers(): List<DisplayTracker>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTracker(tracker: Tracker): Long

    @Update
    fun updateTracker(tracker: Tracker)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFeatureTimer(timer: FeatureTimer): Long

    @Delete
    fun deleteFeatureTimer(timer: FeatureTimer)

    @Query("DELETE FROM feature_timers_table WHERE feature_id = :featureId")
    fun deleteFeatureTimer(featureId: Long)

    @Query("SELECT * FROM feature_timers_table WHERE feature_id = :featureId LIMIT 1")
    fun getFeatureTimer(featureId: Long): FeatureTimer?

    @Query("SELECT * FROM feature_timers_table")
    fun getAllFeatureTimers(): List<FeatureTimer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLuaGraph(luaGraph: LuaGraph): Long

    @Update
    fun updateLuaGraph(luaGraph: LuaGraph)

    @Query("DELETE FROM lua_graph_features_table WHERE lua_graph_id = :luaGraphId")
    fun deleteFeaturesForLuaGraph(luaGraphId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLuaGraphFeatures(luaGraphFeatures: List<LuaGraphFeature>)

    // Return true if there is at least one Lua graph present in the database
    @Query("SELECT COUNT(*) > 0 FROM lua_graphs_table")
    fun hasAnyLuaGraphs(): Boolean

    // Return true if there is at least one graph/stat present in the database
    @Query("SELECT COUNT(*) > 0 FROM graphs_and_stats_table2")
    fun hasAnyGraphs(): Boolean

    // Return true if there is at least one feature present in the database
    @Query("SELECT COUNT(*) > 0 FROM features_table")
    fun hasAnyFeatures(): Boolean

    // Return true if there is at least one group present in the database
    @Query("SELECT COUNT(*) > 0 FROM groups_table")
    fun hasAnyGroups(): Boolean

    // Return true if there is at least one reminder present in the database
    @Query("SELECT COUNT(*) > 0 FROM reminders_table")
    fun hasAnyReminders(): Boolean

    // Return true if there is at least one function present in the database
    @Query("SELECT COUNT(*) > 0 FROM functions_table")
    fun hasAnyFunctions(): Boolean

    // Backwards-compatible function accessors used by interactor/helpers
    @Query("$getFunctionsQuery WHERE functions_table.id = :functionId LIMIT 1")
    fun getFunctionById(functionId: Long): FunctionWithFeature?

    @Query("SELECT * FROM function_input_features_table WHERE function_id = :functionId")
    fun getFunctionInputFeaturesSync(functionId: Long): List<FunctionInputFeature>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFunctionInputFeature(functionInputFeature: FunctionInputFeature): Long

    @Query("DELETE FROM function_input_features_table WHERE function_id = :functionId")
    fun deleteFunctionInputFeatures(functionId: Long)

    @Query(getFunctionsQuery)
    fun getAllFunctionsSync(): List<FunctionWithFeature>

    @Query("$getFunctionsQuery WHERE features_table.group_id = :groupId ORDER BY features_table.display_index ASC, id DESC")
    fun getFunctionsForGroupSync(groupId: Long): List<FunctionWithFeature>

    @Query("$getFunctionsQuery WHERE functions_table.feature_id = :featureId LIMIT 1")
    fun getFunctionByFeatureId(featureId: Long): FunctionWithFeature?

    @Query(getFunctionsQuery)
    fun getAllFunctions(): List<FunctionWithFeature>

    @Query("DELETE FROM function_input_features_table WHERE function_id = :functionId")
    fun deleteInputsForFunction(functionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFunctionInputs(functionInputs: List<FunctionInputFeature>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFunction(function: Function): Long

    @Update
    fun updateFunction(function: Function)

    @Query("SELECT * FROM trackers_table")
    fun getAllTrackers(): List<Tracker>

    @Query("SELECT * FROM function_input_features_table WHERE feature_id = :featureId")
    fun getFunctionInputsForFeature(featureId: Long): List<FunctionInputFeature>

    @Query("SELECT DISTINCT label FROM data_points_table WHERE feature_id IN (SELECT feature_id FROM trackers_table WHERE id = :trackerId) ORDER BY label")
    fun getLabelsForTracker(trackerId: Long): List<String>

    @Query("""
        SELECT graph_stat_id, feature_id FROM pie_charts_table2
        UNION
        SELECT graph_stat_id, feature_id FROM time_histograms_table
        UNION
        SELECT graph_stat_id, feature_id FROM last_value_stats_table
        UNION
        SELECT graph_stat_id, feature_id FROM bar_charts_table
        UNION
        SELECT graph_stat_id, feature_id FROM average_time_between_stat_table4
        UNION
        SELECT lg.graph_stat_id, lgf.feature_id 
        FROM line_graphs_table3 lg 
        JOIN line_graph_features_table2 lgf ON lg.id = lgf.line_graph_id
        UNION
        SELECT lua.graph_stat_id, lf.feature_id
        FROM lua_graphs_table lua
        JOIN lua_graph_features_table lf ON lua.id = lf.lua_graph_id
    """)
    fun getAllGraphDependencies(): List<GraphDependency>

    @Query("""
        SELECT f.feature_id as function_feature_id, fif.feature_id as input_feature_id
        FROM function_input_features_table fif
        JOIN functions_table f ON fif.function_id = f.id
    """)
    fun getAllFunctionDependencies(): List<FunctionDependency>
}

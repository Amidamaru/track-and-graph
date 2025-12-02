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

package com.samco.trackandgraph.data.notifications

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Intent
import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.Tracker
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import javax.inject.Inject

internal interface NotificationService {
    suspend fun checkAndNotifyThreshold(dataPoint: DataPoint, tracker: Tracker)
    fun triggerWidgetUpdate(featureId: Long)
}

internal class NotificationServiceImpl @Inject constructor(
    private val context: Context,
    private val dao: TrackAndGraphDatabaseDao
) : NotificationService {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "tracker_thresholds"
        internal const val ERROR_THRESHOLD_BASE_ID = 10000
        internal const val WARNING_THRESHOLD_BASE_ID = 20000
    }

    override suspend fun checkAndNotifyThreshold(dataPoint: DataPoint, tracker: Tracker) {
        try {
            // Nur wenn Schwellenwerte definiert sind (> -1)
            if (tracker.errorThreshold <= -1.0 && tracker.warningThreshold <= -1.0) {
                return
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as? NotificationManager ?: return

            // Berechtigungsprüfung für Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }

            // Error Threshold Überprüfung
            if (tracker.errorThreshold > -1.0 && dataPoint.value >= tracker.errorThreshold) {
                sendErrorThresholdNotification(
                    notificationManager,
                    tracker,
                    dataPoint
                )
            }
            // Warning Threshold Überprüfung (nur wenn nicht bereits Error)
            else if (tracker.warningThreshold > -1.0 && dataPoint.value >= tracker.warningThreshold) {
                sendWarningThresholdNotification(
                    notificationManager,
                    tracker,
                    dataPoint
                )
            }
            triggerWidgetUpdate(tracker.featureId)
        } catch (e: Exception) {
            Timber.e(e, "Error checking thresholds for tracker: ${'$'}{tracker.name}")
        }
    }

    private fun formatTimestamp(dataPoint: DataPoint): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            dataPoint.timestamp.format(formatter)
        } catch (_: Exception) {
            ""
        }
    }

    private fun calculateElapsedTime(currentDataPoint: DataPoint, tracker: Tracker): String {
        return try {
            val allDataPointEntities = dao.getDataPointsForFeatureSync(tracker.featureId)

            // Find the current data point and the previous one
            // Data points are ordered by epoch_milli DESC (newest first)
            val currentEpochMilli = currentDataPoint.timestamp.toInstant().toEpochMilli()
            val currentIndex = allDataPointEntities.indexOfFirst { entity ->
                entity.epochMilli == currentEpochMilli
            }

            if (currentIndex == -1 || currentIndex >= allDataPointEntities.size - 1) {
                // Either current point not found or no previous point exists
                return "0h"
            }

            val previousDataPointEntity = allDataPointEntities[currentIndex + 1]
            val currentInstant = Instant.ofEpochMilli(allDataPointEntities[currentIndex].epochMilli)
            val previousInstant = Instant.ofEpochMilli(previousDataPointEntity.epochMilli)

            val duration = Duration.between(previousInstant, currentInstant)

            // Format as human-readable string according to requirements:
            // - if < 1day: show only hours (no minutes)
            // - if >= 1day: show days + hours (no minutes)
            val totalHours = duration.toHours()
            val days = totalHours / 24
            val remainingHours = totalHours % 24

            return when {
                days > 0L -> "${days}d ${remainingHours}h"
                totalHours > 0L -> "${totalHours}h"
                else -> "< 1h"
            }
        } catch (e: Exception) {
            Timber.w(e, "Error calculating elapsed time for tracker: ${tracker.name}")
            "N/A"
        }
    }

    override fun triggerWidgetUpdate(featureId: Long) {
        val intent = Intent().apply {
            setClassName(context, "com.samco.trackandgraph.base.service.TrackWidgetProvider")
            putExtra("UPDATE_FEATURE_ID", featureId)
        }
        context.sendBroadcast(intent)
    }
    private fun replaceTemplatePlaceholders(
        template: String,
        tracker: Tracker,
        dataPoint: DataPoint
    ): String {
        val timeStr = formatTimestamp(dataPoint)
        val elapsedStr = calculateElapsedTime(dataPoint, tracker)

        return template
            .replace("{{name}}", tracker.name)
            .replace("{{value}}", dataPoint.value.toString())
            .replace("{{time}}", timeStr)
            .replace("{{elapsed}}", elapsedStr)
            .replace("{{errorThreshold}}", tracker.errorThreshold.toString())
            .replace("{{warningThreshold}}", tracker.warningThreshold.toString())
            .replace("{{label}}", dataPoint.label)
            .replace("{{note}}", dataPoint.note)
    }

    private fun sendErrorThresholdNotification(
        notificationManager: NotificationManager,
        tracker: Tracker,
        dataPoint: DataPoint
    ) {
        val notificationId = (ERROR_THRESHOLD_BASE_ID + tracker.id).toInt()
        val timeStr = formatTimestamp(dataPoint)
        val titleTemplate = tracker.notificationTitleTemplate ?: "Fehler-Schwelle überschritten"
        val title = replaceTemplatePlaceholders(titleTemplate, tracker, dataPoint)

        val bodyTemplate = tracker.notificationBodyTemplate ?: "Tracker: {{name}}\nZeit: {{time}}\nWert: {{value}}\nVerstrichene Zeit: {{elapsed}}\nFehler-Schwelle: {{errorThreshold}}"
        val body = replaceTemplatePlaceholders(bodyTemplate, tracker, dataPoint)
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon) // Entfernt Warn-Dreieck, nimmt App-Icon
            .setContentTitle(title)
            .setContentText("${tracker.name} • $timeStr")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()
        notificationManager.notify(notificationId, notification)
    }

    private fun sendWarningThresholdNotification(
        notificationManager: NotificationManager,
        tracker: Tracker,
        dataPoint: DataPoint
    ) {
        val notificationId = (WARNING_THRESHOLD_BASE_ID + tracker.id).toInt()
        val timeStr = formatTimestamp(dataPoint)
        val titleTemplate = tracker.notificationTitleTemplate ?: "Warn-Schwelle überschritten"
        val title = replaceTemplatePlaceholders(titleTemplate, tracker, dataPoint)

        val bodyTemplate = tracker.notificationBodyTemplate ?: "Tracker: {{name}}\nZeit: {{time}}\nWert: {{value}}\nVerstrichene Zeit: {{elapsed}}\nWarn-Schwelle: {{warningThreshold}}"
        val body = replaceTemplatePlaceholders(bodyTemplate, tracker, dataPoint)
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(title)
            .setContentText("${tracker.name} • $timeStr")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()
        notificationManager.notify(notificationId, notification)
    }
}

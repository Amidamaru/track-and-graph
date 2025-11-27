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
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.Tracker
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import javax.inject.Inject

interface NotificationService {
    suspend fun checkAndNotifyThreshold(dataPoint: DataPoint, tracker: Tracker)
}

class NotificationServiceImpl @Inject constructor(
    private val context: Context
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
        } catch (e: Exception) {
            Timber.e(e, "Error checking thresholds for tracker: ${'$'}{tracker.name}")
        }
    }

    private fun formatTimestamp(dataPoint: DataPoint): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            dataPoint.timestamp.format(formatter)
        } catch (e: Exception) {
            ""
        }
    }

    private fun sendErrorThresholdNotification(
        notificationManager: NotificationManager,
        tracker: Tracker,
        dataPoint: DataPoint
    ) {
        val notificationId = (ERROR_THRESHOLD_BASE_ID + tracker.id).toInt()
        val timeStr = formatTimestamp(dataPoint)
        val title = (tracker.notificationTitleTemplate ?: "Fehler-Schwelle überschritten")
            .replace("{{name}}", tracker.name)
            .replace("{{value}}", dataPoint.value.toString())
            .replace("{{time}}", timeStr)
        val bodyTemplate = tracker.notificationBodyTemplate ?: "Tracker: {{name}}\nZeit: {{time}}\nWert: {{value}}\nFehler-Schwelle: {{errorThreshold}}"
        val body = bodyTemplate
            .replace("{{name}}", tracker.name)
            .replace("{{value}}", dataPoint.value.toString())
            .replace("{{time}}", timeStr)
            .replace("{{errorThreshold}}", tracker.errorThreshold.toString())
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
        val title = (tracker.notificationTitleTemplate ?: "Warn-Schwelle überschritten")
            .replace("{{name}}", tracker.name)
            .replace("{{value}}", dataPoint.value.toString())
            .replace("{{time}}", timeStr)
        val bodyTemplate = tracker.notificationBodyTemplate ?: "Tracker: {{name}}\nZeit: {{time}}\nWert: {{value}}\nWarn-Schwelle: {{warningThreshold}}"
        val body = bodyTemplate
            .replace("{{name}}", tracker.name)
            .replace("{{value}}", dataPoint.value.toString())
            .replace("{{time}}", timeStr)
            .replace("{{warningThreshold}}", tracker.warningThreshold.toString())
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

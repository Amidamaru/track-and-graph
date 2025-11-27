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
        // Diese Konstanten MÜSSEN mit den Werten in TrackAndGraphApplication.kt übereinstimmen
        const val NOTIFICATION_CHANNEL_ID = "tracker_thresholds"
        const val NOTIFICATION_CHANNEL_NAME = "Tracker Schwellenwert-Benachrichtigungen"
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
            Timber.e(e, "Error checking thresholds for tracker: ${tracker.name}")
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
        val contentText = "${tracker.name} • $timeStr"

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Fehler-Schwelle überschritten")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Tracker: ${tracker.name}\nZeit: $timeStr\nWert: ${dataPoint.value}\nFehler-Schwelle: ${tracker.errorThreshold}"))
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
        val contentText = "${tracker.name} • $timeStr"

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⚠️ Warn-Schwelle überschritten")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Tracker: ${tracker.name}\nZeit: $timeStr\nWert: ${dataPoint.value}\nWarn-Schwelle: ${tracker.warningThreshold}"))
            .build()

        notificationManager.notify(notificationId, notification)
    }
}


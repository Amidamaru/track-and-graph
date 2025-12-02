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

package com.samco.trackandgraph.widgets

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import android.Manifest
import com.samco.trackandgraph.R
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import timber.log.Timber
import javax.inject.Inject

/**
 * Service für Benachrichtigungen bei Widget-Schwellenwert-Übergänge
 * Sendet Benachrichtigungen, wenn die Widget-Farbe wechselt (weiß -> gelb oder gelb -> rot)
 * basierend auf verstrichener Zeit (elapsed time)
 */
class WidgetThresholdNotificationService @Inject constructor(
    private val context: Context
) {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "widget_thresholds"
        const val NOTIFICATION_CHANNEL_NAME = "Widget Schwellenwert-Übergänge"
        private const val NOTIFICATION_BASE_ID = 30000
        private const val WIDGET_COLOR_PREFS = "WidgetColorPrefs"

        // Widget-Farb-Status Enum
        enum class ColorStatus {
            WHITE,      // Normal (grau/normal, < warningThreshold)
            YELLOW,     // Warnung (>= warningThreshold, < errorThreshold)
            RED         // Fehler (>= errorThreshold)
        }

        fun getColorStatus(
            lastTimestampMillis: Long?,
            warningThreshold: Double,
            errorThreshold: Double
        ): ColorStatus {
            if (lastTimestampMillis == null || lastTimestampMillis <= 0L) {
                return ColorStatus.WHITE
            }

            val now = Instant.now()
            val then = Instant.ofEpochMilli(lastTimestampMillis)
            val duration = Duration.between(then, now)
            val hours = duration.toMinutes() / 60.0

            return when {
                errorThreshold >= 0 && hours >= errorThreshold -> ColorStatus.RED
                warningThreshold >= 0 && hours >= warningThreshold -> ColorStatus.YELLOW
                else -> ColorStatus.WHITE
            }
        }
    }

    init {
        createChannel()
    }

    /**
     * Prüfe auf Farbwechsel und sende Benachrichtigung wenn nötig
     */
    fun checkAndNotifyColorChange(
        featureId: Long,
        trackerName: String,
        lastTimestampMillis: Long?,
        warningThreshold: Double,
        errorThreshold: Double,
        notificationTitleTemplate: String? = null,
        notificationBodyTemplate: String? = null,
        elapsedTime: String = "N/A",
        trackerValue: Double? = null,
        warningThresholdValue: Double? = null,
        errorThresholdValue: Double? = null
    ) {
        try {
            Timber.d("checkAndNotifyColorChange called for: $trackerName, titleTemplate=$notificationTitleTemplate, bodyTemplate=$notificationBodyTemplate")

            val currentColor = getColorStatus(lastTimestampMillis, warningThreshold, errorThreshold)
            val previousColor = getPreviousColorStatus(featureId)

            Timber.d("Color change: $previousColor -> $currentColor")

            // Speichere die neue Farbe
            savePreviousColorStatus(featureId, currentColor)

            // Sende Benachrichtigung nur wenn sich die Farbe geändert hat
            if (previousColor != currentColor) {
                when {
                    currentColor == ColorStatus.RED && previousColor == ColorStatus.YELLOW -> {
                        Timber.d("Sending ERROR notification (YELLOW->RED)")
                        sendErrorThresholdNotification(
                            featureId, trackerName, notificationTitleTemplate, notificationBodyTemplate,
                            elapsedTime, trackerValue, warningThresholdValue, errorThresholdValue
                        )
                    }
                    currentColor == ColorStatus.RED && previousColor == ColorStatus.WHITE -> {
                        // Direkter Sprung zu ROT (seltener Fall)
                        Timber.d("Sending ERROR notification (WHITE->RED)")
                        sendErrorThresholdNotification(
                            featureId, trackerName, notificationTitleTemplate, notificationBodyTemplate,
                            elapsedTime, trackerValue, warningThresholdValue, errorThresholdValue
                        )
                    }
                    currentColor == ColorStatus.YELLOW && previousColor == ColorStatus.WHITE -> {
                        Timber.d("Sending WARNING notification (WHITE->YELLOW)")
                        sendWarningThresholdNotification(
                            featureId, trackerName, notificationTitleTemplate, notificationBodyTemplate,
                            elapsedTime, trackerValue, warningThresholdValue, errorThresholdValue
                        )
                    }
                    // Falls Farbe zurückgeht (Datenpunkt wurde gelöscht/bearbeitet)
                    currentColor == ColorStatus.WHITE && previousColor != ColorStatus.WHITE -> {
                        // Optional: Benachrichtigung dass Schwelle nicht mehr überschritten
                        // Für jetzt: keine Benachrichtigung
                    }
                    currentColor == ColorStatus.YELLOW && previousColor == ColorStatus.RED -> {
                        // Farbe geht von rot zu gelb zurück - keine Benachrichtigung
                    }
                }
            } else {
                Timber.d("No color change: $currentColor == $previousColor")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking widget color change for feature: $featureId")
        }
    }

    private fun sendErrorThresholdNotification(
        featureId: Long,
        trackerName: String,
        notificationTitleTemplate: String? = null,
        notificationBodyTemplate: String? = null,
        elapsedTime: String = "N/A",
        trackerValue: Double? = null,
        warningThresholdValue: Double? = null,
        errorThresholdValue: Double? = null
    ) {
        try {
            Timber.d("sendErrorThresholdNotification: titleTemplate=$notificationTitleTemplate, bodyTemplate=$notificationBodyTemplate")

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

            val notificationId = (NOTIFICATION_BASE_ID + featureId).toInt()
            val timeStr = Instant.now().toString().substring(11, 19) // HH:mm:ss

            val title = if (notificationTitleTemplate != null) {
                replaceTemplatePlaceholders(
                    notificationTitleTemplate, trackerName, timeStr, elapsedTime,
                    trackerValue, warningThresholdValue, errorThresholdValue
                )
            } else {
                "⚠️ Fehler-Schwelle überschritten"
            }

            val bodyText = if (notificationBodyTemplate != null) {
                replaceTemplatePlaceholders(
                    notificationBodyTemplate, trackerName, timeStr, elapsedTime,
                    trackerValue, warningThresholdValue, errorThresholdValue
                )
            } else {
                "Tracker: $trackerName\nZeit: $timeStr\nDie Fehler-Schwelle wurde überschritten"
            }

            Timber.d("Final title: $title, bodyText: $bodyText")

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.warning_icon)
                .setContentTitle(title)
                .setContentText(bodyText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
                .build()

            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            Timber.e(e, "Error sending error notification for feature: $featureId")
        }
    }

    private fun sendWarningThresholdNotification(
        featureId: Long,
        trackerName: String,
        notificationTitleTemplate: String? = null,
        notificationBodyTemplate: String? = null,
        elapsedTime: String = "N/A",
        trackerValue: Double? = null,
        warningThresholdValue: Double? = null,
        errorThresholdValue: Double? = null
    ) {
        try {
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

            val notificationId = (NOTIFICATION_BASE_ID + featureId).toInt()
            val timeStr = Instant.now().toString().substring(11, 19) // HH:mm:ss

            val title = if (notificationTitleTemplate != null) {
                replaceTemplatePlaceholders(
                    notificationTitleTemplate, trackerName, timeStr, elapsedTime,
                    trackerValue, warningThresholdValue, errorThresholdValue
                )
            } else {
                "⚠️ Warn-Schwelle überschritten"
            }

            val bodyText = if (notificationBodyTemplate != null) {
                replaceTemplatePlaceholders(
                    notificationBodyTemplate, trackerName, timeStr, elapsedTime,
                    trackerValue, warningThresholdValue, errorThresholdValue
                )
            } else {
                "Tracker: $trackerName\nZeit: $timeStr\nDie Warn-Schwelle wurde überschritten"
            }

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.warning_icon)
                .setContentTitle(title)
                .setContentText(bodyText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
                .build()

            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            Timber.e(e, "Error sending warning notification for feature: $featureId")
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    /**
     * Speichere den aktuellen Farbstatus für einen Tracker
     */
    private fun savePreviousColorStatus(featureId: Long, status: ColorStatus) {
        val prefs = context.getSharedPreferences(WIDGET_COLOR_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(getColorStatusKey(featureId), status.name).apply()
    }

    /**
     * Hole den gespeicherten Farbstatus eines Trackers
     */
    private fun getPreviousColorStatus(featureId: Long): ColorStatus {
        val prefs = context.getSharedPreferences(WIDGET_COLOR_PREFS, Context.MODE_PRIVATE)
        val statusName = prefs.getString(getColorStatusKey(featureId), ColorStatus.WHITE.name)
        return try {
            ColorStatus.valueOf(statusName ?: ColorStatus.WHITE.name)
        } catch (e: Exception) {
            ColorStatus.WHITE
        }
    }

    private fun getColorStatusKey(featureId: Long) = "widget_color_status_$featureId"

    private fun replaceTemplatePlaceholders(
        template: String,
        trackerName: String,
        timeStr: String,
        elapsedTime: String,
        trackerValue: Double? = null,
        warningThresholdValue: Double? = null,
        errorThresholdValue: Double? = null
    ): String {
        return template
            .replace("{{name}}", trackerName)
            .replace("{{time}}", timeStr)
            .replace("{{elapsed}}", elapsedTime)
            .replace("{{value}}", trackerValue?.toString() ?: "N/A")
            .replace("{{warningThreshold}}", warningThresholdValue?.toString() ?: "N/A")
            .replace("{{errorThreshold}}", errorThresholdValue?.toString() ?: "N/A")
    }
}


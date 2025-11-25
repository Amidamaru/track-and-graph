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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.samco.trackandgraph.base.service.TrackWidgetProvider
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.widgets.TrackWidgetState.WIDGET_PREFS_NAME
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrackWidgetSettingsActivity : AppCompatActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras
            ?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            TnGComposeTheme {
                 TrackWidgetSettingsDialog(
                    appWidgetId = appWidgetId,
                    onTransparencyChanged = ::onTransparencyChanged,
                    onDismissRequest = ::onDismiss
                )
            }
        }
    }

    private fun onTransparencyChanged(transparency: Double) {
        // Speichere neue Transparenz
        getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE).edit {
            putFloat("widget_transparency_$appWidgetId", transparency.toFloat())
        }

        // Trigger Widget-Update
        val intent = Intent(
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            null,
            this,
            TrackWidgetProvider::class.java
        )
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        sendBroadcast(intent)

        finish()
    }

    private fun onDismiss() {
        finish()
    }
}


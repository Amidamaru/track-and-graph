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
import androidx.core.content.edit
import com.samco.trackandgraph.base.service.TrackWidgetProvider
import com.samco.trackandgraph.widgets.TrackWidgetState.WIDGET_PREFS_NAME

/**
 * Manager für zentrale Widget-Transparenz-Einstellungen
 */
object WidgetTransparencyManager {
    const val GLOBAL_TRANSPARENCY_KEY = "global_widget_transparency"
    const val DEFAULT_TRANSPARENCY = 1.0f // 100%

    /**
     * Speichere globale Transparenz-Einstellung
     */
    fun setGlobalTransparency(context: Context, transparency: Float) {
        val prefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putFloat(GLOBAL_TRANSPARENCY_KEY, transparency)
        }
        // Aktualisiere alle Widgets
        updateAllWidgets(context)
    }

    /**
     * Lade globale Transparenz-Einstellung
     */
    fun getGlobalTransparency(context: Context): Float {
        val prefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(GLOBAL_TRANSPARENCY_KEY, DEFAULT_TRANSPARENCY)
    }

    /**
     * Lade Transparenz für ein bestimmtes Widget
     * Priorisierung: Widget-spezifisch > Global
     */
    fun getWidgetTransparency(context: Context, appWidgetId: Int): Float {
        val prefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
        val widgetKey = "widget_transparency_$appWidgetId"
        val widgetTransparency = prefs.getFloat(widgetKey, -1f)

        // Wenn Widget-spezifische Transparenz gesetzt, verwende diese
        // Sonst verwende globale
        return if (widgetTransparency >= 0f) {
            widgetTransparency
        } else {
            getGlobalTransparency(context)
        }
    }

    /**
     * Aktualisiere alle Widgets mit neuer Transparenz
     */
    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, TrackWidgetProvider::class.java)
        )

        if (appWidgetIds.isNotEmpty()) {
            val intent = Intent(
                AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                null,
                context,
                TrackWidgetProvider::class.java
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.sendBroadcast(intent)
        }
    }
}


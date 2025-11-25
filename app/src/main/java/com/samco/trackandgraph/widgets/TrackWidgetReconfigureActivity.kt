package com.samco.trackandgraph.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.service.TrackWidgetProvider
import com.samco.trackandgraph.selectitemdialog.SelectItemDialog
import com.samco.trackandgraph.selectitemdialog.SelectableItemType
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.widgets.TrackWidgetState.WIDGET_PREFS_NAME
import com.samco.trackandgraph.widgets.TrackWidgetState.getFeatureIdPref
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrackWidgetReconfigureActivity : AppCompatActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isNewWidget: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras
            ?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Prüfe ob Widget bereits konfiguriert ist
        val featureId = getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(getFeatureIdPref(appWidgetId), -1L)

        isNewWidget = featureId == -1L

        if (isNewWidget) {
            // Neues Widget - zeige Tracker-Auswahl
            showTrackerSelection()
        } else {
            // Bestehendes Widget - zeige Transparenz-Einstellungen
            showTransparencySettings()
        }
    }

    private fun showTrackerSelection() {
        setContent {
            TnGComposeTheme {
                SelectItemDialog(
                    title = stringResource(R.string.select_a_tracker),
                    selectableTypes = setOf(SelectableItemType.TRACKER),
                    onFeatureSelected = ::onTrackerSelected,
                    onDismissRequest = ::onDismiss
                )
            }
        }
    }

    private fun showTransparencySettings() {
        val currentTransparency = getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat("widget_transparency_$appWidgetId", 1.0f)

        setContent {
            TnGComposeTheme {
                TrackWidgetSettingsDialog(
                    appWidgetId = appWidgetId,
                    currentTransparency = currentTransparency.toDouble(),
                    onTransparencyChanged = ::onTransparencyChanged,
                    onDismissRequest = ::onDismiss
                )
            }
        }
    }

    private fun onTrackerSelected(featureId: Long?) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        if (featureId == null || featureId == -1L) {
            val errorMessage = getString(R.string.track_widget_configure_no_data_selected_error)
            Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
            return
        }

        // Speichere den Tracker mit Standard-Transparenz (1.0 = 100% opak)
        getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE).edit {
            putLong(getFeatureIdPref(appWidgetId), featureId)
            putFloat("widget_transparency_$appWidgetId", 1.0f)
        }

        val intent = Intent(
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            null,
            this,
            TrackWidgetProvider::class.java
        )
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        sendBroadcast(intent)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
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


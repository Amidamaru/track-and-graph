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

@file:OptIn(ExperimentalGlancePreviewApi::class)

package com.samco.trackandgraph.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.material3.ColorProviders
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import org.threeten.bp.Instant
import org.threeten.bp.Duration
import java.util.Locale
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.service.StartTimerAction
import com.samco.trackandgraph.base.service.StartTimerAction.Companion.FeatureIdKey
import com.samco.trackandgraph.ui.compose.theming.DarkColorScheme
import com.samco.trackandgraph.ui.compose.theming.LightColorScheme
import com.samco.trackandgraph.ui.compose.ui.buttonSize

class TrackWidgetGlance : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            TrackWidgetTheme {
                val prefs = currentState<Preferences>()
                TrackWidgetContent(
                    context = context,
                    widgetData = TrackWidgetState.createWidgetDataFromPreferences(prefs),
                )
            }
        }
    }
}

@Composable
private fun TrackWidgetTheme(
    content: @Composable () -> Unit
) = GlanceTheme(
    colors = ColorProviders(
        light = LightColorScheme,
        dark = DarkColorScheme,
    )
) {
    content()
}

@Composable
private fun TrackWidgetContent(
    context: Context,
    widgetData: TrackWidgetState.WidgetData,
) {
    when (widgetData) {
        is TrackWidgetState.WidgetData.Disabled -> {
            DisabledWidgetContent()
        }

        is TrackWidgetState.WidgetData.Enabled -> {
            EnabledWidgetContent(
                data = widgetData,
                onWidgetClick = TrackWidgetInputDataPointActivity
                    .startActivityInputDataPoint(context, widgetData.featureId),
                onStartTimer = actionRunCallback<StartTimerAction>(
                    actionParametersOf(FeatureIdKey to widgetData.featureId)
                ),
                onStopTimer = TrackWidgetInputDataPointActivity
                    .startActivityStopTimer(context, widgetData.featureId)
            )
        }
    }
}

@Composable
private fun DisabledWidgetContent() {
    Column(
        modifier = GlanceModifier
            .background(ImageProvider(R.drawable.track_widget_card))
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(R.drawable.warning_icon),
            contentDescription = "Widget disabled",
            modifier = GlanceModifier.size(buttonSize),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.error)
        )
    }
}

// Widget dimensions and scaling constants
private val WIDGET_BASE_WIDTH = 120.dp
private val WIDGET_BASE_HEIGHT = 80.dp
private const val WIDGET_TITLE_SIZE = 12
private const val WIDGET_ICON_SIZE = 22

@Composable
private fun EnabledWidgetContent(
    data: TrackWidgetState.WidgetData.Enabled,
    onWidgetClick: Action,
    onStartTimer: Action,
    onStopTimer: Action
) {
    val size = LocalSize.current
    val sizeArea = size.width.value * size.height.value
    val baseArea = WIDGET_BASE_WIDTH.value * WIDGET_BASE_HEIGHT.value
    val scale = (sizeArea / baseArea).coerceIn(1.0f, 1.8f)

    fun sdp(v: Int) = (v * scale).dp
    fun ssp(v: Int) = (v * scale).sp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.track_widget_card))
            // There is a 4dp inset on the end and bottom of the widget card
            .padding(end = 4.dp, bottom = 4.dp)
            .clickable(onWidgetClick),
    ) {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // Title
            Text(
                text = data.title,
                style = TextStyle(
                    fontSize = ssp(WIDGET_TITLE_SIZE),
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface,
                    textAlign = TextAlign.Center
                ),
                modifier = GlanceModifier
                    .padding(
                        start = sdp(8),
                        end = sdp(8),
                        bottom = sdp(WIDGET_ICON_SIZE / 2),
                    )
            )
        }

        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd,
        ) {

            Image(
                provider = ImageProvider(R.drawable.ic_add_record),
                contentDescription = null,
                modifier = GlanceModifier.size(sdp(WIDGET_ICON_SIZE)),
                colorFilter = ColorFilter.tint(
                    if (data.requireInput) GlanceTheme.colors.onSurface
                    else GlanceTheme.colors.secondary
                )
            )
        }
        // Bottom row (right aligned)
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Optional timer buttons
            if (data.isDuration) {
                if (data.timerRunning) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_stop_timer),
                        contentDescription = null,
                        modifier = GlanceModifier
                            .size(sdp(WIDGET_ICON_SIZE))
                            .clickable(onStopTimer),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.error)
                    )
                } else {
                    Image(
                        provider = ImageProvider(R.drawable.ic_play_timer),
                        contentDescription = null,
                        modifier = GlanceModifier
                            .size(sdp(WIDGET_ICON_SIZE))
                            .clickable(onStartTimer),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onError)
                    )
                }
            }
        }
        // Bottom-left: time since last timestamp (if present)
        data.lastTimestampMillis?.let { ts ->
            if (ts > 0L) {
                val rel = formatRelativeTimeGerman(ts)
                
                val now = Instant.now()
                val then = Instant.ofEpochMilli(ts)
                val duration = Duration.between(then, now)
                val hours = duration.toMinutes() / 60.0
                
                val textColor = when {
                    data.errorThreshold >= 0 && hours >= data.errorThreshold -> GlanceTheme.colors.error
                    data.warningThreshold >= 0 && hours >= data.warningThreshold -> ColorProvider(Color.Yellow)
                    else -> GlanceTheme.colors.onSurface
                }
                
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Text(
                        text = rel,
                        style = TextStyle(
                            fontSize = ssp(10),
                            color = textColor
                        ),
                        modifier = GlanceModifier.padding(start = sdp(6), bottom = sdp(6))
                    )
                }
            }
        }
    }
}

private fun formatRelativeTimeGerman(epochMillis: Long): String {
    return try {
        val then = Instant.ofEpochMilli(epochMillis)
        val now = Instant.now()
        val dur = Duration.between(then, now)
        val days = dur.toDays()
        val hours = dur.toHours() % 24

        if (days > 0) {
            "${days}d ${hours}h"
        } else {
            "${hours}h"
        }
    } catch (e: Exception) {
        ""
    }
}


@Preview(widthDp = 200, heightDp = 100)
@Composable
fun TrackWidgetEnabledPreview() {
    TrackWidgetTheme {
        EnabledWidgetContent(
            data = TrackWidgetState.WidgetData.Enabled(
                appWidgetId = 1,
                featureId = 1,
                title = "Daily Steps",
                requireInput = true,
                isDuration = false,
                timerRunning = false,
                lastTimestampMillis = null, // Added this
                warningThreshold = -1.0,
                errorThreshold = -1.0
            ),
            onWidgetClick = emptyAction(),
            onStartTimer = emptyAction(),
            onStopTimer = emptyAction()
        )
    }
}

@Preview(widthDp = 200, heightDp = 100)
@Composable
fun TrackWidgetEnabledWithTimerPreview() {
    TrackWidgetTheme {
        EnabledWidgetContent(
            data = TrackWidgetState.WidgetData.Enabled(
                appWidgetId = 1,
                featureId = 1,
                requireInput = false,
                title = "Work Session",
                isDuration = true,
                timerRunning = false,
                lastTimestampMillis = null, // Added this
                warningThreshold = -1.0,
                errorThreshold = -1.0
            ),
            onWidgetClick = emptyAction(),
            onStartTimer = emptyAction(),
            onStopTimer = emptyAction()
        )
    }
}

@Preview(widthDp = 200, heightDp = 100)
@Composable
fun TrackWidgetEnabledTimerRunningPreview() {
    TrackWidgetTheme {
        EnabledWidgetContent(
            data = TrackWidgetState.WidgetData.Enabled(
                appWidgetId = 1,
                featureId = 1,
                requireInput = false,
                title = "Work Session",
                isDuration = true,
                timerRunning = true,
                lastTimestampMillis = System.currentTimeMillis(), // Added this to test the time display
                warningThreshold = -1.0,
                errorThreshold = -1.0
            ),
            onWidgetClick = emptyAction(),
            onStartTimer = emptyAction(),
            onStopTimer = emptyAction(),
        )
    }
}

@Preview(widthDp = 200, heightDp = 100)
@Composable
fun TrackWidgetDisabledPreview() {
    TrackWidgetTheme {
        DisabledWidgetContent()
    }
}

private fun emptyAction(): Action = object : Action {}

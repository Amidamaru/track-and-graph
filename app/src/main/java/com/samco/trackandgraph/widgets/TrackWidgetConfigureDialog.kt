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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackWidgetConfigureDialog(
    onConfigurationComplete: (Double) -> Unit,
    onDismissRequest: () -> Unit
) {
    val transparency = remember { mutableStateOf(1.0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(
                    MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.widget_configure_transparency),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Transparenz: ${(transparency.value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Slider(
                value = transparency.value,
                onValueChange = { transparency.value = it },
                valueRange = 0f..1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                steps = 9  // 0, 10, 20, ..., 100%
            )

            Text(
                text = stringResource(R.string.widget_configure_transparency_description),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )

            Button(
                onClick = { onConfigurationComplete(transparency.value.toDouble()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(stringResource(android.R.string.ok))
            }

            Button(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    }
}


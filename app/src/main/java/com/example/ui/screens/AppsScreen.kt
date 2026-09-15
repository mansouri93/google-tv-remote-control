package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TvApp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.util.LocalAppStrings
import com.example.util.LocalLiteMode

@Composable
fun AppsScreen(
    apps: List<TvApp>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onLaunchApp: (TvApp) -> Unit,
    onForceStopApp: (TvApp) -> Unit,
    onOpenAppDetails: (TvApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "${strings.installedApps} (${filteredApps.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = strings.installedAppsSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("apps_search_field"),
            placeholder = { Text(strings.searchAppsPlaceholder, color = Slate400, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Slate400
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = strings.clear,
                            tint = Slate400
                        )
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Slate850,
                unfocusedContainerColor = Slate850,
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = Slate700
            ),
            singleLine = true
        )

        // Grid of Apps
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                TvAppCard(
                    app = app,
                    onLaunch = { onLaunchApp(app) },
                    onForceStop = { onForceStopApp(app) },
                    onDetails = { onOpenAppDetails(app) }
                )
            }
        }
    }
}

@Composable
fun TvAppCard(
    app: TvApp,
    onLaunch: () -> Unit,
    onForceStop: () -> Unit,
    onDetails: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isLite = LocalLiteMode.current
    var showMenu by remember { mutableStateOf(false) }

    val iconColor = remember(app.packageName) {
        when {
            app.packageName.contains("youtube") -> CoralAccent
            app.packageName.contains("netflix") -> Color(0xFFE50914)
            app.packageName.contains("spotify") -> EmeraldAccent
            app.packageName.contains("vlc") -> AmberAccent
            app.packageName.contains("disney") -> CyanAccent
            app.packageName.contains("prime") -> Color(0xFF00A8E1)
            else -> IndigoAccent
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onLaunch() }
            .testTag("app_card_${app.packageName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate850),
        border = if (isLite) null else CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // App Logo Placeholder Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconColor.copy(alpha = 0.2f))
                        .then(if (isLite) Modifier else Modifier.border(1.5.dp, iconColor, RoundedCornerShape(12.dp))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.name.take(2).uppercase(),
                        color = iconColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }

                // 3 dots menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = strings.menu,
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Slate900)
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.appDetails, color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Info, contentDescription = null, tint = CyanAccent)
                            },
                            onClick = {
                                showMenu = false
                                onDetails()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.forceStop, color = CoralAccent, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.StopCircle, contentDescription = null, tint = CoralAccent)
                            },
                            onClick = {
                                showMenu = false
                                onForceStop()
                            }
                        )
                    }
                }
            }

            // App Name & Package
            Column {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontSize = 11.sp
                )
            }

            // Launch Button
            Button(
                onClick = onLaunch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Slate800,
                    contentColor = CyanAccent
                ),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = strings.launchOnTv,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

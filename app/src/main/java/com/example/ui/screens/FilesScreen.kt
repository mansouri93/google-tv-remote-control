package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TvFileItem
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
fun FilesScreen(
    currentDirectory: String,
    files: List<TvFileItem>,
    isConnected: Boolean = true,
    onConnectClick: () -> Unit = {},
    onNavigateFolder: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteFile: (TvFileItem) -> Unit,
    onUploadSampleFile: (String) -> Unit,
    onPlayMedia: (TvFileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isLite = LocalLiteMode.current
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    var showUploadDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isConnected) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate850)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = strings.filesNotConnectedTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = strings.filesNotConnectedDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Button(
                        onClick = onConnectClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(strings.connectToTv, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Storage Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate850),
            border = if (isLite) null else CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.internalStorage,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = strings.storageFree,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent
                    )
                }
                LinearProgressIndicator(
                    progress = { 0.6f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = CyanAccent,
                    trackColor = Slate800
                )
            }
        }

        // Action bar: Breadcrumb & Folder creation / file upload buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Up Button and Current Path
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateUp,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Slate800)
                        .testTag("file_btn_up")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.parentFolder,
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentDirectory,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Top action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Upload/Send file to TV
                IconButton(
                    onClick = { showUploadDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Slate800)
                        .testTag("file_btn_upload")
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = strings.sendFilesToTv,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // New Folder
                IconButton(
                    onClick = { showNewFolderDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Slate800)
                        .testTag("file_btn_new_folder")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = strings.newFolder,
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // File List
        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.emptyFolder,
                    color = Slate400,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files, key = { it.path }) { item ->
                    FileRowItem(
                        item = item,
                        onClick = {
                            if (item.isDirectory) {
                                onNavigateFolder(item.path)
                            } else if (item.fileType == "video" || item.fileType == "audio") {
                                onPlayMedia(item)
                            }
                        },
                        onDelete = { onDeleteFile(item) },
                        onPlay = { onPlayMedia(item) }
                    )
                }
            }
        }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text(strings.createNewFolderTitle, color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text(strings.folderNamePlaceholder, color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = Slate700
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onCreateFolder(newFolderName.trim())
                            newFolderName = ""
                            showNewFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950)
                ) {
                    Text(strings.create)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text(strings.cancel, color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }

    // Upload to TV Dialog
    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = { Text(strings.sendFilesToTv, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.selectFileTypeToSend, color = Slate400, fontSize = 13.sp)

                    Button(
                        onClick = {
                            onUploadSampleFile("apk")
                            showUploadDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = EmeraldAccent)
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.sendApk)
                    }

                    Button(
                        onClick = {
                            onUploadSampleFile("video")
                            showUploadDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = CyanAccent)
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.sendVideo)
                    }

                    Button(
                        onClick = {
                            onUploadSampleFile("music")
                            showUploadDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = AmberAccent)
                    ) {
                        Icon(Icons.Default.Audiotrack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.sendAudio)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) {
                    Text(strings.close, color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }
}

@Composable
fun FileRowItem(
    item: TvFileItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isLite = LocalLiteMode.current
    val (icon: ImageVector, color: Color) = when (item.fileType) {
        "folder" -> Pair(Icons.Default.Folder, AmberAccent)
        "video" -> Pair(Icons.Default.Movie, CyanAccent)
        "audio" -> Pair(Icons.Default.Audiotrack, EmeraldAccent)
        "apk" -> Pair(Icons.Default.Android, EmeraldAccent)
        "image" -> Pair(Icons.Default.Image, IndigoAccent)
        else -> Pair(Icons.Default.Description, Slate400)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("file_item_${item.name}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Slate850),
        border = if (isLite) null else CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                }
            }

            // Quick item actions: Play on TV or Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.fileType == "video" || item.fileType == "audio") {
                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = strings.playOnTv,
                            tint = CyanAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = strings.delete,
                        tint = CoralAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

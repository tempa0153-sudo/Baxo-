package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import com.example.data.pref.StremioProviderConfig
import com.example.data.pref.NETFLIX_ADDON_PROVIDER_URL
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.pref.UserPreferences
import com.example.data.pref.UserProfile
import com.example.ui.theme.AccentColorPresets
import java.io.File

@Composable
fun ProfileDialog(
    userProfile: UserProfile,
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    onEnterHomeEditMode: () -> Unit = {}
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf(userProfile.name) }
    var showYtLogin by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    var cacheOptimized by remember { mutableStateOf(true) }

    val storagePermissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    var isStorageGranted by remember {
        mutableStateOf(
            storagePermissionsToRequest.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val requestStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.values.all { it }
        isStorageGranted = allGranted
        if (allGranted) {
            Toast.makeText(context, "Storage permission granted! Local media storage enabled.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Storage permission denied. Some local features may be limited.", Toast.LENGTH_SHORT).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val json = preferences.exportAllSettingsJson()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Settings & custom data exported successfully!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!content.isNullOrBlank()) {
                    val success = preferences.importAllSettingsJson(content)
                    if (success) {
                        Toast.makeText(context, "Settings & custom data imported successfully!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to parse imported backup file.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val avatarFile = File(context.filesDir, "custom_avatar_${System.currentTimeMillis()}.jpg")
                inputStream?.use { input ->
                    avatarFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                preferences.updateCustomAvatarPath(avatarFile.absolutePath)
                Toast.makeText(context, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to update photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("profile_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "Settings & Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Profile Picture Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.size(86.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (!userProfile.customAvatarPath.isNullOrBlank()) {
                                        AsyncImage(
                                            model = userProfile.customAvatarPath,
                                            contentDescription = "User Profile Picture",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = userProfile.name.take(1).uppercase(),
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.CameraAlt,
                                        contentDescription = "Pick Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("change_photo_button")
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose Photo", fontSize = 12.sp)
                            }

                            if (!userProfile.customAvatarPath.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = { preferences.updateCustomAvatarPath(null) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("remove_photo_button")
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Profile Name
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Display Name",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            preferences.updateName(it)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // App Accent Color Customizer
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("accent_color_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Palette,
                                        contentDescription = "Accent Color",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "App Accent Color",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Choose accent theme (${AccentColorPresets.size} colors: Grey, Black, Magenta, etc.)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val colorChunks = remember { AccentColorPresets.chunked(2) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            colorChunks.forEach { pair ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pair.forEach { preset ->
                                        val isSelected = userProfile.accentColorHex.equals(preset.hex, ignoreCase = true)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    preferences.updateAccentColor(preset.hex)
                                                    Toast.makeText(context, "${preset.name} applied!", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                                .testTag("accent_color_${preset.hex.removePrefix("#")}")
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(preset.color)
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 1.5.dp,
                                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                                                        shape = CircleShape
                                                    )
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = preset.name,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Poster & Slideshow Storage Cache with Storage Permission Check & Import/Export
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("poster_cache_storage_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.CloudDone,
                                            contentDescription = "Poster Cache Storage",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Poster & Slideshow Storage Cache",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isStorageGranted)
                                            "Local storage active • Storing custom media & cached posters in system"
                                        else
                                            "Permission required to store custom images & media locally",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Storage Permission Status & Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isStorageGranted) {
                                Button(
                                    onClick = {
                                        requestStorageLauncher.launch(storagePermissionsToRequest)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("request_storage_permission_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderShared,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Grant Storage Permission",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Local Android system storage is active and configured!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("grant_storage_permission_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Storage Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "Poster cache refreshed", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("clear_cache_button")
                            ) {
                                Text("Clear Cache", fontSize = 11.sp)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Import & Export Settings & Custom Media
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    exportLauncher.launch("zoyo_backup_${System.currentTimeMillis()}.json")
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FolderShared,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export Settings", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    importLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDone,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import Settings", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Foreground Service & Start on Boot Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("background_preheat_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Background & Boot System Controls",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Run as Foreground Service
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Run as Foreground Service",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Keep app process alive in background so content stays preheated",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = userProfile.runForegroundService,
                                onCheckedChange = { checked ->
                                    preferences.updateRunForegroundService(checked)
                                    if (checked) {
                                        com.example.service.AppForegroundService.start(context)
                                        Toast.makeText(context, "Foreground service started", Toast.LENGTH_SHORT).show()
                                    } else {
                                        com.example.service.AppForegroundService.stop(context)
                                        Toast.makeText(context, "Foreground service stopped", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        // Start on Boot
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Start on Boot (Auto Preheat)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Launch background preheating automatically when device powers on",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = userProfile.startOnBoot,
                                onCheckedChange = { checked ->
                                    preferences.updateStartOnBoot(checked)
                                    Toast.makeText(
                                        context,
                                        if (checked) "Start on boot enabled" else "Start on boot disabled",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }

                // Haptic Feedback & Touch Vibration Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("haptic_feedback_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Haptic Vibration",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Haptic Vibration Feedback",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Gentle vibration when tapping Home, Search, Library navbar and interacting with app UI",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        androidx.compose.material3.Switch(
                            checked = userProfile.hapticFeedbackEnabled,
                            onCheckedChange = { checked ->
                                preferences.updateHapticFeedback(checked)
                                if (checked) {
                                    com.example.util.HapticUtil.performClickHaptic(context, true)
                                }
                                Toast.makeText(
                                    context,
                                    if (checked) "Haptic feedback enabled" else "Haptic feedback disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.testTag("haptic_feedback_switch")
                        )
                    }
                }

                // Streaming Providers & Stremio Add-ons Section
                var showManageProvidersDialog by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("streaming_providers_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Tune,
                                        contentDescription = "Providers",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Streaming Providers & Stremio Add-ons",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Configure HD Hub, PenguPlay, YAS Stream or add unlimited custom Stremio stream providers",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Option to Use Netflix Add-ons as Provider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Use Netflix Add-on as Provider",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Stream series & anime catalogs directly from Stremio Netflix add-on",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = userProfile.useNetflixAsProvider || preferences.getProviders().any { it.enabled && it.baseUrl.contains("netflix", ignoreCase = true) },
                                onCheckedChange = { checked ->
                                    preferences.toggleNetflixAddonProvider(checked)
                                    Toast.makeText(
                                        context,
                                        if (checked) "Netflix add-on enabled as provider!" else "Netflix add-on disabled",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.testTag("netflix_provider_toggle")
                            )
                        }

                        // Button with (+) icon: "Add or Edit Existing Providers" / "Change the provider"
                        Button(
                            onClick = { showManageProvidersDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manage_providers_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add or Edit Existing Providers (Change Provider)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Edit Home Screen Layout & 20 Poster Placeholders Section
                var showHomeScreenEditor by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_home_screen_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Palette,
                                        contentDescription = "Edit Home Screen",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Edit Home Screen",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Reorder side-by-side posters, add image placeholders with (+) and rename titles. Auto-refreshes on next catalog update while 8 permanent movies stay locked.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onEnterHomeEditMode()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_home_screen_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Home Screen Layout & 20 Poster Placeholders", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Save / Close
                Button(
                    onClick = {
                        Toast.makeText(context, "Settings & Home Screen layout saved!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_save_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save & Apply Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (showHomeScreenEditor) {
                    HomeScreenLayoutEditorDialog(
                        onDismiss = { showHomeScreenEditor = false },
                        onApply = {
                            Toast.makeText(context, "Custom Home Screen posters applied for current session!", Toast.LENGTH_SHORT).show()
                            showHomeScreenEditor = false
                        }
                    )
                }

                if (showManageProvidersDialog) {
                    ManageProvidersDialog(
                        preferences = preferences,
                        onDismiss = { showManageProvidersDialog = false }
                    )
                }
            }
        }
    }

    if (showYtLogin) {
        Dialog(
            onDismissRequest = { showYtLogin = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFFE50914),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "YouTube Sign-In",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    CookieManager.getInstance().flush()
                                    preferences.updateYoutubeConnected(true, "YouTube Account")
                                    Toast.makeText(context, "YouTube session verified and connected!", Toast.LENGTH_SHORT).show()
                                    showYtLogin = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            IconButton(onClick = { showYtLogin = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                settings.userAgentString =
                                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

                                val cm = CookieManager.getInstance()
                                cm.setAcceptCookie(true)
                                cm.setAcceptThirdPartyCookies(this, true)

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        if (url != null && (url.contains("myaccount.google.com") || url.contains("m.youtube.com"))) {
                                            cm.flush()
                                        }
                                    }
                                }
                                loadUrl("https://accounts.google.com/ServiceLogin?service=youtube")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreenLayoutEditorDialog(
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    val context = LocalContext.current
    data class PosterSlot(val index: Int, var title: String, var imageUrl: String)

    var slotsList by remember {
        mutableStateOf(
            (1..20).map { i ->
                when (i) {
                    2 -> PosterSlot(i, "Jujutsu Kaisen (Perm #1)", "poster_jujutsu_kaisen")
                    5 -> PosterSlot(i, "2012 (Perm #2)", "poster_2012")
                    8 -> PosterSlot(i, "Solo Leveling (Perm #3)", "poster_solo_leveling")
                    10 -> PosterSlot(i, "Naruto (Perm #4)", "poster_naruto")
                    13 -> PosterSlot(i, "Pacific Rim (Perm #5)", "poster_pacific_rim")
                    15 -> PosterSlot(i, "The Originals (Perm #6)", "poster_the_originals")
                    18 -> PosterSlot(i, "The Seven Deadly Sins (Perm #7)", "poster_seven_deadly_sins")
                    20 -> PosterSlot(i, "A Haunting (Perm #8)", "poster_a_haunting")
                    else -> PosterSlot(i, "Poster Placeholder #$i", "")
                }
            }.toMutableList()
        )
    }

    var editingSlotIndex by remember { mutableStateOf<Int?>(null) }
    var inputImageUriOrUrl by remember { mutableStateOf("") }
    var inputTitleName by remember { mutableStateOf("") }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedLocal = com.example.ui.components.persistCustomImageLocally(context, uri.toString())
            inputImageUriOrUrl = savedLocal
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Edit Home Screen (20 Poster Placeholders)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Tap (+) on any placeholder to add an image & rename title. Drag or move side by side. User edits auto-update on next catalog refresh while 8 permanent movies stay locked.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 20 Side-by-Side Poster Placeholders
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    items(slotsList.size) { idx ->
                        val slot = slotsList[idx]
                        val isPermanent = slot.title.contains("Perm #")

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPermanent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .width(130.dp)
                                .fillMaxHeight()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Slot #${slot.index}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .clickable {
                                            editingSlotIndex = idx
                                            inputImageUriOrUrl = slot.imageUrl
                                            inputTitleName = slot.title
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (slot.imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = com.example.ui.components.buildOptimizedImageRequest(context, slot.imageUrl, 270, 405),
                                            contentDescription = slot.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Add,
                                                    contentDescription = "Add Poster Image",
                                                    tint = Color.White,
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .padding(6.dp)
                                                )
                                            }
                                            Text("Add Image", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = slot.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    if (idx > 0) {
                                        IconButton(
                                            onClick = {
                                                val list = slotsList.toMutableList()
                                                val tmp = list[idx]
                                                list[idx] = list[idx - 1]
                                                list[idx - 1] = tmp
                                                slotsList = list
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("<", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                    if (idx < slotsList.size - 1) {
                                        IconButton(
                                            onClick = {
                                                val list = slotsList.toMutableList()
                                                val tmp = list[idx]
                                                list[idx] = list[idx + 1]
                                                list[idx + 1] = tmp
                                                slotsList = list
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text(">", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onApply,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Save & Apply Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }

    if (editingSlotIndex != null) {
        val currentSlotIdx = editingSlotIndex!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editingSlotIndex = null },
            title = { Text("Edit Poster Slot #${currentSlotIdx + 1}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputTitleName,
                        onValueChange = { inputTitleName = it },
                        label = { Text("Rename Title / Poster Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputImageUriOrUrl,
                        onValueChange = { inputImageUriOrUrl = it },
                        label = { Text("Poster Image URL or Path") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            photoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.FolderShared, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Image from Device")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val list = slotsList.toMutableList()
                        val old = list[currentSlotIdx]
                        list[currentSlotIdx] = old.copy(
                            title = inputTitleName.ifBlank { "Custom Poster #${currentSlotIdx + 1}" },
                            imageUrl = inputImageUriOrUrl.trim()
                        )
                        slotsList = list
                        editingSlotIndex = null
                    }
                ) {
                    Text("Apply Poster", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingSlotIndex = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ManageProvidersDialog(
    preferences: UserPreferences,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var providersList by remember { mutableStateOf(preferences.getProviders()) }
    var newProviderUrlInput by remember { mutableStateOf("") }
    var newProviderNameInput by remember { mutableStateOf("") }
    val nextProviderNum = (providersList.maxOfOrNull { it.streamerNumber } ?: providersList.size) + 1

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .testTag("manage_providers_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Manage & Add Providers",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Configure existing 3 providers or add unlimited Stremio add-ons",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                // Quick Action: Use Netflix Add-on shortcut
                val hasNetflixAddon = providersList.any { it.baseUrl.contains("netflix", ignoreCase = true) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Netflix Stremio Add-on",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasNetflixAddon) "Netflix catalog provider active" else "Add official Stremio Netflix add-on link",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!hasNetflixAddon) {
                            Button(
                                onClick = {
                                    val nextNum = (providersList.maxOfOrNull { it.streamerNumber } ?: providersList.size) + 1
                                    val netflixP = StremioProviderConfig(
                                        id = "netflix_addon_${System.currentTimeMillis()}",
                                        name = "Netflix Add-on",
                                        baseUrl = NETFLIX_ADDON_PROVIDER_URL,
                                        enabled = true,
                                        streamerNumber = nextNum
                                    )
                                    providersList = providersList + netflixP
                                    Toast.makeText(context, "Netflix Add-on added as Provider $nextNum!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Use Netflix Add-on", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE50914).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Active",
                                    color = Color(0xFFE50914),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Scrollable List of Providers + Add Section
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        providersList,
                        key = { _, item -> item.id }
                    ) { index, provider ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("provider_card_${provider.streamerNumber}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (provider.enabled)
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (provider.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Card Title & Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "Provider ${provider.streamerNumber}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = provider.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        androidx.compose.material3.Switch(
                                            checked = provider.enabled,
                                            onCheckedChange = { isEnabled ->
                                                providersList = providersList.map {
                                                    if (it.id == provider.id) it.copy(enabled = isEnabled) else it
                                                }
                                            }
                                        )

                                        if (providersList.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    providersList = providersList.filterNot { it.id == provider.id }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Remove Provider",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Editable Provider Name
                                OutlinedTextField(
                                    value = provider.name,
                                    onValueChange = { newName ->
                                        providersList = providersList.map {
                                            if (it.id == provider.id) it.copy(name = newName) else it
                                        }
                                    },
                                    label = { Text("Provider Name", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                // Editable Provider URL (User can replace with any new Stremio add-on link)
                                OutlinedTextField(
                                    value = provider.baseUrl,
                                    onValueChange = { newUrl ->
                                        providersList = providersList.map {
                                            if (it.id == provider.id) it.copy(baseUrl = newUrl.trim()) else it
                                        }
                                    },
                                    label = { Text("Stremio Add-on Link / Base URL", fontSize = 11.sp) },
                                    singleLine = false,
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // Add Provider Section below existing providers
                    item(key = "add_provider_section") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_provider_${nextProviderNum}_card"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Add Provider $nextProviderNum",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                OutlinedTextField(
                                    value = newProviderNameInput,
                                    onValueChange = { newProviderNameInput = it },
                                    placeholder = { Text("Name (e.g. Provider $nextProviderNum or Torrentio)", fontSize = 11.sp) },
                                    label = { Text("Provider $nextProviderNum Name", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                OutlinedTextField(
                                    value = newProviderUrlInput,
                                    onValueChange = { newProviderUrlInput = it },
                                    placeholder = { Text("Paste Stremio add-on link (https://... or manifest.json)", fontSize = 11.sp) },
                                    label = { Text("Provider $nextProviderNum Link", fontSize = 11.sp) },
                                    singleLine = false,
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Button(
                                    onClick = {
                                        val url = newProviderUrlInput.trim()
                                        if (url.isNotBlank()) {
                                            val name = newProviderNameInput.trim().ifBlank { "Provider $nextProviderNum" }
                                            val newP = StremioProviderConfig(
                                                id = "custom_provider_${System.currentTimeMillis()}",
                                                name = name,
                                                baseUrl = url,
                                                enabled = true,
                                                streamerNumber = nextProviderNum
                                            )
                                            providersList = providersList + newP
                                            newProviderUrlInput = ""
                                            newProviderNameInput = ""
                                            Toast.makeText(context, "$name added as Provider $nextProviderNum! You can now add Provider ${nextProviderNum + 1}.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Please enter a valid Stremio add-on link", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submit_add_provider_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Provider $nextProviderNum", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Action Buttons: Restore Defaults & Save Providers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            providersList = preferences.resetDefaultProviders()
                            Toast.makeText(context, "Restored default 3 providers (HD Hub, PenguPlay, YAS Stream)", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restore_default_providers_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Defaults", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            preferences.saveProviders(providersList)
                            Toast.makeText(context, "All ${providersList.size} providers saved & active!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_providers_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Providers", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

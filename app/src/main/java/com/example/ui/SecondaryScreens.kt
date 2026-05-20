package com.example.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayerManager
import com.example.audio.Song
import com.example.ui.theme.NeonTheme
import com.example.ui.theme.ThemeState

// ==========================================
// PLAYLIST SCREEN (Organizes & scans songs)
// ==========================================
@Composable
fun PlaylistScreen(onTrackSelected: (Song) -> Unit) {
    val playerManager = remember { AudioPlayerManager.getInstance() }
    val colors = ThemeState.getColors()
    val context = LocalContext.current
    
    var selectedTab by remember { mutableStateOf(0) } // 0: ALL, 1: PLAYLISTS, 2: ARTISTS, 3: FOLDERS
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }

    // Standard Android Permission Launcher API
    val scanPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            playerManager.scanLocalMusic(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Core Category tab selection caps
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val tabs = listOf("ALL SONGS", "PLAYLISTS", "ARTISTS", "FOLDERS")
            tabs.forEachIndexed { index, title ->
                val isActive = selectedTab == index
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isActive) colors.neonAccent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedTab = index }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isActive) colors.neonAccent else Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // List Render Zone
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> { // ALL INDEXED SONGS
                    Column {
                        // Scan Local Storage Action Core Button
                        GlassBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable {
                                    permissionLauncher.launch(scanPermission)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (playerManager.isScanning) Icons.Default.RotateRight else Icons.Default.LibraryMusic,
                                    contentDescription = "Scan",
                                    tint = colors.neonAccent,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .then(
                                            if (playerManager.isScanning) {
                                                // Rotate indicator dynamically when indexing
                                                val inf = rememberInfiniteTransition("scandot")
                                                val rotation by inf.animateFloat(
                                                    initialValue = 0f,
                                                    targetValue = 360f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(1500, easing = LinearEasing),
                                                        repeatMode = RepeatMode.Restart
                                                    ),
                                                    label = "rotation"
                                                )
                                                Modifier.rotate(rotation)
                                            } else Modifier
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (playerManager.isScanning) "INDEXING FILES..." else "SCAN LOCAL STORAGE",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.neonAccent
                                )
                            }
                        }

                        // Lazy lists of glass song cards
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(playerManager.allSongs) { song ->
                                SongGlassCard(
                                    song = song,
                                    isActive = playerManager.currentSong?.id == song.id,
                                    onPlay = { onTrackSelected(song) },
                                    onAddToPlaylistClick = {
                                        playerManager.addSongToPlaylist("FAVORITES", song)
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> { // CUSTOM MANAGED PLAYLISTS
                    Column {
                        // Create Playlist caps button
                        GlassBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable { showNewPlaylistDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = colors.primaryColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CREAT NEW CYBER PLAYLIST",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryColor
                                )
                            }
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(playerManager.playlists) { playlist ->
                                GlassBox(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = playlist.name,
                                                    fontSize = 14.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "${playlist.songs.size} DIRECT CHANNELS",
                                                    fontSize = 10.sp,
                                                    color = colors.primaryColor.copy(alpha = 0.5f)
                                                )
                                            }
                                            
                                            Icon(
                                                imageVector = Icons.Default.QueueMusic,
                                                contentDescription = "pl",
                                                tint = colors.primaryColor
                                            )
                                        }
                                        
                                        if (playlist.songs.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                playlist.songs.take(3).forEach { song ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { onTrackSelected(song) }
                                                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(6.dp))
                                                            .padding(6.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = song.title,
                                                            fontSize = 11.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = Color.White.copy(alpha = 0.8f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Text(
                                                            text = "PLAY",
                                                            fontSize = 9.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = colors.neonAccent
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> { // ARTIST AGGREGATOR
                    val grouped = playerManager.allSongs.groupBy { it.artist }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        grouped.forEach { (artist, songs) ->
                            item {
                                GlassBox(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                            .clickable { onTrackSelected(songs[0]) },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = artist,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${songs.size} DIGITAL CHUNKS INDEXED",
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                        Icon(Icons.Default.Person, contentDescription = "Art", tint = colors.neonAccent)
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> { // DIRECTORIES/FOLDERS INDEX
                    val folders = playerManager.allSongs.groupBy { song ->
                        if (song.isSynth) "VIRTUAL CORES"
                        else {
                            val fStr = song.path.substringBeforeLast("/")
                            fStr.substringAfterLast("/")
                        }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        folders.forEach { (folderName, songs) ->
                            item {
                                GlassBox(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                            .clickable { onTrackSelected(songs[0]) },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = folderName.uppercase(),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${songs.size} CHNL FL",
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                        Icon(Icons.Default.Folder, contentDescription = "Fold", tint = colors.secondaryColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Holographic Playlist creation prompt dialog
    if (showNewPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showNewPlaylistDialog = false },
            containerColor = colors.glassBg.copy(alpha = 0.95f),
            title = {
                Text(
                    text = "INITIATE DIRECT PL NODE",
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                OutlinedTextField(
                    value = playlistNameInput,
                    onValueChange = { playlistNameInput = it },
                    textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
                    placeholder = { Text("Caps name...", color = Color.White.copy(alpha = 0.3f), fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.neonAccent,
                        unfocusedBorderColor = colors.glassBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = colors.neonAccent),
                    onClick = {
                        if (playlistNameInput.isNotBlank()) {
                            playerManager.createPlaylist(playlistNameInput.uppercase())
                            playlistNameInput = ""
                            showNewPlaylistDialog = false
                        }
                    }
                ) {
                    Text("CONNECT LINK", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNewPlaylistDialog = false }
                ) {
                    Text("ABORT", color = Color.White.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace)
                }
            },
            modifier = Modifier.border(1.dp, colors.glassBorder, RoundedCornerShape(28.dp))
        )
    }
}

// Subordinate Card component for Songs
@Composable
fun SongGlassCard(
    song: Song,
    isActive: Boolean,
    onPlay: () -> Unit,
    onAddToPlaylistClick: () -> Unit
) {
    val colors = ThemeState.getColors()
    
    GlassBox(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        borderWidth = if (isActive) 1.5.dp else 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tiny animated dynamic neon cover profile blocks
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(song.coverColorStart), Color(song.coverColorEnd))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // If currentSong is active, show small glowing beat pulses
                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Pulsing",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (song.isSynth) Icons.Default.PowerSettingsNew else Icons.Default.MusicNote,
                        contentDescription = "Acoustic",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) colors.neonAccent else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick add to favorites icon
            IconButton(onClick = onAddToPlaylistClick) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "AddFav",
                    tint = if (isActive) colors.neonAccent.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = formatDuration(song.duration),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%d:%02d", min, sec)
}

// ==========================================
// EQUALIZER SCREEN (Futuristic frequencies)
// ==========================================
@Composable
fun EqualizerScreen() {
    val playerManager = remember { AudioPlayerManager.getInstance() }
    val colors = ThemeState.getColors()
    
    val bands = playerManager.eqBands
    // High-tech frequency definitions labels
    val hzLabels = listOf("60Hz", "150Hz", "400Hz", "1kHz", "3kHz", "6kHz", "12kHz", "16kHz")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "HOLOGRAPHIC EQ COUPLING",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = colors.primaryColor.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "REAL-TIME RESONANCE MATRIX",
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }

        // Animated neon curve spectrum connector waves
        GlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(vertical = 12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pointCount = bands.size
                val stepX = size.width / (pointCount - 1)
                
                val path = Path()
                val fillPath = Path()
                
                // Construct beautiful spline path based on EQ sliders levels
                for (i in 0 until pointCount) {
                    val x = i * stepX
                    // Re-orient levels from 0..100 onto canvas heights
                    val value = bands[i] / 100f
                    val y = size.height - (value * (size.height * 0.7f) + (size.height * 0.15f))
                    
                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, size.height)
                        fillPath.lineTo(x, y)
                    } else {
                        // Smooth cubic bezier curves for high fidelity graphics
                        val prevX = (i - 1) * stepX
                        val prevVal = bands[i - 1] / 100f
                        val prevY = size.height - (prevVal * (size.height * 0.7f) + (size.height * 0.15f))
                        
                        path.cubicTo(
                            prevX + stepX / 2f, prevY,
                            x - stepX / 2f, y,
                            x, y
                        )
                        fillPath.cubicTo(
                            prevX + stepX / 2f, prevY,
                            x - stepX / 2f, y,
                            x, y
                        )
                    }
                    
                    if (i == pointCount - 1) {
                        fillPath.lineTo(x, size.height)
                        fillPath.close()
                    }
                }

                // Fill glowing area under spline
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(colors.neonAccent.copy(alpha = 0.25f), Color.Transparent)
                    )
                )

                // Write outline spline line
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        colors = listOf(colors.neonAccent, colors.secondaryColor)
                    ),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw pulsing index frequency node markers on the line
                for (i in 0 until pointCount) {
                    val x = i * stepX
                    val value = bands[i] / 100f
                    val y = size.height - (value * (size.height * 0.7f) + (size.height * 0.15f))
                    
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = colors.neonAccent,
                        radius = 8f,
                        center = Offset(x, y),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }

        // Grid set of sliders
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bands.forEachIndexed { index, level ->
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "+${level - 50}dB",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.primaryColor.copy(alpha = 0.7f)
                    )

                    // Vertical Slider represented by standard pointer input dragging on Box
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .weight(1f)
                            .padding(vertical = 8.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .border(0.5.dp, colors.glassBorder.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val h = constraints.maxHeight.toFloat()
                            val frac = level / 100f
                            
                            // Active glowing line block
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(frac)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(colors.neonAccent, colors.secondaryColor)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            )
                            
                            // Sliding node indicator
                            val sliderOffset = h * (1f - frac) - 8.dp.value
                            Box(
                                modifier = Modifier
                                    .offset(y = sliderOffset.dp)
                                    .size(16.dp, 16.dp)
                                    .align(Alignment.TopCenter)
                                    .background(Color.White, shape = CircleShape)
                                    .border(2.dp, colors.neonAccent, shape = CircleShape)
                            )
                            
                            // Simple click and drag interaction zone helper
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val nextVal = ((h - change.position.y) / h * 100).toInt()
                                            playerManager.setBandLevel(index, nextVal.coerceIn(0, 100))
                                        }
                                    }
                            )
                        }
                    }

                    Text(
                        text = hzLabels[index],
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ==========================================
// SEARCH SCREEN (Filtering query interface)
// ==========================================
@Composable
fun SearchScreen(onTrackSelected: (Song) -> Unit) {
    val playerManager = remember { AudioPlayerManager.getInstance() }
    val colors = ThemeState.getColors()
    
    var query by remember { mutableStateOf("") }
    
    val filteredSongs = playerManager.allSongs.filter {
        it.title.contains(query, ignoreCase = true) ||
        it.artist.contains(query, ignoreCase = true) ||
        it.album.contains(query, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "HOLOGRAPHIC SEARCH ROUTER",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = colors.primaryColor.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Glass Input text bar
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                placeholder = { Text("Search title, artist or album...", color = Color.White.copy(alpha = 0.3f), fontFamily = FontFamily.Monospace) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = colors.neonAccent) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.5f))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.neonAccent,
                    unfocusedBorderColor = colors.glassBorder
                ),
                shape = RoundedCornerShape(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Animated results entrance
        Box(modifier = Modifier.weight(1f)) {
            if (filteredSongs.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "Void",
                        tint = colors.primaryColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "NO COMPATIBLE CHANNELS LOCATED",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSongs) { song ->
                        SongGlassCard(
                            song = song,
                            isActive = playerManager.currentSong?.id == song.id,
                            onPlay = { onTrackSelected(song) },
                            onAddToPlaylistClick = {
                                playerManager.addSongToPlaylist("FAVORITES", song)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SETTINGS SCREEN (Neon customizations)
// ==========================================
@Composable
fun SettingsScreen() {
    val colors = ThemeState.getColors()
    val playerManager = remember { AudioPlayerManager.getInstance() }
    
    val currentTheme = ThemeState.activeTheme
    
    val themeList = listOf(
        Triple(NeonTheme.CYBERPUNK_RESET, "CYBERPUNK 2077", "RAZOR PINK & SHIELD PURPLE"),
        Triple(NeonTheme.GLASS_VISION_PRO, "MISTY VISION", "AERO BLUE & SILICON GLASS"),
        Triple(NeonTheme.TOXIC_INDUSTRIAL, "TOXIC WASTE", "ACID GREEN & MUTATED GLOW"),
        Triple(NeonTheme.QUANTUM_ORANGE, "SOLAR STORM", "MOLTEN ORANGE & SOLAR LAVA")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "SYSTEM ENGINE CONTEXT",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = colors.primaryColor.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "NEON CHROMATIC PANEL",
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Choose Theme options
            themeList.forEach { (themeEnum, name, colorText) ->
                val isSelected = currentTheme == themeEnum
                
                GlassBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { ThemeState.activeTheme = themeEnum },
                    borderWidth = if (isSelected) 1.5.dp else 0.5.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = name,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) colors.neonAccent else Color.White
                            )
                            Text(
                                text = colorText,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { ThemeState.activeTheme = themeEnum },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.neonAccent,
                                unselectedColor = colors.glassBorder
                            )
                        )
                    }
                }
            }
        }

        // Meta profile system card
        GlassBox(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "METADATA TELEMETRY CORE",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryColor.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL TRACK_NODES ACTIVE", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace)
                    Text("${playerManager.allSongs.size} CORE", fontSize = 10.sp, color = colors.neonAccent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("PLAYBACK BUFFER QUALITY", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace)
                    Text("16BIT PCM STEREO / 22.05kHz", fontSize = 10.sp, color = colors.primaryColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("AUDIO SYSTEM MODULES", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace)
                    Text("SYNTH WAVE SYNTHESIZER", fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f), fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// ==========================================
// MINI-PLAYER FLOATING COMPOSABLE DOCK PANEL
// ==========================================
@Composable
fun MiniPlayer(onExpand: () -> Unit) {
    val playerManager = remember { AudioPlayerManager.getInstance() }
    val colors = ThemeState.getColors()

    val currentSong = playerManager.currentSong ?: return
    val isPlaying = playerManager.isPlaying

    GlassBox(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onExpand() },
        cornerRadius = 14.dp,
        borderWidth = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Rotating Cover miniaturized
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(currentSong.coverColorStart), Color(currentSong.coverColorEnd))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = "Note", tint = Color.White, modifier = Modifier.size(14.dp))
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Metadata summary
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong.title,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong.artist,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    color = colors.neonAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick Playback Key Deck
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { playerManager.skipPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                
                IconButton(
                    onClick = { playerManager.togglePlayback() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.neonAccent.copy(alpha = 0.2f), CircleShape)
                        .border(0.5.dp, colors.neonAccent.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Pos",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = { playerManager.skipNext() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

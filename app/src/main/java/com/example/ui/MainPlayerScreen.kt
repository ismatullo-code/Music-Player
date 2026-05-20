package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayerManager
import com.example.ui.theme.ThemeState
import kotlin.math.abs

@Composable
fun MainPlayerScreen(modifier: Modifier = Modifier) {
    val playerManager = remember { AudioPlayerManager.getInstance() }
    val colors = ThemeState.getColors()
    
    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying
    val progress = playerManager.currentProgress
    val volume = playerManager.volume

    // Cover rotation transition (runs only when song is actively playing)
    val infiniteTransition = rememberInfiniteTransition(label = "disc_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val activeRotation = if (isPlaying) rotationAngle else 0f

    // Dynamic color glowing background matching the album cover profile
    val coverColorStart = currentSong?.coverColorStart ?: 0xFF7B00FF
    val coverColorEnd = currentSong?.coverColorEnd ?: 0xFF00FFCC
    
    // Swipe Gestures Tracking state
    var dragOffsetX by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        
        // 1. Header / Playlist Indicator info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ACTIVE PRESET NODE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryColor.copy(alpha = 0.5f)
                )
                Text(
                    text = currentSong?.album ?: "NO COMPILATION DETECTED",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Graphic",
                tint = colors.neonAccent,
                modifier = Modifier.size(20.dp)
            )
        }

        // 2. Large Rotating Album Artwork + Gesture Detector + Ambient cover glow
        Box(
            modifier = Modifier
                .size(240.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (dragOffsetX > 100f) {
                                playerManager.skipPrevious() // Swipe Right -> Prev Track
                            } else if (dragOffsetX < -100f) {
                                playerManager.skipNext()     // Swipe Left -> Next Track
                            }
                            dragOffsetX = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetX += dragAmount.x
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Dynamic neon-aurora under-shadow (ambient lighting based on album cover colors)
            Canvas(modifier = Modifier.size(260.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(coverColorStart).copy(alpha = 0.35f),
                            Color(coverColorEnd).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
            }

            // Outer Vinyl mechanical-groove circular deck
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .border(
                        width = 4.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                colors.primaryColor.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.8f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .background(Color(0xFF0D0B14), shape = CircleShape)
            )

            // Inner Rotating album disc with modern glass vinyl rings
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .rotate(activeRotation)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(coverColorStart),
                                Color(coverColorEnd),
                                Color(0xFF040209)
                            )
                        )
                    )
            ) {
                // Vinyl sound waves grooves lines drawn over it
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (i in 1..4) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = size.width * (0.1f * i),
                            style = Stroke(width = 1f)
                        )
                    }
                    
                    // High-tech holographic laser vector ticks
                    for (angle in 0..360 step 45) {
                        val angleRad = Math.toRadians(angle.toDouble())
                        val startLen = size.width * 0.42f
                        val endLen = size.width * 0.48f
                        drawLine(
                            color = Color.White.copy(alpha = 0.18f),
                            start = Offset(
                                (size.width / 2 + Math.cos(angleRad) * startLen).toFloat(),
                                (size.height / 2 + Math.sin(angleRad) * startLen).toFloat()
                            ),
                            end = Offset(
                                (size.width / 2 + Math.cos(angleRad) * endLen).toFloat(),
                                (size.height / 2 + Math.sin(angleRad) * endLen).toFloat()
                            ),
                            strokeWidth = 2f
                        )
                    }

                    // Shiny specular light beam reflections over rotating disc
                    drawArc(
                        color = Color.White.copy(alpha = 0.1f),
                        startAngle = 45f,
                        sweepAngle = 30f,
                        useCenter = true,
                        size = size,
                        topLeft = Offset.Zero
                    )
                    drawArc(
                        color = Color.White.copy(alpha = 0.1F),
                        startAngle = 225f,
                        sweepAngle = 30f,
                        useCenter = true,
                        size = size,
                        topLeft = Offset.Zero
                    )
                }

                // Central high-tech spindle node
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center)
                        .background(Color.Black, shape = CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), shape = CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.Center)
                            .background(colors.neonAccent, shape = CircleShape)
                    )
                }
            }
        }

        // 3. Real-Time Interactive Music Visualizer (Bouncing Neon Columns)
        val spectrumData = playerManager.getVisualizerSpectrum()
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
        ) {
            val barWidth = (size.width / 16) - 4f
            val maxBarHeight = size.height
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(coverColorStart),
                    Color(coverColorEnd).copy(alpha = 0.6f),
                    Color.Transparent
                )
            )

            // Draw symmetrical dual-frequency bands for futuristic look
            for (i in 0 until 16) {
                val heightPercent = spectrumData[i]
                val barHeight = (heightPercent * maxBarHeight).coerceAtLeast(3f)
                val xPos = i * (size.width / 16) + 2f
                val yPos = size.height - barHeight

                // Background glow underlay
                drawRoundRect(
                    color = Color(coverColorEnd).copy(alpha = 0.22f),
                    topLeft = Offset(xPos - 1f, yPos - 1f),
                    size = Size(barWidth + 2f, barHeight + 2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )

                // Foreground neon gradient column
                drawRoundRect(
                    brush = gradientBrush,
                    topLeft = Offset(xPos, yPos),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )

                // Drawing sparkling neon tip dot (liquid energy visualizer bar peak)
                if (barHeight > 4f) {
                    drawCircle(
                        color = Color.White,
                        radius = 2f,
                        center = Offset(xPos + barWidth / 2f, yPos + 1.5f)
                    )
                    drawCircle(
                        color = Color(coverColorStart),
                        radius = 4.5f,
                        center = Offset(xPos + barWidth / 2f, yPos + 1.5f),
                        style = Stroke(width = 1f)
                    )
                }
            }
        }

        // 4. Metadata Details (Song Title, Artist, Core Type info)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentSong?.title ?: "SYSTEM IDLE",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = currentSong?.artist ?: "SELECT SUB-SECTOR AUDIO DIRECTORY",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = colors.neonAccent,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 5. Progress Indicator Bar with time text margins
        Column(modifier = Modifier.fillMaxWidth()) {
            val progressPercent = if (currentSong != null && currentSong.duration > 0) {
                progress.toFloat() / currentSong.duration
            } else 0f

            CyberSlider(
                value = progressPercent,
                onValueChange = { frac ->
                    if (currentSong != null) {
                        playerManager.seekTo((frac * currentSong.duration).toLong())
                    }
                },
                activeColorStart = Color(coverColorStart),
                activeColorEnd = Color(coverColorEnd)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatMs(progress),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = formatMs(currentSong?.duration ?: 0L),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // 6. Holographic Media Key Control Deck
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { playerManager.setVolumeLevel((volume - 0.15f).coerceAtLeast(0f)) }
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeDown,
                    contentDescription = "Vol Down",
                    tint = colors.primaryColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Skip Prev Command Button
            NeonButton(
                onClick = { playerManager.skipPrevious() },
                modifier = Modifier.size(52.dp),
                glowColor = Color(coverColorStart)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Prev",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Big Center Action Play Toggle Button (Pulsing neon ring)
            val pulseTransition = rememberInfiniteTransition(label = "play_pulse")
            val pulseFactor by pulseTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )
            val centerGlowFactor = if (isPlaying) pulseFactor else 1.0f

            NeonButton(
                onClick = { playerManager.togglePlayback() },
                modifier = Modifier
                    .size(72.dp)
                    .rotate(if (isPlaying) 0f else 0f),
                glowColor = Color(coverColorEnd)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
                
                // Pulsing light outline ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(coverColorEnd).copy(alpha = 0.15f * (2.0f - centerGlowFactor)),
                        radius = (size.width / 2f) * centerGlowFactor,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // Skip Next Command Button
            NeonButton(
                onClick = { playerManager.skipNext() },
                modifier = Modifier.size(52.dp),
                glowColor = Color(coverColorStart)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = { playerManager.setVolumeLevel((volume + 0.15f).coerceIn(0f, 1f)) }
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Vol Up",
                    tint = colors.primaryColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 7. Mini Glass inline Master Volume Indicator Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VolumeMute,
                contentDescription = "Mute",
                tint = colors.primaryColor.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(10.dp))
            
            CyberSlider(
                value = volume,
                onValueChange = { playerManager.setVolumeLevel(it) },
                modifier = Modifier.weight(1f),
                barHeight = 4.dp,
                activeColorStart = colors.neonAccent.copy(alpha = 0.8f),
                activeColorEnd = colors.primaryColor.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Max",
                tint = colors.primaryColor.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Global Millisecond Formatter helper
private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%d:%02d", min, sec)
}

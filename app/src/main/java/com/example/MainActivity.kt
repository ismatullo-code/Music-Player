package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayerManager
import com.example.ui.*
import com.example.ui.theme.ThemeState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var activeScreenIndex by remember { mutableStateOf(0) }
            val colors = ThemeState.getColors()

            Box(modifier = Modifier.fillMaxSize()) {
                // Static space background with drifting stellar light particles
                ParticleBackground()

                // Layout framing
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    
                    // 1. Sleek Cybernetic Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeonTitle(text = "CYBER.DECK-UX")
                        
                        GlassBox(
                            cornerRadius = 8.dp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp).clickable {
                                // Manual quick loop back to primary deck
                                activeScreenIndex = 0
                            }
                        ) {
                            Text(
                                text = when (activeScreenIndex) {
                                    0 -> "STEREO TERMINAL"
                                    1 -> "INDEX FILES"
                                    2 -> "EQ COUPLING"
                                    3 -> "SEARCH ROUTE"
                                    else -> "CONFIG CONTEXT"
                                },
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = colors.neonAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // 2. Animated Core Screen Viewport (120fps fluid page transitions)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = activeScreenIndex,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                            },
                            label = "viewport_slide"
                        ) { index ->
                            when (index) {
                                0 -> MainPlayerScreen()
                                1 -> PlaylistScreen { selectedTrack ->
                                    AudioPlayerManager.getInstance().play(selectedTrack)
                                    activeScreenIndex = 0 // Expand playing track to fullscreen
                                }
                                2 -> EqualizerScreen()
                                3 -> SearchScreen { selectedTrack ->
                                    AudioPlayerManager.getInstance().play(selectedTrack)
                                    activeScreenIndex = 0 // Expand to fullscreen
                                }
                                else -> SettingsScreen()
                            }
                        }
                    }

                    // 3. Bottom Dock Decking (MiniPlayer + Glassmorphic Tab Selector)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Floating MiniPlayer handles operations when browsing secondary panels
                        if (activeScreenIndex != 0) {
                            MiniPlayer(onExpand = { activeScreenIndex = 0 })
                        }

                        // Glass Tab Selector
                        GlassBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val tabs = listOf(
                                    Triple(0, Icons.Default.MusicNote, "TERMINAL"),
                                    Triple(1, Icons.Default.QueueMusic, "INDEX"),
                                    Triple(2, Icons.Default.Equalizer, "MATRIX"),
                                    Triple(3, Icons.Default.Search, "ROUTER"),
                                    Triple(4, Icons.Default.Settings, "CONFIG")
                                )

                                tabs.forEach { (idx, icon, label) ->
                                    val isSelected = activeScreenIndex == idx
                                    
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { activeScreenIndex = idx }
                                            .padding(vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (isSelected) colors.neonAccent else Color.White.copy(alpha = 0.35f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = label,
                                            fontSize = 7.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) colors.neonAccent else Color.White.copy(alpha = 0.35f)
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

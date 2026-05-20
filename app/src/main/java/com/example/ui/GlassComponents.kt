package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ThemeState
import kotlin.random.Random
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

// Reusable Realistic Glass Block Container with specular border highlights
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = ThemeState.getColors()
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.glassBg,
                        colors.glassBg.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        colors.primaryColor.copy(alpha = 0.12f),
                        colors.secondaryColor.copy(alpha = 0.06f),
                        Color.Black.copy(alpha = 0.35f)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius)),
        content = content
    )
}

// Drifting Floating Particles Background inside Web-style Android canvas frame
@Composable
fun ParticleBackground(modifier: Modifier = Modifier) {
    val colors = ThemeState.getColors()
    
    // Core animation tick for drifting particles without taking major resources
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    // Seed randomized custom coordinates in-memory once to keep rendering stable
    val particleCount = 25
    val particles = remember {
        List(particleCount) {
            ParticleInfo(
                xSeed = Random.nextFloat(),
                ySeed = Random.nextFloat(),
                size = Random.nextFloat() * 5f + 3f,
                speedX = Random.nextFloat() * 0.12f - 0.06f,
                speedY = Random.nextFloat() * -0.15f - 0.03f, // General upwards drift
                baseAlpha = Random.nextFloat() * 0.4f + 0.15f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize().background(colors.screenBackground)) {
        val width = size.width
        val height = size.height

        // Draw deep ambient radial glowing core blobs
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(colors.neonGlow.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(width * 0.2f, height * 0.25f),
                radius = width * 1.5f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(colors.primaryColor.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(width * 0.8f, height * 0.7f),
                radius = width * 1.3f
            )
        )

        // Render drifting cyber-lights
        particles.forEach { particle ->
            // Update offset based on animated tick
            val offsetX = (particle.xSeed * width + particle.speedX * animTime * width * 0.1f) % width
            // Drift upward, cycle back to screen bottom
            val currentY = (particle.ySeed * height + particle.speedY * animTime * height * 0.2f) % height
            val offsetY = if (currentY < 0) height + currentY else currentY

            val flicker = Math.sin(animTime * 0.5 + particle.xSeed * 100.0).toFloat() * 0.1f
            val alpha = (particle.baseAlpha + flicker).coerceIn(0.05f, 0.7f)

            drawCircle(
                color = colors.neonAccent.copy(alpha = alpha),
                radius = particle.size,
                center = Offset(offsetX, offsetY)
            )

            // Dynamic light glow halo around larger particles
            if (particle.size > 6f) {
                drawCircle(
                    color = colors.neonAccent.copy(alpha = alpha * 0.3f),
                    radius = particle.size * 2.5f,
                    center = Offset(offsetX, offsetY)
                )
            }
        }
    }
}

private data class ParticleInfo(
    val xSeed: Float,
    val ySeed: Float,
    val size: Float,
    val speedX: Float,
    val speedY: Float,
    val baseAlpha: Float
)

// High-tech Glowing Holographic Button
@Composable
fun NeonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = ThemeState.getColors().neonAccent,
    isEnabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = ThemeState.getColors()
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .drawBehind {
                // Drop neon glow behind the key
                drawRoundRect(
                    color = glowColor.copy(alpha = 0.25f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(90f, 90f)
                )
                // Thick neon accent halo
                drawRoundRect(
                    color = glowColor.copy(alpha = 0.08f),
                    topLeft = Offset(-10f, -10f),
                    size = Size(size.width + 20f, size.height + 20f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(100f, 100f)
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        glowColor,
                        colors.glassBorder,
                        glowColor.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(50)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.glassBg.copy(alpha = 0.5f),
                        Color.Black.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(50)
            )
            .clip(RoundedCornerShape(50))
            .clickable(
                interactionSource = interactionSource,
                indication = null, // No standard ripple, use scale press or custom glows for futuristic UI
                onClick = { if (isEnabled) onClick() }
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

// Glowing Header Typography
@Composable
fun NeonTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 24,
    color: Color = ThemeState.getColors().primaryColor,
    glowColor: Color = ThemeState.getColors().secondaryColor
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Drop neon shadow offset for holographic chromatic aberration effect
        Text(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = glowColor.copy(alpha = 0.35f),
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(x = (-1.5).dp, y = 1.dp)
        )
        Text(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

// Custom Glass Sliders (Volume & Equalizers) with Liquid Neon Tip Controls
@Composable
fun CyberSlider(
    value: Float, // 0f to 1f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 8.dp,
    activeColorStart: Color = ThemeState.getColors().neonAccent,
    activeColorEnd: Color = ThemeState.getColors().secondaryColor
) {
    val colors = ThemeState.getColors()
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Drag handles clicking safely */ },
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val maxW = maxWidth
        
        // Touch Drag detection
        var widthFraction by remember { mutableStateOf(value) }
        widthFraction = value.coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(50)
                )
                .border(
                    width = 0.5.dp,
                    color = colors.glassBorder.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(50)
                )
                .drawBehind {
                    val activeWidth = totalWidth * widthFraction
                    
                    // Draw Liquid Glowing active track
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(activeColorStart, activeColorEnd)
                        ),
                        size = Size(activeWidth, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                    )
                    
                    // Neon Bloom underlay
                    if (activeWidth > 0f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(activeColorStart.copy(alpha = 0.35f), activeColorEnd.copy(alpha = 0.35f))
                            ),
                            topLeft = Offset(0f, -4f),
                            size = Size(activeWidth, size.height + 8f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(15f, 15f)
                        )
                    }
                    
                    // Specular white light accent line on top of active bar
                    if (activeWidth > 10f) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.45f),
                            start = Offset(4f, size.height * 0.25f),
                            end = Offset(activeWidth - 4f, size.height * 0.25f),
                            strokeWidth = 2f
                        )
                    }
                }
                .cyberPointerInput { x ->
                    val frac = (x / totalWidth).coerceIn(0f, 1f)
                    onValueChange(frac)
                }
        ) {
            // Sliding thumb feedback light
            val sliderThumbOffsetDp = (maxOf(0f, widthFraction) * maxW.value).dp - 8.dp
            Box(
                modifier = Modifier
                    .offset(x = sliderThumbOffsetDp)
                    .size(16.dp, 16.dp)
                    .align(Alignment.CenterStart)
                    .background(Color.White, shape = RoundedCornerShape(50))
                    .border(3.dp, activeColorStart, shape = RoundedCornerShape(50))
                    .drawBehind {
                        // Halo around sliding node knob
                        drawCircle(
                            color = activeColorStart.copy(alpha = 0.6f),
                            radius = size.width * 1.5f
                        )
                    }
            )
        }
    }
}

// Drag logic pointer input tracking helper (avoiding shading Compose framework functions)
private fun Modifier.cyberPointerInput(onXChange: (Float) -> Unit): Modifier {
    return this.then(
        Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, _ ->
                    change.consume()
                    onXChange(change.position.x)
                },
                onDragStart = { offset ->
                    onXChange(offset.x)
                }
            )
        }
    )
}

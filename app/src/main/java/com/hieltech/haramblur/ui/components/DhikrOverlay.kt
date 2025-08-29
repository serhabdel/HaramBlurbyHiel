package com.hieltech.haramblur.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hieltech.haramblur.data.Dhikr
import com.hieltech.haramblur.data.DhikrPosition
import com.hieltech.haramblur.data.DhikrSettings
import kotlinx.coroutines.delay

@Composable
fun DhikrOverlay(
    dhikr: Dhikr?,
    settings: DhikrSettings,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    if (dhikr == null) return
    
    // Auto-dismiss after display duration
    LaunchedEffect(dhikr) {
        delay(settings.displayDurationSeconds * 1000L)
        onDismiss()
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        DhikrCard(
            dhikr = dhikr,
            settings = settings,
            onDismiss = onDismiss,
            modifier = Modifier.align(getAlignmentFromPosition(settings.displayPosition))
        )
    }
}

@Composable
private fun DhikrCard(
    dhikr: Dhikr,
    settings: DhikrSettings,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation states
    val animatedVisibility = remember { mutableStateOf(false) }
    
    LaunchedEffect(dhikr) {
        animatedVisibility.value = true
    }
    
    // Position offset animation
    val offsetX by animateFloatAsState(
        targetValue = if (animatedVisibility.value) 0f else 300f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    // Fade animation
    val alpha by animateFloatAsState(
        targetValue = if (animatedVisibility.value) 1f else 0f,
        animationSpec = tween(500)
    )
    
    Card(
        modifier = modifier
            .padding(16.dp)
            .width(280.dp)
            .offset(x = if (isRightPosition(settings.displayPosition)) offsetX.dp else (-offsetX).dp)
            .graphicsLayer(alpha = alpha),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2E7D32).copy(alpha = 0.1f),
                            Color(0xFF1B5E20).copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with dhikr type
            Text(
                text = "${dhikr.time.displayName} Dhikr",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Arabic text
            Text(
                text = dhikr.arabicText,
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF1B5E20),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (settings.showTransliteration) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Transliteration
                Text(
                    text = dhikr.transliteration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF424242),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            if (settings.showTranslation) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // English translation
                Text(
                    text = dhikr.englishTranslation,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF616161),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Category tag
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2E7D32).copy(alpha = 0.1f),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = dhikr.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun getAlignmentFromPosition(position: DhikrPosition): Alignment {
    return when (position) {
        DhikrPosition.TOP_RIGHT -> Alignment.TopEnd
        DhikrPosition.TOP_LEFT -> Alignment.TopStart
        DhikrPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
        DhikrPosition.BOTTOM_LEFT -> Alignment.BottomStart
        DhikrPosition.CENTER -> Alignment.Center
    }
}

private fun isRightPosition(position: DhikrPosition): Boolean {
    return position == DhikrPosition.TOP_RIGHT || position == DhikrPosition.BOTTOM_RIGHT
}

@Composable
fun DhikrProgressIndicator(
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSeconds > 0) {
        (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()
    } else 0f
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = progress,
            color = Color(0xFF2E7D32),
            trackColor = Color(0xFF2E7D32).copy(alpha = 0.2f),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "${remainingSeconds}s",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF616161)
        )
    }
}
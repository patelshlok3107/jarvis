package com.shlok.jarvis.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shlok.jarvis.data.JarvisStatus

@Composable
fun JarvisCore(status: JarvisStatus, listening: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "core")
    val pulse by infinite.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label="pulse")
    val glowAlpha by infinite.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label="glow")

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(148.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val c = if (status==JarvisStatus.AVAILABLE) Color(0xFF00E676) else Color(0xFF00E5FF)
                drawCircle(Brush.radialGradient(listOf(c.copy(alpha=glowAlpha), Color.Transparent)), radius = size.minDimension/2 * pulse)
                drawCircle(c, style = Stroke(width = 2.dp.toPx()))
                drawCircle(c.copy(alpha=0.15f), radius = size.minDimension/2 * 0.72f)
                drawCircle(c, radius = size.minDimension/2 * 0.28f)
            }
            Text("◉", color = Color.White, fontSize = 28.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text("JARVIS", color = Color(0xFF00E5FF), fontWeight = FontWeight.Light, letterSpacing = 8.sp, fontSize = 18.sp)
        Spacer(Modifier.height(6.dp))
        val label = when(status){
            JarvisStatus.AVAILABLE -> "● AVAILABLE"
            JarvisStatus.BUSY -> "● BUSY MODE"
            JarvisStatus.DND -> "◐ DO NOT DISTURB"
            JarvisStatus.DRIVING -> "◑ DRIVING"
            JarvisStatus.SLEEPING -> "◒ SLEEPING"
            JarvisStatus.MEETING -> "◓ MEETING"
        }
        Text(label, color = if(status==JarvisStatus.AVAILABLE) Color(0xFF00E676) else Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 2.sp)
        Text(status.subtitle, color = Color.White.copy(alpha=0.55f), fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Text(if(listening) "Listening…" else "Listening: OFF", color = if(listening) Color(0xFFFF3B30) else Color.White.copy(0.35f), fontSize = 11.sp)
    }
}

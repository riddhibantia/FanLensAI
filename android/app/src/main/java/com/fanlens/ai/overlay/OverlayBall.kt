package com.fanlens.ai.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fanlens.ai.ui.MascotBall

/**
 * Mascot plus an in-overlay dismiss affordance. Hiding persists
 * `overlay_enabled = false`, so the ball stays hidden until re-enabled.
 */
@Composable
fun OverlayBall(
    animated: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MascotBall(sizeDp = 56, reactive = animated, onClick = onOpen)
        Text(
            text = "✕",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .padding(top = 2.dp)
                .clickable(onClick = onDismiss),
        )
    }
}

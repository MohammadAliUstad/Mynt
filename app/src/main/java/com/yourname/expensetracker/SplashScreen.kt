package com.yourname.expensetracker

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.delay

/**
 * Startup splash screen showing app name, icon, and tagline.
 * Displays with a gentle fade-in animation for ~2 seconds before moving to the main screen.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animatable alpha value for fade-in effect (0.0 = invisible, 1.0 = fully visible)
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Fade in smoothly over 1000 milliseconds
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
        // Hold splash screen for another 1000ms (total ~2 seconds)
        delay(1000)
        // Transition to main screen
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .alpha(alphaAnim.value) // Apply fade-in alpha
        ) {
            // App Logo Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💳",
                    fontSize = 44.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "ExpenseTracker",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Short Tagline
            Text(
                text = "Track smart, save more",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true, name = "Splash Screen Light")
@Composable
fun SplashScreenLightPreview() {
    ExpenseTrackerTheme(darkTheme = false) {
        SplashScreen(onSplashFinished = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Splash Screen Dark")
@Composable
fun SplashScreenDarkPreview() {
    ExpenseTrackerTheme(darkTheme = true) {
        SplashScreen(onSplashFinished = {})
    }
}

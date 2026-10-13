package com.fanlens.ai.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.sp
import com.fanlens.ai.MainActivity
import com.fanlens.ai.data.cache.CachedFreshness
import com.fanlens.ai.data.cache.CachedStanding
import com.fanlens.ai.di.ServiceLocator
import kotlinx.coroutines.flow.first
import com.fanlens.ai.data.prefs.CompanionMode

/**
 * Home-screen summary of the user's fantasy team. Works entirely from the
 * Room cache, so the widget renders even when the overlay permission is denied
 * or the phone is offline.
 */
class ScoreWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = ServiceLocator.settingsStore.settings.first()
        val matchId = settings.activeMatchId
        val cached: List<CachedStanding> = if (matchId.isNotBlank()) {
            runCatching {
                ServiceLocator.database.cacheDao().standings(matchId).first()
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val freshness: CachedFreshness? = if (matchId.isNotBlank()) {
            runCatching {
                ServiceLocator.database.cacheDao().freshness(matchId).first()
            }.getOrDefault(null)
        } else {
            null
        }
        val self = cached.firstOrNull { it.isSelf }
        val openApp: Action = actionStartActivity(Intent(context, MainActivity::class.java))

        provideContent {
            WidgetContent(
                self = self,
                freshness = freshness?.freshness ?: "NO DATA",
                total = cached.size,
                tapMode = settings.companionMode == CompanionMode.TAP_TO_ACTIVATE,
                widgetFields = settings.widgetFields,
                openApp = openApp,
            )
        }
    }
}

@Composable
private fun WidgetContent(
    self: CachedStanding?,
    freshness: String,
    total: Int,
    tapMode: Boolean,
    widgetFields: Set<String>,
    openApp: Action,
) {
    val textColor = ColorProvider(Color.White)
    val subColor = ColorProvider(Color(0xB3FFFFFF))
    val showRank = widgetFields.contains("rank")
    val showPoints = widgetFields.contains("points")
    val showGap = widgetFields.contains("gap")
    val showDelta = widgetFields.contains("delta")
    val showFreshness = widgetFields.contains("freshness")

    Column(
        modifier = GlanceModifier
            .background(com.fanlens.ai.ui.theme.PitchDark)
            .padding(12.dp)
            .clickable(openApp),
        verticalAlignment = Alignment.Top,
    ) {
        // DESIGN.md `mono-label`: uppercase technical marker, not a bold wordmark.
        Text(
            text = "FANLENS",
            style = TextStyle(
                color = ColorProvider(Color(0xFFE53935)),
                fontSize = 12.sp,
            ),
        )
        if (self == null) {
            Text(text = "No team data yet", style = TextStyle(color = textColor, fontSize = 14.sp))
            Text(
                text = if (tapMode) "TAP TO ACTIVATE" else "ALWAYS REACTIVE",
                style = TextStyle(color = subColor, fontSize = 12.sp),
            )
        } else {
            Row(modifier = GlanceModifier.padding(top = 4.dp)) {
                if (showRank) {
                    Text(
                        text = "#${self.rank}",
                        style = TextStyle(color = textColor, fontSize = 14.sp),
                    )
                }
                if (showPoints) {
                    Text(
                        text = (if (showRank) " of $total · " else "") + "${self.totalPoints} pts",
                        style = TextStyle(color = textColor, fontSize = 14.sp),
                    )
                }
            }
            if (showGap) {
                Text(
                    text = "gap to leader: ${self.gapToLeader}",
                    style = TextStyle(color = subColor, fontSize = 12.sp),
                )
            }
            if (showDelta) {
                Text(
                    text = "delta: ${self.deltaVsPrevious}",
                    style = TextStyle(color = subColor, fontSize = 12.sp),
                )
            }
            if (showFreshness) {
                Text(
                    text = freshness,
                    style = TextStyle(color = subColor, fontSize = 12.sp),
                )
            }
        }
    }
}

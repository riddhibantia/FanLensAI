package com.fanlens.ai.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Deep link into the app and widget refresh entry point. */
class ScoreWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScoreWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
                glanceAppWidget.updateAll(context)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.fanlens.ai.widget.REFRESH"
    }
}

/** Request a widget update from anywhere in the app. */
object WidgetUpdater {
    fun refresh(context: Context) {
        context.sendBroadcast(
            Intent(context, ScoreWidgetReceiver::class.java)
                .setAction(ScoreWidgetReceiver.ACTION_REFRESH)
        )
    }
}

package com.jpweytjens.barberfish.extension

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jpweytjens.barberfish.screens.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SparklineTapReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION = "com.jpweytjens.barberfish.SPARKLINE_TAP"
        const val EXTRA_LOOKAHEAD = "current_lookahead"
        // Pair<tapTimestamp, nextLookaheadKm>
        // Timestamp is always unique (ms), preventing MutableStateFlow deduplication.
        val tapSignal = MutableStateFlow(0L to 10)

        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private var pendingJob: Job? = null
        private var tapCount = 0
    }

    override fun onReceive(context: Context, intent: Intent) {
        val current = intent.getIntExtra(EXTRA_LOOKAHEAD, 10)
        tapCount++
        val count = tapCount

        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(400L)
            tapCount = 0
            if (count >= 3) {
                context.startActivity(
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } else {
                val next = when (current) { 5 -> 10; 10 -> 20; else -> 5 }
                tapSignal.value = System.currentTimeMillis() to next
                val cfg = context.streamSparklineConfig().first()
                context.saveSparklineConfig(cfg.copy(lookaheadKm = next))
            }
        }
    }
}

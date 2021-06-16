package io.github.hiromoo.kyutechtimetable

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.hiromoo.kyutechtimetable.model.Data.Companion.load

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notifier = Notifier(context)
        val action = intent.action

        if (action != null) {
            when (action) {
                "android.intent.action.BOOT_COMPLETED" -> {
                    load(context)
                    notifier.updateAll()
                }
            }
        } else {
            val id = intent.getStringExtra("id")!!
            val icon = intent.getIntExtra("icon", R.drawable.outline_event_24)
            val name = intent.getStringExtra("title")!!
            val location = intent.getStringExtra("text")!!
            val startTime = intent.getLongExtra("time", 0)
            notifier.notify(id, icon, name, location, startTime)
        }
    }
}
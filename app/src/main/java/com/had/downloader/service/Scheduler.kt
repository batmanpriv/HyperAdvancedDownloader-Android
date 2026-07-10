package com.had.downloader.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object Scheduler {

    private const val TAG = "Scheduler"
    private const val PREF = "had_scheduler"
    private const val EXTRA_ID = "download_id"

    fun schedule(context: Context, downloadId: Long, triggerTime: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(context, downloadId)

        val canExact = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> am.canScheduleExactAlarms()
            else -> true
        }

        try {
            if (canExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pi)
                }
                Log.d(TAG, "Exact alarm set for id=$downloadId at $triggerTime")
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
                Log.w(TAG, "Inexact alarm set for id=$downloadId (no exact alarm permission)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set alarm for id=$downloadId: ${e.message}")
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
        }

        save(context, downloadId, triggerTime)
    }

    fun cancel(context: Context, downloadId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(buildPendingIntent(context, downloadId))

        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val set = prefs.getStringSet("scheduled_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        set.remove(downloadId.toString())
        prefs.edit()
            .putStringSet("scheduled_ids", set)
            .remove("sched_$downloadId")
            .apply()

        Log.d(TAG, "Alarm cancelled for id=$downloadId")
    }

    fun cancelAll(context: Context) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet("scheduled_ids", emptySet()) ?: emptySet()
        ids.forEach { cancel(context, it.toLongOrNull() ?: return@forEach) }
        prefs.edit().clear().apply()
    }

    fun getScheduledDownloads(context: Context): Map<Long, Long> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet("scheduled_ids", emptySet()) ?: emptySet()
        return ids.mapNotNull { idStr ->
            val id = idStr.toLongOrNull() ?: return@mapNotNull null
            val time = prefs.getLong("sched_$id", 0L)
            if (time > 0L) id to time else null
        }.toMap()
    }

    fun rescheduleAll(context: Context) {
        val scheduled = getScheduledDownloads(context)
        val now = System.currentTimeMillis()
        scheduled.forEach { (id, time) ->
            if (time > now) {
                schedule(context, id, time)
                Log.d(TAG, "Rescheduled alarm for id=$id at $time")
            }
        }
    }

    private fun buildPendingIntent(context: Context, downloadId: Long): PendingIntent {
        val intent = Intent(context, DownloadAlarmReceiver::class.java).apply {
            action = "com.had.downloader.ALARM_TRIGGER"
            putExtra(EXTRA_ID, downloadId)
        }
        return PendingIntent.getBroadcast(
            context,
            downloadId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun save(context: Context, id: Long, time: Long) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val set = prefs.getStringSet("scheduled_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add(id.toString())
        prefs.edit()
            .putStringSet("scheduled_ids", set)
            .putLong("sched_$id", time)
            .apply()
    }
}
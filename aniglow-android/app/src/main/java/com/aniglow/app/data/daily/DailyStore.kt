package com.aniglow.app.data.daily

import android.content.Context
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DailyState(
    val date: String = LocalDate.now().toString(),
    val completedTaskIds: Set<String> = emptySet(),
    val tickets: Int = 0,
    val browseSeconds: Int = 0,
) {
    val completedCount: Int get() = completedTaskIds.size
}

object DailyStore {
    const val VIEW_COMMENTS = "view-comments"
    const val PUBLISH_COMMENT = "publish-comment"
    const val BROWSE_FIVE_MINUTES = "browse-five-minutes"
    const val BROWSE_TARGET_SECONDS = 5 * 60

    private const val PREFS_NAME = "aniglow_daily_tasks"
    private const val DATE_KEY = "date"
    private const val COMPLETED_KEY = "completed"
    private const val TICKETS_KEY = "tickets"
    private const val BROWSE_SECONDS_KEY = "browse_seconds"

    private lateinit var context: Context
    private val mutableState = MutableStateFlow(DailyState())
    val state: StateFlow<DailyState> = mutableState.asStateFlow()

    @Synchronized
    fun initialize(applicationContext: Context) {
        if (!::context.isInitialized) context = applicationContext
        refreshForToday()
    }

    @Synchronized
    fun completeTask(taskId: String) {
        refreshForToday()
        val current = mutableState.value
        if (taskId in current.completedTaskIds) return
        persist(
            current.copy(
                completedTaskIds = current.completedTaskIds + taskId,
                tickets = current.tickets + 1,
            ),
        )
    }

    @Synchronized
    fun tickBrowseSecond() {
        refreshForToday()
        val current = mutableState.value
        if (BROWSE_FIVE_MINUTES in current.completedTaskIds) return
        val seconds = (current.browseSeconds + 1).coerceAtMost(BROWSE_TARGET_SECONDS)
        persist(current.copy(browseSeconds = seconds))
        if (seconds >= BROWSE_TARGET_SECONDS) completeTask(BROWSE_FIVE_MINUTES)
    }

    @Synchronized
    fun consumeTicket(): Boolean {
        refreshForToday()
        val current = mutableState.value
        if (current.tickets <= 0) return false
        persist(current.copy(tickets = current.tickets - 1))
        return true
    }

    @Synchronized
    private fun refreshForToday() {
        check(::context.isInitialized) { "DailyStore must be initialized first" }
        val today = LocalDate.now().toString()
        val preferences = preferences()
        val storedDate = preferences.getString(DATE_KEY, null)
        if (storedDate != today) {
            persist(DailyState(date = today))
            return
        }
        mutableState.value = DailyState(
            date = today,
            completedTaskIds = preferences.getStringSet(COMPLETED_KEY, emptySet()).orEmpty().toSet(),
            tickets = preferences.getInt(TICKETS_KEY, 0).coerceAtLeast(0),
            browseSeconds = preferences.getInt(BROWSE_SECONDS_KEY, 0).coerceIn(0, BROWSE_TARGET_SECONDS),
        )
    }

    private fun persist(state: DailyState) {
        mutableState.value = state
        preferences().edit()
            .putString(DATE_KEY, state.date)
            .putStringSet(COMPLETED_KEY, state.completedTaskIds)
            .putInt(TICKETS_KEY, state.tickets)
            .putInt(BROWSE_SECONDS_KEY, state.browseSeconds)
            .apply()
    }

    private fun preferences() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

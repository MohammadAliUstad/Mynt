package com.yugentech.mynt.reminders

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderSchedulerTest {

    private fun at(millis: Long): LocalDateTime =
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault())

    @Test
    fun `a time later today fires today`() {
        val now = LocalDateTime.of(2026, 10, 17, 9, 0)
        assertEquals(LocalDateTime.of(2026, 10, 17, 20, 0), at(ReminderScheduler.nextTrigger(20, 0, now)))
    }

    @Test
    fun `a time already passed fires tomorrow`() {
        val now = LocalDateTime.of(2026, 10, 17, 21, 30)
        assertEquals(LocalDateTime.of(2026, 10, 18, 20, 0), at(ReminderScheduler.nextTrigger(20, 0, now)))
    }

    @Test
    fun `exactly now counts as passed`() {
        val now = LocalDateTime.of(2026, 10, 17, 20, 0)
        assertEquals(LocalDateTime.of(2026, 10, 18, 20, 0), at(ReminderScheduler.nextTrigger(20, 0, now)))
    }
}

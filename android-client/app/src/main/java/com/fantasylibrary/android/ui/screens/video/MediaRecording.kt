package com.fantasylibrary.android.ui.screens.video

import android.os.SystemClock
import androidx.media3.common.Player
import com.fantasylibrary.android.data.repository.ViewingSync

internal class MediaRecording(private val player: Player) {
    private data class Media(val work: Long, val edition: String, val number: Int, val sync: ViewingSync.Recording)
    private var media: Media? = null
    private var playing = false
    private var started = false
    private var lastSample = 0L

    fun attach(work: Long, edition: String, number: Int, sync: ViewingSync.Recording) {
        media = Media(work, edition, number, sync)
        if (player.isPlaying) playing(true)
    }

    fun playing(active: Boolean) {
        if (active == playing || media == null) return
        if (!active) send()
        playing = active
        if (active) {
            lastSample = SystemClock.elapsedRealtime()
            val position = player.currentPosition.coerceAtLeast(0)
            if (!started) {
                started = true
                media?.sync?.begin(position)
            } else media?.sync?.sample(position, player.duration, playing = false, discontinuity = true)
        }
    }

    fun heartbeat() {
        if (playing && SystemClock.elapsedRealtime() - lastSample >= 18_000) send()
    }

    fun beforeSeek() { if (playing) send() }

    fun discontinuity(position: Long) {
        if (started) media?.sync?.sample(position.coerceAtLeast(0), player.duration, playing = false, discontinuity = true)
        lastSample = SystemClock.elapsedRealtime()
    }

    fun finish() { if (playing) send() }

    fun closeForSwitch(work: Long, edition: String?, number: Int?): Long? {
        val old = media
        val offset = player.currentPosition.takeIf {
            old != null && old.work == work && old.edition == edition && old.number == number
        }
        close()
        return offset
    }

    fun close() {
        if (playing) send()
        media = null
        playing = false
        started = false
    }

    private fun send() {
        media?.sync?.sample(player.currentPosition.coerceAtLeast(0), player.duration, playing = true)
        lastSample = SystemClock.elapsedRealtime()
    }
}

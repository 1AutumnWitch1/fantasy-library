package com.fantasylibrary.android.data.repository

import android.content.Context
import com.fantasylibrary.android.data.network.Outcome
import com.fantasylibrary.android.data.network.SavedEpisodeDto
import com.fantasylibrary.android.data.network.PlaybackSampleDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ViewingSync(context: Context, private val reader: ReaderRepository, private val auth: AuthRepository) {
    private val device = context.getSharedPreferences("viewing-resume", Context.MODE_PRIVATE)
    // Disposal queues the final sample before releasing the player. A return to the details page
    // waits for that write before fetching the new resume position.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pending = Mutex()

    suspend fun progress(base: String, work: Long) = pending.withLock { reader.viewingProgress(base, work) }


    fun localProgress(base: String, work: Long): SavedEpisodeDto? {
        val saved = device.getString(deviceKey(base, work), null)?.split(':') ?: return null
        val episode = saved.getOrNull(0)?.toIntOrNull() ?: return null
        val position = saved.getOrNull(1)?.toLongOrNull() ?: return null
        val duration = saved.getOrNull(2)?.toLongOrNull() ?: return null
        return SavedEpisodeDto(episode, position, duration, completed = false, updatedAt = "")
    }

    fun session(base: String, work: Long, source: Long, episode: Int?, onCounted: () -> Unit): Recording =
        Recording(base, work, source, episode, auth.session.value.username, onCounted)

    private fun deviceKey(base: String, work: Long) = "$base#$work"

    inner class Recording(
        private val base: String,
        private val work: Long,
        private val source: Long,
        private val episode: Int?,
        private val owner: String,
        private val onCounted: () -> Unit,
    ) {
        private var session: String? = null

        fun begin(position: Long) = queue {
            if (session == null) connect(position)
        }

        fun sample(position: Long, duration: Long, playing: Boolean, discontinuity: Boolean = false) {
            if (duration <= 0 || position < 0) return
            if (owner.isEmpty() && episode != null && playing && !discontinuity && position > 0) {
                val key = deviceKey(base, work)
                val edit = device.edit()
                if (position >= duration - 500) edit.remove(key)
                else edit.putString(key, "$episode:$position:$duration")
                edit.apply()
            }
            queue {
                val id = session
                if (id == null) connect(position)
                else when (val result = reader.samplePlayback(base, work, id,
                    PlaybackSampleDto(position, duration, playing, discontinuity))) {
                    is Outcome.Ok -> if (result.value.counted) onCounted()
                    is Outcome.Err -> session = null
                }
            }
        }

        private suspend fun connect(position: Long) {
            session = (reader.startPlayback(base, work, source, position) as? Outcome.Ok)?.value?.id
        }

        private fun queue(action: suspend () -> Unit) {
            scope.launch {
                pending.withLock {
                    if (auth.session.value.username == owner) action()
                }
            }
        }
    }
}

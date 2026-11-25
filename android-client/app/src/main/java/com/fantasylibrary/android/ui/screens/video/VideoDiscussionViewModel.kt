package com.fantasylibrary.android.ui.screens.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fantasylibrary.android.data.network.*
import com.fantasylibrary.android.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VideoDiscussionViewModel(private val container: AppContainer) : ViewModel() {
    data class Replies(val items: List<ScreeningCommentDto> = emptyList(), val page: Int = 0,
        val more: Boolean = true, val loading: Boolean = false, val error: Failure? = null)
    data class State(val workId: Long = 0, val threads: List<ScreeningThreadDto> = emptyList(), val total: Long? = null,
        val page: Int = 0, val more: Boolean = true, val loading: Boolean = false,
        val replies: Map<Long, Replies> = emptyMap(), val error: Failure? = null,
        val sending: Boolean = false, val writeError: Failure? = null, val likesPending: Set<Long> = emptySet())

    private data class Watch(val base: String, val work: Long, val member: Boolean)
    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()
    private var watch: Watch? = null
    private var reads: Job? = null
    private val repository get() = container.readerRepository

    fun load(base: String, work: Long, member: Boolean) {
        val next = Watch(base, work, member)
        if (base.isBlank() || work <= 0 || next == watch) return
        reads?.cancel()
        watch = next
        _state.value = State(workId = work)
        loadPage()
    }

    fun loadPage() {
        val current = watch ?: return
        val prior = _state.value
        if (prior.loading || !prior.more) return
        val page = prior.page + 1
        _state.update { it.copy(loading = true, error = null) }
        reads = viewModelScope.launch {
            val response = repository.screeningThreads(current.base, current.work, page, current.member)
            if (watch !== current) return@launch
            _state.update {
                when (response) {
                    is Outcome.Ok -> it.copy(threads = (it.threads + response.value.items).distinctBy { t -> t.comment.id },
                        total = response.value.total, page = response.value.page, more = response.value.page < response.value.totalPages,
                        loading = false)
                    is Outcome.Err -> it.copy(loading = false, error = response.failure)
                }
            }
        }
    }

    fun moreReplies(root: Long) {
        val current = watch ?: return
        val before = _state.value.replies[root] ?: Replies()
        if (before.loading || !before.more) return
        _state.update { it.copy(replies = it.replies + (root to before.copy(loading = true, error = null))) }
        viewModelScope.launch {
            val result = repository.screeningReplies(current.base, current.work, root, before.page + 1, current.member)
            if (watch !== current) return@launch
            _state.update { state ->
                val next = when (result) {
                    is Outcome.Ok -> Replies((before.items + result.value.items).distinctBy { it.id },
                        result.value.page, result.value.page < result.value.totalPages)
                    is Outcome.Err -> before.copy(error = result.failure)
                }
                state.copy(replies = state.replies + (root to next))
            }
        }
    }

    fun post(body: String, parent: Long?, onSuccess: () -> Unit) {
        val current = watch ?: return
        if (!current.member || body.isBlank() || _state.value.sending) return
        _state.update { it.copy(sending = true, writeError = null) }
        viewModelScope.launch {
            val result = repository.screeningComment(current.base, current.work, CommentDraft(body.trim(), parent))
            if (watch !== current) return@launch
            when (result) {
                is Outcome.Ok -> {
                    reads?.cancel()
                    watch = current.copy()
                    _state.value = State(workId = current.work)
                    onSuccess()
                    loadPage()
                }
                is Outcome.Err -> _state.update { it.copy(sending = false, writeError = result.failure) }
            }
        }
    }

    fun like(comment: ScreeningCommentDto) {
        val current = watch ?: return
        if (!current.member || comment.removed || comment.id in _state.value.likesPending) return
        _state.update { it.copy(likesPending = it.likesPending + comment.id, writeError = null) }
        viewModelScope.launch {
            val result = repository.screeningLike(current.base, current.work, comment.id, !comment.liked)
            if (watch !== current) return@launch
            _state.update { state ->
                val next = state.copy(likesPending = state.likesPending - comment.id)
                when (result) {
                    is Outcome.Err -> next.copy(writeError = result.failure)
                    is Outcome.Ok -> {
                        val update: (ScreeningCommentDto) -> ScreeningCommentDto = {
                            if (it.id == comment.id) it.copy(liked = result.value.liked, likeCount = result.value.count) else it
                        }
                        next.copy(
                            threads = next.threads.map { it.copy(comment = update(it.comment), replies = it.replies.map(update)) },
                            replies = next.replies.mapValues { (_, page) -> page.copy(items = page.items.map(update)) },
                        )
                    }
                }
            }
        }
    }

    fun clearError() { _state.update { it.copy(writeError = null) } }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory { initializer { VideoDiscussionViewModel(container) } }
    }
}

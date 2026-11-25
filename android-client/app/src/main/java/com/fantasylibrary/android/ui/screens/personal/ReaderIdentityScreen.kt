package com.fantasylibrary.android.ui.screens.personal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fantasylibrary.android.data.network.*
import com.fantasylibrary.android.di.AppContainer
import com.fantasylibrary.android.ui.components.*
import com.fantasylibrary.android.ui.screens.community.PostPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReaderIdentityViewModel(private val container: AppContainer) : ViewModel() {
    data class State(val base: String = "", val signedIn: Boolean = false,
        val profile: ProfileDto? = null, val failure: Failure? = null, val loading: Boolean = false)
    data class Posts(val items: List<PostDto> = emptyList(), val page: Int = 0, val more: Boolean = true,
        val busy: Boolean = false, val failure: Failure? = null)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()
    private val _posts = MutableStateFlow(Posts())
    val posts = _posts.asStateFlow()
    private val retry = MutableStateFlow(0)
    private var postsGeneration = 0

    init {
        viewModelScope.launch {
            combine(container.settingsRepository.settings.map { it.baseUrl }.distinctUntilChanged(),
                container.authRepository.session.map { it.signedIn }.distinctUntilChanged(),
                retry,
            ) { base, signed, _ -> base to signed }.collectLatest { (base, signed) ->
                postsGeneration++
                _posts.value = Posts()
                _state.value = State(base, signed, loading = signed && base.isNotBlank())
                if (signed && base.isNotBlank()) {
                    when (val result = container.authRepository.loadProfile(base)) {
                        is Outcome.Ok -> _state.value = State(base, true, result.value)
                        is Outcome.Err -> {
                            if (result.failure.kind == FailureKind.UNAUTHORIZED) container.authRepository.clearLocal()
                            _state.value = State(base, true, failure = result.failure)
                        }
                    }
                }
            }
        }
    }

    fun refresh() { retry.update { it + 1 } }

    fun saveProfile(name: String, bio: String, onDone: (Outcome<ProfileDto>) -> Unit) {
        val current = _state.value
        if (current.profile == null || name.isBlank()) return
        viewModelScope.launch {
            val result = container.readerRepository.updateProfile(current.base, ProfileEditDto(name.trim(), bio.trim()))
            val active = _state.value
            if (active.profile?.id == current.profile.id && active.base == current.base && result is Outcome.Ok) {
                _state.value = active.copy(profile = result.value, failure = null)
            }
            onDone(result)
        }
    }

    fun signOut(onDone: (Outcome<Unit>) -> Unit) {
        val base = _state.value.base
        if (base.isBlank()) return
        viewModelScope.launch { onDone(container.authRepository.logout(base)) }
    }

    fun loadPosts() {
        val person = _state.value
        val id = person.profile?.id ?: return
        val generation = postsGeneration
        val prior = _posts.value
        if (prior.busy || !prior.more) return
        val next = prior.page + 1
        _posts.value = prior.copy(busy = true, failure = null)
        viewModelScope.launch {
            val result = container.readerRepository.posts(person.base, next, null, false, null)
            if (postsGeneration != generation) return@launch
            when (result) {
                is Outcome.Ok -> _posts.value = Posts(
                    items = (prior.items + result.value.items.filter { it.authorId == id }).distinctBy { it.id },
                    page = result.value.page,
                    more = result.value.page < result.value.totalPages,
                )
                is Outcome.Err -> _posts.value = prior.copy(failure = result.failure, busy = false)
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory { initializer { ReaderIdentityViewModel(container) } }
    }
}

@Composable
fun ReaderIdentityScreen(identity: ReaderIdentityViewModel.State, posts: ReaderIdentityViewModel.Posts,
    cleartext: Boolean, onBack: () -> Unit, onLogin: () -> Unit, onShelf: () -> Unit,
    onHistory: () -> Unit, onSettings: () -> Unit, onPost: (Long) -> Unit,
    onRefresh: () -> Unit, onMorePosts: () -> Unit,
) {
    val person = identity.profile
    LaunchedEffect(person?.id) { if (person != null && posts.page == 0 && posts.failure == null) onMorePosts() }
    ScrollRoom("个人资料", onBack, backLabel = "返回") {
        when {
            identity.loading -> PageStatus("正在读取个人资料…", busy = true)
            !identity.signedIn -> PageStatus("登录即可查看个人资料。", actionLabel = "登录", action = onLogin)
            identity.failure != null -> PageStatus(identity.failure.readerMessage(), action = onRefresh)
            person == null -> PageStatus("连接服务器后即可读取个人资料。", actionLabel = "连接设置", action = onSettings)
            else -> LazyColumn(Modifier.fillMaxSize(), state = rememberLazyListState(),
                contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                item {
                    PaperTile(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                            ReaderAvatar(person.avatarKey, identity.base, cleartext, person.displayName, Modifier.size(64.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(person.displayName.ifBlank { person.username }, color = PageInk, style = MaterialTheme.typography.headlineSmall)
                                Text("@${person.username}", color = PageMuted, style = MaterialTheme.typography.labelMedium)
                                person.bio?.takeIf { it.isNotBlank() }?.let { Text(it, color = PageInk, style = MaterialTheme.typography.bodyMedium) }
                            }
                        }
                        if (!posts.more || posts.page > 0) Text(if (posts.more) "已找到 ${posts.items.size} 篇公开手稿" else "${posts.items.size} 篇公开手稿",
                            Modifier.padding(horizontal = 15.dp, vertical = 8.dp), color = PageMuted, style = MaterialTheme.typography.labelMedium)
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        PageButton("个人收藏", onShelf)
                        PageButton("观看历史", onHistory)
                        PageButton("设置", onSettings)
                    }
                }
                item { ScrollHeading("发布的手稿") }
                items(posts.items, key = { it.id }) { post ->
                    PostPreview(post, identity.base, cleartext) { onPost(post.id) }
                }
                item {
                    when {
                        posts.busy -> PageStatus("正在查找手稿…", busy = true)
                        posts.failure != null -> PageStatus(posts.failure.readerMessage(), action = onMorePosts)
                        posts.more -> PageStatus(if (posts.items.isEmpty()) "本页档案里暂无你的手稿。" else "还有更早的公开档案。",
                            actionLabel = "查找更早的手稿", action = onMorePosts)
                        posts.items.isEmpty() -> PageStatus("尚无公开手稿。")
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderSettingsScreen(identity: ReaderIdentityViewModel.State, viewModel: ReaderIdentityViewModel,
    onBack: () -> Unit, onConnection: () -> Unit,
) {
    val profile = identity.profile
    var name by rememberSaveable(profile?.id) { mutableStateOf(profile?.displayName.orEmpty()) }
    var bio by rememberSaveable(profile?.id) { mutableStateOf(profile?.bio.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    ScrollRoom("个人设置", onBack, backLabel = "返回") {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (profile != null) {
                ScrollHeading("编辑资料")
                PageField(name, { name = it.take(60) }, "昵称", Modifier.fillMaxWidth())
                PageField(bio, { bio = it.take(240) }, "简介", Modifier.fillMaxWidth(), singleLine = false)
                PageButton("保存个人资料", enabled = name.isNotBlank() && !busy, emphasized = true, onClick = {
                    busy = true
                    viewModel.saveProfile(name, bio) { result ->
                        busy = false
                        status = when (result) {
                            is Outcome.Ok -> "个人资料已保存。"
                            is Outcome.Err -> result.failure.readerMessage()
                        }
                    }
                })
                status?.let { Text(it, color = PageMuted) }
                PageButton("退出登录", enabled = !busy, onClick = {
                    busy = true
                    viewModel.signOut { onBack() }
                })
            }
            PageButton("连接设置", onConnection)
        }
    }
}

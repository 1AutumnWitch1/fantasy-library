package com.fantasylibrary.android.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.compose.currentBackStackEntryAsState
import com.fantasylibrary.android.di.AppContainer
import com.fantasylibrary.android.ui.screens.catalog.*
import com.fantasylibrary.android.ui.screens.community.*
import com.fantasylibrary.android.ui.screens.config.ServerConfigScreen
import com.fantasylibrary.android.ui.screens.config.ServerConfigViewModel
import com.fantasylibrary.android.ui.screens.detail.WorkDetailScreen
import com.fantasylibrary.android.ui.screens.detail.WorkDetailViewModel
import com.fantasylibrary.android.ui.screens.home.ScrollHomeScreen
import com.fantasylibrary.android.ui.screens.home.LibraryScroll
import com.fantasylibrary.android.ui.screens.login.LoginScreen
import com.fantasylibrary.android.ui.screens.login.LoginViewModel
import com.fantasylibrary.android.ui.screens.personal.ProfileScreen
import com.fantasylibrary.android.ui.screens.personal.ReaderIdentityScreen
import com.fantasylibrary.android.ui.screens.personal.ReaderSettingsScreen
import com.fantasylibrary.android.ui.screens.personal.ReaderIdentityViewModel
import com.fantasylibrary.android.ui.screens.recommendation.RecommendationScreen
import com.fantasylibrary.android.ui.screens.recommendation.RecommendationViewModel
import com.fantasylibrary.android.ui.screens.video.VideoScreen
import com.fantasylibrary.android.ui.screens.video.VideoDiscussionViewModel
import com.fantasylibrary.android.ui.screens.video.VideoViewModel

private const val Home = "home"
private const val Shelf = "personal?history={history}"
private const val Login = "login?profile={profile}"
private const val Community = "community?workId={workId}"
private val scrollDestinations = LibraryScroll.entries.map { when (it) { LibraryScroll.Community -> Community; LibraryScroll.Personal -> Shelf; else -> it.route } }.toSet()

@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberNavController()
    val currentEntry by nav.currentBackStackEntryAsState()
    var openingBias by remember { mutableFloatStateOf(0f) }
    val back: () -> Unit = { if (!nav.popBackStack()) nav.open(Home) }
    val settings: () -> Unit = { nav.open("config") }
    val login: () -> Unit = { nav.open("login") }
    val work: (Long) -> Unit = { id -> nav.open("detail/$id") }
    val post: (Long) -> Unit = { id -> nav.open("post/$id") }
    val cleartext = container.apiClient.allowCleartext
    val reader: ReaderIdentityViewModel = viewModel(factory = ReaderIdentityViewModel.factory(container))
    val identity by reader.state.collectAsStateWithLifecycle()
    val showIdentity: () -> Unit = {
        if (container.authRepository.session.value.signedIn) nav.open("reader-profile")
        else nav.open("login?profile=true")
    }

    NavHost(navController = nav, startDestination = Home, modifier = Modifier.fillMaxSize(),
        enterTransition = {
            if (initialState.destination.route == Home && targetState.destination.route in scrollDestinations) {
                fadeIn(tween(205)) + expandVertically(tween(430, easing = FastOutSlowInEasing), expandFrom = BiasAlignment.Vertical(openingBias), initialHeight = { (it * .09f).toInt() })
            } else EnterTransition.None
        },
        exitTransition = {
            if (initialState.destination.route == Home && targetState.destination.route in scrollDestinations) {
                fadeOut(tween(310, delayMillis = 95))
            } else ExitTransition.None
        },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable(Home) { entry ->
            ScrollHomeScreen(active = currentEntry == entry, identity = identity, cleartext = cleartext,
                onOpen = { scroll, bias ->
                    if (nav.currentDestination?.route == Home) {
                        openingBias = bias
                        nav.open(scroll.route)
                    }
                }, onProfile = showIdentity,
            )
        }
        composable("anime", deepLinks = scrollLinks("anime")) {
            val vm: CatalogViewModel = viewModel(factory = CatalogViewModel.factory(container, CatalogKind.ANIME))
            AnimeScreen(vm, back, work, settings)
        }
        composable("games", deepLinks = scrollLinks("games")) {
            val vm: CatalogViewModel = viewModel(factory = CatalogViewModel.factory(container, CatalogKind.GAME))
            GameScreen(vm, back, work, settings)
        }
        composable(Community, deepLinks = scrollLinks("community"), arguments = listOf(navArgument("workId") { type = NavType.LongType; defaultValue = -1L })) { entry ->
            val workId = entry.arguments?.getLong("workId")?.takeIf { it > 0 }
            val vm: CommunityViewModel = viewModel(factory = CommunityViewModel.factory(container, workId))
            val refresh by entry.savedStateHandle.getStateFlow("post-published", false).collectAsStateWithLifecycle()
            LaunchedEffect(refresh) {
                if (refresh) {
                    entry.savedStateHandle["post-published"] = false
                    vm.load(true)
                }
            }
            CommunityScreen(vm, cleartext, back, post, { nav.open("compose-post") }, settings)
        }
        composable("recommendations", deepLinks = scrollLinks("recommendations")) {
            val vm: RecommendationViewModel = viewModel(factory = RecommendationViewModel.factory(container))
            RecommendationScreen(vm, cleartext, back, work, post, settings)
        }
        composable(Shelf, deepLinks = scrollLinks("personal"),
            arguments = listOf(navArgument("history") { type = NavType.BoolType; defaultValue = false })) { entry ->
            ProfileScreen(container, back, login, settings, work, post, entry.arguments?.getBoolean("history") == true)
        }
        composable("reader-profile") {
            val published by reader.posts.collectAsStateWithLifecycle()
            ReaderIdentityScreen(identity, published, cleartext, back, showIdentity,
                onShelf = { nav.open("personal") }, onHistory = { nav.open("personal?history=true") },
                onSettings = { nav.open("reader-settings") }, onPost = post, onRefresh = reader::refresh, onMorePosts = reader::loadPosts)
        }
        composable("reader-settings") { ReaderSettingsScreen(identity, reader, back, settings) }
        composable("config") {
            val vm: ServerConfigViewModel = viewModel(factory = ServerConfigViewModel.factory(container))
            ServerConfigScreen(vm, back)
        }
        composable(Login, arguments = listOf(navArgument("profile") { type = NavType.BoolType; defaultValue = false })) { entry ->
            val vm: LoginViewModel = viewModel(factory = LoginViewModel.factory(container))
            LoginScreen(vm, back, settings, onSignedIn = {
                if (entry.arguments?.getBoolean("profile") == true && nav.currentBackStackEntry == entry) {
                    nav.navigate("reader-profile") {
                        popUpTo(entry.destination.id) { inclusive = true }
                        launchSingleTop = true
                    }
                } else if (nav.currentBackStackEntry == entry) back()
            })
        }
        composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = requireNotNull(entry.arguments).getLong("id")
            val vm: WorkDetailViewModel = viewModel(factory = WorkDetailViewModel.factory(container, id))
            WorkDetailScreen(vm, container, back, login,
                onPlayAnime = { nav.open("video/$it") },
                onDiscussion = { nav.open("community?workId=$it") },
            )
        }
        composable("video/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = requireNotNull(entry.arguments).getLong("id")
            val vm: VideoViewModel = viewModel(factory = VideoViewModel.factory(container, initialWorkId = id))
            val discussion: VideoDiscussionViewModel = viewModel(factory = VideoDiscussionViewModel.factory(container))
            VideoScreen(vm, discussion, container, onOpenConfig = settings, onBack = back, onLogin = login)
        }
        composable("post/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            PostScreen(requireNotNull(entry.arguments).getLong("id"), container, back, login, work)
        }
        composable("compose-post") {
            ComposePostScreen(container, back, login, onPublished = {
                nav.previousBackStackEntry?.savedStateHandle?.set("post-published", true)
                nav.popBackStack()
                post(it)
            })
        }
    }
}

private fun NavHostController.open(route: String) = navigate(route) { launchSingleTop = true }

private fun scrollLinks(route: String) = listOf(navDeepLink { uriPattern = "fantasy-library://scroll/$route" })

package com.fantasylibrary.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fantasylibrary.android.di.AppContainer
import com.fantasylibrary.android.ui.components.RuneIcon
import com.fantasylibrary.android.ui.components.RuneKind
import com.fantasylibrary.android.ui.screens.catalog.CatalogKind
import com.fantasylibrary.android.ui.screens.catalog.CatalogScreen
import com.fantasylibrary.android.ui.screens.catalog.CatalogViewModel
import com.fantasylibrary.android.ui.screens.config.ServerConfigScreen
import com.fantasylibrary.android.ui.screens.config.ServerConfigViewModel
import com.fantasylibrary.android.ui.screens.detail.WorkDetailScreen
import com.fantasylibrary.android.ui.screens.detail.WorkDetailViewModel
import com.fantasylibrary.android.ui.screens.home.HomeScreen
import com.fantasylibrary.android.ui.screens.home.HomeViewModel
import com.fantasylibrary.android.ui.screens.login.LoginScreen
import com.fantasylibrary.android.ui.screens.login.LoginViewModel
import com.fantasylibrary.android.ui.screens.video.VideoScreen
import com.fantasylibrary.android.ui.screens.video.VideoViewModel
import com.fantasylibrary.android.ui.theme.Brass
import com.fantasylibrary.android.ui.theme.BrassDim
import com.fantasylibrary.android.ui.theme.ParchmentDeep
import com.fantasylibrary.android.ui.theme.Walnut
import com.fantasylibrary.android.ui.theme.WalnutEdge

private data class Tab(val route: String, val label: String, val rune: RuneKind)

private val TABS = listOf(
    Tab("config", "连接", RuneKind.Link),
    Tab("home", "状态", RuneKind.Status),
    Tab("anime", "动漫", RuneKind.Anime),
    Tab("games", "游戏", RuneKind.Games),
    Tab("video", "放映", RuneKind.Play),
)

@Composable
fun AppNavHost(
    container: AppContainer,
    startDestination: String,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable("config") {
                    val configViewModel: ServerConfigViewModel =
                        viewModel(factory = ServerConfigViewModel.factory(container))
                    ServerConfigScreen(viewModel = configViewModel, onBack = null)
                }

                composable("home") {
                    val homeViewModel: HomeViewModel =
                        viewModel(factory = HomeViewModel.factory(container))
                    HomeScreen(
                        viewModel = homeViewModel,
                        onOpenAnime = { navController.navigateToTab("anime") },
                        onOpenGames = { navController.navigateToTab("games") },
                        onOpenVideo = { navController.navigateToTab("video") },
                        onOpenConfig = { navController.navigateToTab("config") },
                        onOpenLogin = { navController.navigate("login") },
                    )
                }

                composable("anime") {
                    val catalogViewModel: CatalogViewModel =
                        viewModel(factory = CatalogViewModel.factory(container, CatalogKind.ANIME))
                    CatalogScreen(
                        viewModel = catalogViewModel,
                        onOpenDetail = { id -> navController.navigate("detail/$id") },
                    )
                }

                composable("games") {
                    val catalogViewModel: CatalogViewModel =
                        viewModel(factory = CatalogViewModel.factory(container, CatalogKind.GAME))
                    CatalogScreen(
                        viewModel = catalogViewModel,
                        onOpenDetail = { id -> navController.navigate("detail/$id") },
                    )
                }

                composable(
                    route = "video?workId={workId}",
                    arguments = listOf(
                        navArgument("workId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                    ),
                ) { entry ->
                    val workId = entry.arguments?.getLong("workId") ?: -1L
                    val videoViewModel: VideoViewModel = viewModel(
                        factory = VideoViewModel.factory(container, initialWorkId = workId.takeIf { it > 0 }),
                    )
                    VideoScreen(
                        viewModel = videoViewModel,
                        onOpenConfig = { navController.navigateToTab("config") },
                    )
                }

                composable(
                    route = "detail/{workId}",
                    arguments = listOf(navArgument("workId") { type = NavType.LongType }),
                ) { entry ->
                    val workId = entry.arguments?.getLong("workId") ?: 0L
                    val detailViewModel: WorkDetailViewModel =
                        viewModel(factory = WorkDetailViewModel.factory(container, workId))
                    WorkDetailScreen(
                        viewModel = detailViewModel,
                        onBack = { navController.popBackStack() },
                        onPlayAnime = { id -> navController.navigate("video?workId=$id") },
                    )
                }

                composable("login") {
                    val loginViewModel: LoginViewModel =
                        viewModel(factory = LoginViewModel.factory(container))
                    LoginScreen(
                        viewModel = loginViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }

        if (currentRoute != null && TABS.any { it.route == currentRoute }) {
            LibraryBottomBar(
                currentRoute = currentRoute,
                onSelect = navController::navigateToTab,
            )
        }
    }
}

@Composable
private fun LibraryBottomBar(
    currentRoute: String,
    onSelect: (String) -> Unit,
) {
    Surface(
        color = Walnut,
        border = BorderStroke(1.dp, WalnutEdge),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TABS.forEach { tab ->
                val selected = tab.route == currentRoute
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab.route) }
                        .padding(vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    RuneIcon(
                        kind = tab.rune,
                        tint = if (selected) Brass else ParchmentDeep,
                        size = 19.dp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Brass else ParchmentDeep,
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        Modifier
                            .height(1.5.dp)
                            .fillMaxWidth(if (selected) 0.26f else 0f)
                            .background(BrassDim),
                    )
                }
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

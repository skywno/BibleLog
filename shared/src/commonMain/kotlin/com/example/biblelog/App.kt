package com.example.biblelog

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.biblelog.di.AppContainer
import com.example.biblelog.di.LocalAuthRepository
import com.example.biblelog.di.ProvideAppDependencies
import com.example.biblelog.feature.ai.AiChatScreen
import com.example.biblelog.feature.auth.LoginScreen
import com.example.biblelog.feature.bible.BibleScreen
import com.example.biblelog.feature.community.CommunityScreen
import com.example.biblelog.feature.home.HomeScreen
import com.example.biblelog.feature.journal.JournalScreen
import com.example.biblelog.feature.profile.ProfileScreen
import com.example.biblelog.navigation.BibleSubRoute
import com.example.biblelog.navigation.JournalNavState
import com.example.biblelog.navigation.JournalNavStateSaver
import com.example.biblelog.navigation.JournalSubRoute
import com.example.biblelog.navigation.MainTab
import com.example.biblelog.ui.theme.WantedColors
import com.example.biblelog.ui.theme.WantedTheme
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    WantedTheme {
        ProvideAppDependencies {
            BibleLogApp()
        }
    }
}

@Composable
fun BibleLogApp() {
    val authRepository = LocalAuthRepository.current
    val session by authRepository.session.collectAsState()
    var isBootstrapping by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val restored = authRepository.restoreSession()
        if (restored) {
            runCatching { AppContainer.repository.refreshAll() }
        }
        isBootstrapping = false
    }

    when {
        isBootstrapping -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WantedColors.Primary)
            }
        }
        session == null -> LoginScreen(
            onLoggedIn = {
                scope.launch {
                    runCatching { AppContainer.repository.refreshAll() }
                }
            },
        )
        else -> BibleLogMainScaffold()
    }
}

@Composable
private fun BibleLogMainScaffold() {
    val authRepository = LocalAuthRepository.current
    val session by authRepository.session.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var bibleSubRoute by rememberSaveable { mutableStateOf(BibleSubRoute.Dashboard) }
    var journalNavState by rememberSaveable(stateSaver = JournalNavStateSaver) {
        mutableStateOf(JournalNavState())
    }

    LaunchedEffect(session?.user?.id) {
        session?.user?.let { AppContainer.repository.seedCurrentUser(it) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = WantedColors.Canvas,
        bottomBar = {
            NavigationBar(
                containerColor = WantedColors.Canvas,
                contentColor = WantedColors.Heading,
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            if (tab == MainTab.Bible) bibleSubRoute = BibleSubRoute.Dashboard
                            if (tab == MainTab.Journal) journalNavState = JournalNavState()
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WantedColors.Primary,
                            selectedTextColor = WantedColors.Primary,
                            unselectedIconColor = WantedColors.Secondary,
                            unselectedTextColor = WantedColors.Secondary,
                            indicatorColor = WantedColors.Primary.copy(alpha = 0.1f),
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.Home -> HomeScreen(
                onNavigateToBible = {
                    selectedTab = MainTab.Bible
                    bibleSubRoute = BibleSubRoute.AddRecord
                },
                onNavigateToJournal = {
                    selectedTab = MainTab.Journal
                    journalNavState = JournalNavState(JournalSubRoute.Write)
                },
                onNavigateToAi = { selectedTab = MainTab.Ai },
                modifier = Modifier.padding(innerPadding),
            )
            MainTab.Bible -> BibleScreen(
                subRoute = bibleSubRoute,
                onSubRouteChange = { bibleSubRoute = it },
                modifier = Modifier.padding(innerPadding),
            )
            MainTab.Journal -> JournalScreen(
                navState = journalNavState,
                onNavStateChange = { journalNavState = it },
                modifier = Modifier.padding(innerPadding),
            )
            MainTab.Community -> CommunityScreen(modifier = Modifier.padding(innerPadding))
            MainTab.Ai -> AiChatScreen(modifier = Modifier.padding(innerPadding))
            MainTab.Profile -> ProfileScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}

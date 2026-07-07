package com.example.biblelog.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(
    val label: String,
    val icon: ImageVector,
) {
    Home("홈", Icons.Default.Home),
    Bible("성경", Icons.Default.Star),
    Journal("묵상", Icons.Default.Edit),
    Community("공동체", Icons.Default.Favorite),
    Ai("AI", Icons.Default.Info),
    Profile("프로필", Icons.Default.Person),
}

enum class BibleSubRoute {
    Dashboard,
    AddRecord,
    Stats,
}

enum class JournalSubRoute {
    List,
    Write,
}

data class JournalNavState(
    val route: JournalSubRoute = JournalSubRoute.List,
    val editingNoteId: String? = null,
)

val JournalNavStateSaver = listSaver(
    save = { state -> listOf(state.route.name, state.editingNoteId) },
    restore = { saved ->
        JournalNavState(
            route = JournalSubRoute.valueOf(saved[0] as String),
            editingNoteId = saved[1] as String?,
        )
    },
)

val LogoutIcon = Icons.AutoMirrored.Filled.ExitToApp

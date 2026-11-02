package com.timeo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.timeo.app.ui.components.TopBar
import com.timeo.app.ui.day.DayScreen
import com.timeo.app.ui.day.DayViewModel
import com.timeo.app.ui.editor.EditorRequest
import com.timeo.app.ui.editor.EditorSheet
import com.timeo.app.ui.insights.InsightsScreen
import com.timeo.app.ui.insights.InsightsViewModel
import com.timeo.app.ui.theme.LocalTimeo
import com.timeo.app.ui.theme.TimeoTheme
import com.timeo.app.ui.theme.TimeoType
import com.timeo.app.ui.today.TodayScreen
import com.timeo.app.ui.today.TodayViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TimeoTheme { TimeoNav() }
        }
    }
}

@Composable
private fun TimeoNav() {
    val nav = rememberNavController()
    val repo = LocalContext.current.repository
    NavHost(nav, startDestination = "main") {
        composable("main") {
            MainScreen(onOpenDay = { nav.navigate("day/${it.toEpochDay()}") })
        }
        composable("day/{epochDay}", arguments = listOf(navArgument("epochDay") { type = NavType.LongType })) { entry ->
            val date = LocalDate.ofEpochDay(entry.arguments!!.getLong("epochDay"))
            val vm = viewModel(key = "day-$date") { DayViewModel(repo, date) }
            val state by vm.state.collectAsStateWithLifecycle()
            DayScreen(vm, state, onBack = { nav.popBackStack() })
        }
    }
}

@Composable
private fun MainScreen(onOpenDay: (LocalDate) -> Unit) {
    val c = LocalTimeo.current
    val repo = LocalContext.current.repository
    val today = viewModel { TodayViewModel(repo) }
    val insights = viewModel { InsightsViewModel(repo) }
    val todayState by today.state.collectAsStateWithLifecycle()
    val insightsState by insights.state.collectAsStateWithLifecycle()
    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    var editor by remember { mutableStateOf<EditorRequest?>(null) }

    Scaffold(
        containerColor = c.background,
        topBar = {
            TopBar(
                tabs = listOf("Today", "Insights"),
                selected = pager.currentPage,
                onSelect = { scope.launch { pager.animateScrollToPage(it) } },
                modifier = Modifier.background(c.background).statusBarsPadding(),
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = pager.currentPage == 0 && todayState.items.isNotEmpty(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Row(
                    Modifier
                        .background(c.text)
                        .clickable { editor = EditorRequest() }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, null, tint = c.background, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("New timer", style = TimeoType.bodyStrong, color = c.background)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            // Swiping is off so the heatmap's horizontal scroll never fights the pager.
            HorizontalPager(pager, userScrollEnabled = false, beyondViewportPageCount = 1) { page ->
                when (page) {
                    0 -> TodayScreen(
                        state = todayState,
                        onToggle = today::toggle,
                        onEdit = { editor = EditorRequest(category = it) },
                        onAdd = { editor = EditorRequest(parentId = it) },
                        onOpenDay = onOpenDay,
                        onSeed = today::seed,
                        contentPadding = padding,
                    )
                    else -> InsightsScreen(
                        state = insightsState,
                        onSelectCategory = insights::select,
                        onOpenDay = onOpenDay,
                        onWeekStart = { insights.setWeekStart(it) },
                        contentPadding = padding,
                    )
                }
            }
        }
    }

    editor?.let { req ->
        val cats = insightsState.categories.ifEmpty { todayState.items.flatMap { listOf(it.category) + it.children.map { ch -> ch.category } } }
        EditorSheet(
            request = req,
            allCategories = cats,
            onDismiss = { editor = null },
            onSave = { cat ->
                scope.launch { repo.saveCategory(cat) }
                editor = null
            },
            onDelete = { cat, withChildren ->
                scope.launch { repo.deleteCategory(cat, withChildren) }
                editor = null
            },
        )
    }
}

package dev.ujhhgtg.wekit.activity.agent

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.ujhhgtg.wekit.features.api.agent.WeAgentService
import dev.ujhhgtg.wekit.ui.agent.settings.BuiltinProvidersScreen
import dev.ujhhgtg.wekit.ui.agent.settings.ExternalServicesScreen
import dev.ujhhgtg.wekit.ui.agent.settings.McpServerDetailScreen
import dev.ujhhgtg.wekit.ui.agent.settings.McpServersScreen
import dev.ujhhgtg.wekit.ui.agent.settings.MemoryScreen
import dev.ujhhgtg.wekit.ui.agent.settings.ModelProviderDetailScreen
import dev.ujhhgtg.wekit.ui.agent.settings.ModelProvidersScreen
import dev.ujhhgtg.wekit.ui.agent.settings.PromptsScreen
import dev.ujhhgtg.wekit.ui.agent.settings.SkillsScreen
import dev.ujhhgtg.wekit.ui.agent.settings.ToolPermissionListScreen
import dev.ujhhgtg.wekit.ui.agent.settings.TriggersScreen
import dev.ujhhgtg.wekit.ui.agent.settings.WeAgentHomeScreen
import dev.ujhhgtg.wekit.ui.agent.settings.WorkspacesScreen
import dev.ujhhgtg.wekit.ui.agent.settings.builtinProviderTools
import dev.ujhhgtg.wekit.ui.navigation.NavigationTransitions
import dev.ujhhgtg.wekit.ui.utils.CommonContextWrapper
import dev.ujhhgtg.wekit.ui.utils.theme.AppUiEngine
import dev.ujhhgtg.wekit.ui.utils.theme.ModuleTheme
import dev.ujhhgtg.wekit.ui.utils.theme.ThemeSettings

/**
 * Dedicated WeAgent configuration Activity. Deliberately separate from the floating overlay:
 * the overlay stays lean while all detailed configuration (model providers, MCP servers, tool
 * permissions, prompts, workspaces, skills, global settings) lives here.
 *
 * Navigation is handled by [androidx.navigation.compose.NavHost], supporting both the Miuix and
 * Material 3 UI engines. The back-stack is managed by [rememberNavController]; predictive-back
 * and the activity-level finish() are wired automatically by the NavController.
 *
 * Named `*SettingsActivity` so [dev.ujhhgtg.wekit.loader.utils.ActivityProxy] routes it through the
 * opaque splash proxy when launched from WeChat's host process.
 */
@Keep
class WeAgentSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ensure the backend is initialized even if the overlay feature hasn't been toggled yet.
        WeAgentService.init()

        val context = CommonContextWrapper(this)
        val resources = context.resources

        setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalResources provides resources,
                LocalActivity provides this,
            ) {
                ModuleTheme {
                    WeAgentSettingsRoot(onFinish = { finish() })
                }
            }
        }
    }
}

/**
 * Typed navigation destinations for this Activity. Still referenced by screen composables
 * (e.g. [WeAgentHomeScreen]) so the sealed interface is kept; navigation itself is handled by
 * NavHost route strings via [AgentSettingsScreen.toRoute].
 */
sealed interface AgentSettingsScreen {
    data object Home : AgentSettingsScreen
    data object ModelProviders : AgentSettingsScreen
    data class ModelProviderDetail(val providerId: String) : AgentSettingsScreen
    data object BuiltinTools : AgentSettingsScreen
    data class BuiltinToolPermissions(val providerId: String, val name: String) : AgentSettingsScreen
    data object McpServers : AgentSettingsScreen
    data class McpServerDetail(val serverId: String) : AgentSettingsScreen
    data object Prompts : AgentSettingsScreen
    data object Workspaces : AgentSettingsScreen
    data object Memory : AgentSettingsScreen
    data object Skills : AgentSettingsScreen
    data object Triggers : AgentSettingsScreen
    data object ExternalServices : AgentSettingsScreen
}

/** Maps a typed [AgentSettingsScreen] destination to a NavHost route string. */
private fun AgentSettingsScreen.toRoute(): String = when (this) {
    AgentSettingsScreen.Home                    -> "home"
    AgentSettingsScreen.ModelProviders          -> "model_providers"
    is AgentSettingsScreen.ModelProviderDetail  -> "model_provider/${Uri.encode(providerId)}"
    AgentSettingsScreen.BuiltinTools            -> "builtin_tools"
    is AgentSettingsScreen.BuiltinToolPermissions ->
        "builtin_tool_permissions/${Uri.encode(providerId)}/${Uri.encode(name)}"
    AgentSettingsScreen.McpServers              -> "mcp_servers"
    is AgentSettingsScreen.McpServerDetail      -> "mcp_server/${Uri.encode(serverId)}"
    AgentSettingsScreen.Prompts                 -> "prompts"
    AgentSettingsScreen.Workspaces              -> "workspaces"
    AgentSettingsScreen.Memory                  -> "memory"
    AgentSettingsScreen.Skills                  -> "skills"
    AgentSettingsScreen.Triggers                -> "triggers"
    AgentSettingsScreen.ExternalServices        -> "external_services"
}

@Composable
private fun WeAgentSettingsRoot(onFinish: () -> Unit) {
    when (ThemeSettings.uiEngine) {
        AppUiEngine.MIUIX     -> MiuixWeAgentSettingsRoot(onFinish = onFinish)
        AppUiEngine.MATERIAL3 -> Material3WeAgentSettingsRoot(onFinish = onFinish)
    }
}

@Composable
private fun MiuixWeAgentSettingsRoot(
    @Suppress("UNUSED_PARAMETER") onFinish: () -> Unit,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition   = { NavigationTransitions.enter },
        exitTransition    = { NavigationTransitions.exit },
        popEnterTransition  = { NavigationTransitions.popEnter },
        popExitTransition   = { NavigationTransitions.popExit },
    ) {
        composable("home") {
            WeAgentHomeScreen(onOpen = { screen -> navController.navigate(screen.toRoute()) })
        }

        composable("model_providers") {
            ModelProvidersScreen(
                onBack = { navController.popBackStack() },
                onOpenProvider = { id ->
                    navController.navigate(AgentSettingsScreen.ModelProviderDetail(id).toRoute())
                },
            )
        }

        composable(
            route = "model_provider/{providerId}",
            arguments = listOf(navArgument("providerId") { type = NavType.StringType }),
        ) { entry ->
            ModelProviderDetailScreen(
                providerId = entry.arguments?.getString("providerId").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }

        composable("builtin_tools") {
            BuiltinProvidersScreen(
                onBack = { navController.popBackStack() },
                onOpenProvider = { id, name ->
                    navController.navigate(
                        AgentSettingsScreen.BuiltinToolPermissions(id, name).toRoute()
                    )
                },
            )
        }
        composable(
            route = "builtin_tool_permissions/{providerId}/{name}",
            arguments = listOf(
                navArgument("providerId") { type = NavType.StringType },
                navArgument("name")       { type = NavType.StringType },
            ),
        ) { entry ->
            val providerId = entry.arguments?.getString("providerId").orEmpty()
            ToolPermissionListScreen(
                title      = entry.arguments?.getString("name").orEmpty(),
                providerId = providerId,
                tools      = builtinProviderTools(providerId),
                onBack     = { navController.popBackStack() },
            )
        }

        composable("mcp_servers") {
            McpServersScreen(
                onBack = { navController.popBackStack() },
                onOpenServer = { id ->
                    navController.navigate(AgentSettingsScreen.McpServerDetail(id).toRoute())
                },
            )
        }
        composable(
            route = "mcp_server/{serverId}",
            arguments = listOf(navArgument("serverId") { type = NavType.StringType }),
        ) { entry ->
            McpServerDetailScreen(
                serverId = entry.arguments?.getString("serverId").orEmpty(),
                onBack   = { navController.popBackStack() },
            )
        }

        composable("prompts")          { PromptsScreen(onBack = { navController.popBackStack() }) }
        composable("workspaces")       { WorkspacesScreen(onBack = { navController.popBackStack() }) }
        composable("memory")           { MemoryScreen(onBack = { navController.popBackStack() }) }
        composable("skills")           { SkillsScreen(onBack = { navController.popBackStack() }) }
        composable("triggers")         { TriggersScreen(onBack = { navController.popBackStack() }) }
        composable("external_services"){ ExternalServicesScreen(onBack = { navController.popBackStack() }) }
    }
}

@Composable
private fun Material3WeAgentSettingsRoot(
    @Suppress("UNUSED_PARAMETER") onFinish: () -> Unit,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition   = { NavigationTransitions.enter },
        exitTransition    = { NavigationTransitions.exit },
        popEnterTransition  = { NavigationTransitions.popEnter },
        popExitTransition   = { NavigationTransitions.popExit },
    ) {
        composable("home") {
            dev.ujhhgtg.wekit.ui.agent.settings.Material3WeAgentHomeScreen(
                onOpen = { screen -> navController.navigate(screen.toRoute()) }
            )
        }

        composable("model_providers") {
            dev.ujhhgtg.wekit.ui.agent.settings.Material3ModelProvidersScreen(
                onBack = { navController.popBackStack() },
                onOpenProvider = { id -> navController.navigate(AgentSettingsScreen.ModelProviderDetail(id).toRoute()) },
            )
        }
        composable(
            route = "model_provider/{providerId}",
            arguments = listOf(navArgument("providerId") { type = NavType.StringType }),
        ) { entry ->
            dev.ujhhgtg.wekit.ui.agent.settings.Material3ModelProviderDetailScreen(
                providerId = entry.arguments?.getString("providerId").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }

        composable("builtin_tools") {
            dev.ujhhgtg.wekit.ui.agent.settings.Material3BuiltinProvidersScreen(
                onBack = { navController.popBackStack() },
                onOpenProvider = { id, name ->
                    navController.navigate(AgentSettingsScreen.BuiltinToolPermissions(id, name).toRoute())
                },
            )
        }
        composable(
            route = "builtin_tool_permissions/{providerId}/{name}",
            arguments = listOf(
                navArgument("providerId") { type = NavType.StringType },
                navArgument("name")       { type = NavType.StringType },
            ),
        ) { entry ->
            val providerId = entry.arguments?.getString("providerId").orEmpty()
            dev.ujhhgtg.wekit.ui.agent.settings.Material3ToolPermissionListScreen(
                title      = entry.arguments?.getString("name").orEmpty(),
                providerId = providerId,
                tools      = builtinProviderTools(providerId),
                onBack     = { navController.popBackStack() },
            )
        }

        composable("mcp_servers") {
            dev.ujhhgtg.wekit.ui.agent.settings.Material3McpServersScreen(
                onBack = { navController.popBackStack() },
                onOpenServer = { id -> navController.navigate(AgentSettingsScreen.McpServerDetail(id).toRoute()) },
            )
        }
        composable(
            route = "mcp_server/{serverId}",
            arguments = listOf(navArgument("serverId") { type = NavType.StringType }),
        ) { entry ->
            dev.ujhhgtg.wekit.ui.agent.settings.Material3McpServerDetailScreen(
                serverId = entry.arguments?.getString("serverId").orEmpty(),
                onBack   = { navController.popBackStack() },
            )
        }

        composable("prompts")          { dev.ujhhgtg.wekit.ui.agent.settings.Material3PromptsScreen(onBack = { navController.popBackStack() }) }
        composable("workspaces")       { dev.ujhhgtg.wekit.ui.agent.settings.Material3WorkspacesScreen(onBack = { navController.popBackStack() }) }
        composable("memory")           { dev.ujhhgtg.wekit.ui.agent.settings.Material3MemoryScreen(onBack = { navController.popBackStack() }) }
        composable("skills")           { dev.ujhhgtg.wekit.ui.agent.settings.Material3SkillsScreen(onBack = { navController.popBackStack() }) }
        composable("triggers")         { dev.ujhhgtg.wekit.ui.agent.settings.Material3TriggersScreen(onBack = { navController.popBackStack() }) }
        composable("external_services"){ dev.ujhhgtg.wekit.ui.agent.settings.Material3ExternalServicesScreen(onBack = { navController.popBackStack() }) }
    }
}

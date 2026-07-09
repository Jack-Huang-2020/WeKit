package dev.ujhhgtg.wekit.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import dev.ujhhgtg.wekit.ui.content.NavAnimationEasing

private const val DURATION_MS = 500
private const val FADE_DURATION_MS = DURATION_MS / 2

/**
 * Screen transitions for the [androidx.navigation.compose.NavHost]-based navigation used by
 * [dev.ujhhgtg.wekit.activity.settings.SettingsActivity] and
 * [dev.ujhhgtg.wekit.activity.agent.WeAgentSettingsActivity].
 *
 * - **Push** (forward): new screen slides in from the right, current screen fades out.
 * - **Pop** (back):  current screen slides out to the right, previous screen fades in.
 */
object NavigationTransitions {

    // ---- forward (push) ----

    val enter: EnterTransition =
        slideInHorizontally(
            animationSpec = tween(DURATION_MS, easing = NavAnimationEasing),
            initialOffsetX = { fullWidth -> fullWidth },
        ) + fadeIn(animationSpec = tween(FADE_DURATION_MS))

    val exit: ExitTransition =
        fadeOut(animationSpec = tween(FADE_DURATION_MS))

    // ---- backward (pop) ----

    val popEnter: EnterTransition =
        fadeIn(animationSpec = tween(FADE_DURATION_MS))

    val popExit: ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(DURATION_MS, easing = NavAnimationEasing),
            targetOffsetX = { fullWidth -> fullWidth },
        ) + fadeOut(animationSpec = tween(FADE_DURATION_MS))
}

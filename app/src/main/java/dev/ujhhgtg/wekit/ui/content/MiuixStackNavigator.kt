package dev.ujhhgtg.wekit.ui.content

import androidx.compose.animation.core.Easing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The miuix navigation3 spring easing, ported verbatim from the miuix fork's
 * `NavTransitionEasing(response = 0.8, damping = 0.95)`. It is the analytic solution of an
 * under-damped spring expressed as an [Easing], which is what gives the Miuix predictive-back
 * transition its distinctive feel.
 *
 * Shared by [SettingsActivity][dev.ujhhgtg.wekit.activity.settings.SettingsActivity] and
 * [WeAgentSettingsActivity][dev.ujhhgtg.wekit.activity.agent.WeAgentSettingsActivity].
 */
internal val NavAnimationEasing: Easing = run {
    val response = 0.8
    val damping  = 0.95
    val omega = 2.0 * PI / response
    val k = omega * omega
    val c = damping * 4.0 * PI / response
    val w = sqrt(4.0 * k - c * c) / 2.0
    val r  = -c / 2.0
    val c2 = r / w
    Easing { fraction ->
        val t = fraction.toDouble()
        val decay = exp(r * t)
        (decay * (-cos(w * t) + c2 * sin(w * t)) + 1.0).toFloat()
    }
}

package dev.ujhhgtg.wekit.features.api.net

import androidx.compose.ui.graphics.Color

/**
 * WeKit Pro subscription tier. [NONE] is the default unauthenticated state (普通用户).
 * The [serverKey] values match the `tier` field returned by the WeKit Pro API.
 */
enum class WekitProTier(
    val serverKey: String,
    val displayName: String,
) {
    NONE("none", "普通"),
    BASIC("basic", "PRO 基础版"),
    STANDARD("standard", "PRO 标准版"),
    FLAGSHIP("flagship", "PRO 旗舰版"),
    ;

    companion object {
        fun fromServerKey(key: String?): WekitProTier =
            entries.firstOrNull { it.serverKey == key } ?: NONE
    }
}

/** Background color for the tier chip in the profile card. */
val WekitProTier.chipBackground: Color
    get() = when (this) {
        WekitProTier.NONE     -> Color(0x1A888888)          // translucent gray
        WekitProTier.BASIC    -> Color(0x1A2196F3)          // translucent blue
        WekitProTier.STANDARD -> Color(0xFFFFD600)          // solid gold
        WekitProTier.FLAGSHIP -> Color(0xFFFF6B00)          // solid deep-amber/orange
    }

/** Text/icon color for the tier chip. */
val WekitProTier.chipText: Color
    get() = when (this) {
        WekitProTier.NONE     -> Color(0xFF888888)
        WekitProTier.BASIC    -> Color(0xFF1565C0)
        WekitProTier.STANDARD -> Color(0xFF6B4E00)
        WekitProTier.FLAGSHIP -> Color(0xFF7A2200)
    }

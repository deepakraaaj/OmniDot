package com.omnidot.ui.goals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omnidot.goals.GoalTier
import com.omnidot.ui.theme.TierBig
import com.omnidot.ui.theme.TierMedium
import com.omnidot.ui.theme.TierSmall

fun GoalTier.color(): Color = when (this) {
    GoalTier.SMALL -> TierSmall
    GoalTier.MEDIUM -> TierMedium
    GoalTier.BIG -> TierBig
}

@Composable
fun TierShape(tier: GoalTier, size: Dp = 12.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val c = tier.color()
        when (tier) {
            GoalTier.SMALL -> drawCircle(c)
            GoalTier.MEDIUM -> drawRoundRect(c, cornerRadius = CornerRadius(this.size.width * .24f))
            GoalTier.BIG -> drawPath(Path().apply {
                moveTo(this@Canvas.size.width / 2, 0f); lineTo(this@Canvas.size.width, this@Canvas.size.height / 2)
                lineTo(this@Canvas.size.width / 2, this@Canvas.size.height); lineTo(0f, this@Canvas.size.height / 2); close()
            }, c)
        }
    }
}

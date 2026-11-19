package com.lightning.browser.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Voltage shape scale: chips 12-16, cards 20-28, hero surfaces 44. The lobed
// and connected-list shapes live in ExpressiveShapes.

val LightningShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(44.dp),
)

package com.lightning.browser.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.lightning.browser.ui.theme.ExpressiveShapes

/** Connected list: 2dp gaps, big outer corners, small inner corners. */
@Composable
fun GroupedColumn(
    count: Int,
    modifier: Modifier = Modifier,
    item: @Composable (index: Int, shape: Shape) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(count) { i -> item(i, ExpressiveShapes.grouped(i, count)) }
    }
}
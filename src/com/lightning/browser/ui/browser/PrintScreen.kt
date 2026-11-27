package com.lightning.browser.ui.browser

import android.graphics.Bitmap
import android.print.PrintAttributes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.components.TonalIconButton
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.EyebrowTextStyle
import com.lightning.browser.ui.theme.LightningMotion
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.rememberLightningHaptics
import kotlinx.coroutines.delay
import kotlin.math.ceil

enum class PrintPaperSize(
    val labelRes: Int,
    val portraitMediaSize: PrintAttributes.MediaSize,
) {
    A4(R.string.print_paper_a4, PrintAttributes.MediaSize.ISO_A4),
    LETTER(R.string.print_paper_letter, PrintAttributes.MediaSize.NA_LETTER),
    LEGAL(R.string.print_paper_legal, PrintAttributes.MediaSize.NA_LEGAL),
    A5(R.string.print_paper_a5, PrintAttributes.MediaSize.ISO_A5),
    A3(R.string.print_paper_a3, PrintAttributes.MediaSize.ISO_A3),
    B5(R.string.print_paper_b5, PrintAttributes.MediaSize.ISO_B5),
    TABLOID(R.string.print_paper_tabloid, PrintAttributes.MediaSize.NA_TABLOID),
    PHOTO_4X6(R.string.print_paper_4x6, PrintAttributes.MediaSize.NA_INDEX_4X6),
    ;

    fun portraitAspectRatio(): Float =
        portraitMediaSize.widthMils.toFloat() / portraitMediaSize.heightMils.toFloat()
}

private enum class PrintOrientation(val labelRes: Int) {
    PORTRAIT(R.string.print_orientation_portrait),
    LANDSCAPE(R.string.print_orientation_landscape),
}

private enum class PrintColor(val labelRes: Int) {
    COLOR(R.string.print_color_full),
    MONOCHROME(R.string.print_color_mono),
}

private enum class PrintPageRangeMode(val labelRes: Int) {
    ALL(R.string.print_pages_all),
    CUSTOM(R.string.print_pages_custom),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrintScreen(
    pageTitle: String,
    pageDomain: String,
    previewBitmap: Bitmap?,
    onBack: () -> Unit,
    onSavePdf: (PrintAttributes.MediaSize, Boolean, Set<Int>) -> Unit,
    onPrint: (PrintAttributes) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    val haptics = rememberLightningHaptics()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    var wasImeVisible by remember { mutableStateOf(false) }

    LaunchedEffect(imeVisible) {
        if (wasImeVisible && !imeVisible) {
            focusManager.clearFocus()
        }
        wasImeVisible = imeVisible
    }

    var paper by remember { mutableStateOf(PrintPaperSize.A4) }
    var orientation by remember { mutableStateOf(PrintOrientation.PORTRAIT) }
    var colorMode by remember { mutableStateOf(PrintColor.COLOR) }
    var copies by remember { mutableIntStateOf(1) }
    var pageRangeMode by remember { mutableStateOf(PrintPageRangeMode.ALL) }
    var customRangeText by remember { mutableStateOf("1") }
    var currentPage by remember { mutableIntStateOf(1) }
    var selectedPrinter by remember { mutableStateOf("pdf") }
    var showPrinterMenu by remember { mutableStateOf(false) }
    var searchingPrinters by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2200L)
        searchingPrinters = false
    }

    val aspectRatio = remember(paper, orientation) {
        val portrait = paper.portraitAspectRatio()
        if (orientation == PrintOrientation.LANDSCAPE) 1f / portrait else portrait
    }

    val totalPages = remember(previewBitmap, aspectRatio) {
        if (previewBitmap == null || previewBitmap.width <= 0) {
            1
        } else {
            val pageSliceHeight = (previewBitmap.width / aspectRatio).coerceAtLeast(1f)
            ceil(previewBitmap.height / pageSliceHeight).toInt().coerceIn(1, 20)
        }
    }

    LaunchedEffect(totalPages) {
        if (currentPage > totalPages) currentPage = totalPages
        if (pageRangeMode == PrintPageRangeMode.ALL) {
            customRangeText = if (totalPages > 1) "1-$totalPages" else "1"
        }
    }

    val selectedPages = remember(pageRangeMode, customRangeText, totalPages) {
        if (pageRangeMode == PrintPageRangeMode.ALL) {
            (1..totalPages).toSet()
        } else {
            parsePageRange(customRangeText, totalPages)
        }
    }

    val pageBitmap = remember(previewBitmap, currentPage, totalPages, aspectRatio) {
        slicePreviewPage(previewBitmap, currentPage, aspectRatio)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            TonalIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
                size = 44.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.print_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                )
                Text(
                    text = pageTitle.ifBlank { pageDomain },
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                Surface(
                    onClick = { showPrinterMenu = true },
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceContainerHigh,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = if (selectedPrinter == "pdf") {
                                Icons.Filled.PictureAsPdf
                            } else {
                                Icons.Filled.Print
                            },
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(
                                if (selectedPrinter == "pdf") {
                                    R.string.print_printer_pdf
                                } else {
                                    R.string.print_printer_all
                                },
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurface,
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                DropdownMenu(
                    expanded = showPrinterMenu,
                    onDismissRequest = { showPrinterMenu = false },
                    containerColor = colors.surfaceContainerHigh,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.print_printer_pdf)) },
                        leadingIcon = {
                            Icon(Icons.Filled.PictureAsPdf, null, tint = colors.primary)
                        },
                        onClick = {
                            selectedPrinter = "pdf"
                            showPrinterMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(stringResource(R.string.print_printer_all))
                                Text(
                                    text = stringResource(
                                        if (searchingPrinters) {
                                            R.string.print_looking_for_printers
                                        } else {
                                            R.string.print_printer_system_hint
                                        },
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        },
                        leadingIcon = {
                            if (searchingPrinters) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = colors.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            } else {
                                Icon(Icons.Filled.Print, null, tint = colors.onSurfaceVariant)
                            }
                        },
                        onClick = {
                            selectedPrinter = "all"
                            showPrinterMenu = false
                        },
                    )
                }
            }
        }

        if (searchingPrinters) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 2.dp),
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = colors.primary,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.print_looking_for_printers),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            PrintPaperPreview(
                pageBitmap = pageBitmap,
                aspectRatio = aspectRatio,
                monochrome = colorMode == PrintColor.MONOCHROME,
                included = currentPage in selectedPages,
                pageTitle = pageTitle.ifBlank { pageDomain },
            )

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TonalIconButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.find_in_page_prev),
                    onClick = { if (currentPage > 1) currentPage-- },
                    enabled = currentPage > 1,
                    size = 40.dp,
                )
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = CircleShape,
                    color = colors.surfaceContainerHigh,
                ) {
                    Text(
                        text = stringResource(R.string.print_page_indicator, currentPage, totalPages),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                TonalIconButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.find_in_page_next),
                    onClick = { if (currentPage < totalPages) currentPage++ },
                    enabled = currentPage < totalPages,
                    size = 40.dp,
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerHigh, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = stringResource(R.string.print_copies_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TonalIconButton(
                        icon = Icons.Filled.Remove,
                        contentDescription = "",
                        onClick = {
                            if (copies > 1) {
                                haptics.tick()
                                copies--
                            }
                        },
                        enabled = copies > 1,
                        size = 36.dp,
                        iconSize = 18.dp,
                    )
                    Text(
                        text = copies.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                    TonalIconButton(
                        icon = Icons.Filled.Add,
                        contentDescription = "",
                        onClick = {
                            if (copies < 99) {
                                haptics.tick()
                                copies++
                            }
                        },
                        enabled = copies < 99,
                        size = 36.dp,
                        iconSize = 18.dp,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.print_paper_label),
                style = EyebrowTextStyle,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                PrintPaperSize.entries.forEach { size ->
                    val isSelected = size == paper
                    Surface(
                        onClick = {
                            haptics.tick()
                            paper = size
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) colors.primary else colors.surfaceContainerHigh,
                    ) {
                        Text(
                            text = stringResource(size.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.print_orientation_label),
                style = EyebrowTextStyle,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
            ExpressiveSegmentedPicker(
                items = PrintOrientation.entries,
                selected = orientation,
                labelOf = { stringResource(it.labelRes) },
                onSelect = {
                    haptics.tick()
                    orientation = it
                },
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.print_color_label),
                style = EyebrowTextStyle,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
            ExpressiveSegmentedPicker(
                items = PrintColor.entries,
                selected = colorMode,
                labelOf = { stringResource(it.labelRes) },
                onSelect = {
                    haptics.tick()
                    colorMode = it
                },
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.print_pages_label),
                style = EyebrowTextStyle,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
            ExpressiveSegmentedPicker(
                items = PrintPageRangeMode.entries,
                selected = pageRangeMode,
                labelOf = { stringResource(it.labelRes) },
                onSelect = {
                    haptics.tick()
                    pageRangeMode = it
                },
            )

            AnimatedVisibility(visible = pageRangeMode == PrintPageRangeMode.CUSTOM) {
                TextField(
                    value = customRangeText,
                    onValueChange = { customRangeText = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.print_pages_hint),
                            color = colors.onSurfaceVariant,
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = colors.onSurface,
                        fontWeight = FontWeight.Medium,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        keyboard?.hide()
                    }),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceContainerHigh,
                        unfocusedContainerColor = colors.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = colors.primary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        Surface(
            color = colors.surfaceContainerLow,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = {
                    val baseSize = paper.portraitMediaSize
                    val mediaSize = if (orientation == PrintOrientation.LANDSCAPE) {
                        baseSize.asLandscape()
                    } else {
                        baseSize.asPortrait()
                    }
                    val isMono = colorMode == PrintColor.MONOCHROME
                    onBack()
                    if (selectedPrinter == "pdf") {
                        onSavePdf(mediaSize, isMono, selectedPages)
                    } else {
                        val colorAttr = if (isMono) {
                            PrintAttributes.COLOR_MODE_MONOCHROME
                        } else {
                            PrintAttributes.COLOR_MODE_COLOR
                        }
                        val attrs = PrintAttributes.Builder()
                            .setMediaSize(mediaSize)
                            .setColorMode(colorAttr)
                            .build()
                        onPrint(attrs)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .height(52.dp),
            ) {
                Icon(
                    imageVector = if (selectedPrinter == "pdf") Icons.Filled.PictureAsPdf else Icons.Filled.Print,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(
                        if (selectedPrinter == "pdf") {
                            R.string.print_action_save_pdf
                        } else {
                            R.string.print_action_continue
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun PrintPaperPreview(
    pageBitmap: Bitmap?,
    aspectRatio: Float,
    monochrome: Boolean,
    included: Boolean,
    pageTitle: String,
) {
    val colors = LightningTheme.colors
    val animatedRatio by animateFloatAsState(
        targetValue = aspectRatio,
        animationSpec = LightningMotion.defaultSpatial(),
        label = "paperRatio",
    )
    val monoFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(colors.surfaceContainerHigh, RoundedCornerShape(28.dp))
            .padding(16.dp),
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                width = if (included) 1.5.dp else 1.dp,
                color = if (included) colors.primary.copy(alpha = 0.5f) else colors.outline.copy(alpha = 0.25f),
            ),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(animatedRatio),
        ) {
            Box(Modifier.fillMaxSize()) {
                if (pageBitmap != null) {
                    val img = remember(pageBitmap) { pageBitmap.asImageBitmap() }
                    Image(
                        bitmap = img,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        colorFilter = if (monochrome) monoFilter else null,
                        alpha = if (included) 1f else 0.3f,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = pageTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (it == 3) 0.6f else 1f)
                                    .height(6.dp)
                                    .background(Color(0xFFE0E0E0), RoundedCornerShape(3.dp)),
                            )
                        }
                    }
                }
                if (!included) {
                    Surface(
                        color = colors.surfaceContainerHighest.copy(alpha = 0.9f),
                        shape = CircleShape,
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        Text(
                            text = stringResource(R.string.print_page_excluded),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> ExpressiveSegmentedPicker(
    items: List<T>,
    selected: T,
    labelOf: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    val colors = LightningTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
    ) {
        items.forEachIndexed { i, item ->
            val isSelected = item == selected
            val inner by animateDpAsState(
                if (isSelected) 23.dp else 12.dp,
                LightningMotion.defaultSpatial(),
                label = "printSeg",
            )
            val last = items.lastIndex
            val shape = RoundedCornerShape(
                topStart = if (i == 0) 23.dp else inner,
                bottomStart = if (i == 0) 23.dp else inner,
                topEnd = if (i == last) 23.dp else inner,
                bottomEnd = if (i == last) 23.dp else inner,
            )
            Surface(
                onClick = { onSelect(item) },
                shape = shape,
                color = if (isSelected) colors.primary else colors.surfaceContainerHigh,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = labelOf(item),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun slicePreviewPage(source: Bitmap?, page: Int, aspectRatio: Float): Bitmap? {
    if (source == null || source.width <= 0 || source.height <= 0) return null
    val sliceHeight = (source.width / aspectRatio).toInt().coerceAtLeast(1)
    val startY = ((page - 1) * sliceHeight).coerceIn(0, (source.height - 1).coerceAtLeast(0))
    val height = sliceHeight.coerceAtMost(source.height - startY).coerceAtLeast(1)
    return runCatching {
        Bitmap.createBitmap(source, 0, startY, source.width, height)
    }.getOrNull()
}

private fun parsePageRange(input: String, maxPages: Int): Set<Int> {
    val result = mutableSetOf<Int>()
    input.split(",").forEach { part ->
        val trimmed = part.trim()
        if (trimmed.contains("-")) {
            val bounds = trimmed.split("-", limit = 2)
            val start = bounds.getOrNull(0)?.trim()?.toIntOrNull()
            val end = bounds.getOrNull(1)?.trim()?.toIntOrNull()
            if (start != null && end != null) {
                val lo = minOf(start, end).coerceIn(1, maxPages)
                val hi = maxOf(start, end).coerceIn(1, maxPages)
                for (p in lo..hi) result.add(p)
            }
        } else {
            trimmed.toIntOrNull()?.let { p ->
                if (p in 1..maxPages) result.add(p)
            }
        }
    }
    return result
}

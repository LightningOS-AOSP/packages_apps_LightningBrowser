package com.lightning.browser.ui.browser

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.components.MorphLoader
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.components.TonalIconButton
import com.lightning.browser.ui.components.pressScale
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.EyebrowTextStyle
import com.lightning.browser.ui.theme.LightningMotion
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.rememberLightningHaptics
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    title: String,
    sourceUrl: String?,
    pdfFile: File?,
    isLoading: Boolean,
    loadError: Boolean,
    initialPage: Int,
    nightMode: Boolean,
    onToggleNightMode: () -> Unit,
    onDocumentLoaded: (Int) -> Unit,
    onPageChanged: (Int) -> Unit,
    onSaveToDownloads: () -> Unit,
    onShare: () -> Unit,
    onOpenExternal: () -> Unit,
    onPrint: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    val haptics = rememberLightningHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var documentSource by remember(pdfFile) { mutableStateOf<PdfDocumentSource?>(null) }
    var openError by remember(pdfFile) { mutableStateOf(false) }
    var isEncryptedError by remember(pdfFile) { mutableStateOf(false) }

    val latestOnDocumentLoaded by rememberUpdatedState(onDocumentLoaded)
    val latestOnPageChanged by rememberUpdatedState(onPageChanged)

    DisposableEffect(pdfFile) {
        openError = false
        isEncryptedError = false
        val holder = if (pdfFile != null && pdfFile.exists()) {
            try {
                PdfDocumentSource(pdfFile)
            } catch (_: SecurityException) {
                isEncryptedError = true
                null
            } catch (_: Throwable) {
                openError = true
                null
            }
        } else {
            null
        }
        documentSource = holder
        if (holder != null && holder.pageCount > 0) {
            latestOnDocumentLoaded(holder.pageCount)
        }
        onDispose {
            holder?.close()
            if (documentSource === holder) {
                documentSource = null
            }
        }
    }

    val listState = rememberLazyListState()
    val pageCount = documentSource?.pageCount ?: 0
    val currentPage by remember(pageCount) {
        derivedStateOf {
            if (pageCount == 0) 0 else (listState.firstVisibleItemIndex + 1).coerceIn(1, pageCount)
        }
    }
    val latestCurrentPage by rememberUpdatedState(currentPage)

    val startPage = remember(pdfFile) { initialPage }
    var appliedInitialScroll by remember(pdfFile) { mutableStateOf(false) }

    LaunchedEffect(documentSource, pageCount) {
        if (documentSource != null && pageCount > 0 && !appliedInitialScroll) {
            appliedInitialScroll = true
            val target = startPage.coerceIn(1, pageCount)
            if (target > 1) {
                listState.scrollToItem(target - 1)
            }
        }
    }

    LaunchedEffect(listState, pageCount) {
        if (pageCount <= 0) return@LaunchedEffect
        snapshotFlow { currentPage }
            .collectLatest { page ->
                if (page > 0) {
                    delay(180L)
                    latestOnPageChanged(page)
                }
            }
    }

    DisposableEffect(pdfFile) {
        onDispose {
            if (latestCurrentPage > 0) {
                latestOnPageChanged(latestCurrentPage)
            }
        }
    }

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var showMenu by remember { mutableStateOf(false) }
    var showGoToPage by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    BackHandler(enabled = showGoToPage) { showGoToPage = false }
    BackHandler(enabled = showInfo) { showInfo = false }

    fun clampOffsets(nextScale: Float, rawX: Float, rawY: Float): Pair<Float, Float> {
        if (nextScale <= 1f || viewportSize.width <= 0 || viewportSize.height <= 0) {
            return 0f to 0f
        }
        val maxX = viewportSize.width * (nextScale - 1f) / 2f
        val maxY = viewportSize.height * (nextScale - 1f) / 2f
        return rawX.coerceIn(-maxX, maxX) to rawY.coerceIn(-maxY, maxY)
    }

    fun applyScaleCentered(targetScale: Float) {
        val nextScale = targetScale.coerceIn(0.35f, 5f)
        val factor = if (scale > 0f) nextScale / scale else 1f
        val (clampedX, clampedY) = clampOffsets(nextScale, offsetX * factor, offsetY * factor)
        scale = nextScale
        offsetX = clampedX
        offsetY = clampedY
    }

    fun goToPage(page: Int) {
        if (pageCount <= 0) return
        val clamped = page.coerceIn(1, pageCount)
        scope.launch {
            listState.animateScrollToItem(clamped - 1)
        }
    }

    val paddingPx = with(density) { 24.dp.toPx() }
    val baseWidthPx = ((viewportSize.width - paddingPx).roundToInt()).coerceAtLeast(480)
    val renderWidthPx = remember(baseWidthPx, scale) {
        val multiplier = when {
            scale <= 1.25f -> 1f
            scale <= 2f -> 1.5f
            scale <= 3f -> 2f
            else -> 2.5f
        }
        (baseWidthPx * multiplier).roundToInt().coerceIn(480, 2048)
    }

    val fitPageScale = remember(documentSource, viewportSize, baseWidthPx) {
        val src = documentSource
        if (src == null || viewportSize.height <= 0 || baseWidthPx <= 0) {
            1f
        } else {
            val pageHeightPx = baseWidthPx / src.firstPageAspect.coerceAtLeast(0.1f)
            (viewportSize.height / pageHeightPx).coerceIn(0.35f, 1f)
        }
    }
    val isFitPageActive = kotlin.math.abs(scale - fitPageScale) < 0.03f && fitPageScale < 0.97f

    val canActOnDoc = pdfFile != null && pageCount > 0 && !isLoading && !loadError && !openError && !isEncryptedError

    val nightFilter = remember {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
    }

    if (showGoToPage && pageCount > 0) {
        GoToPageDialog(
            currentPage = currentPage,
            pageCount = pageCount,
            onDismiss = { showGoToPage = false },
            onConfirm = { target ->
                haptics.confirm()
                showGoToPage = false
                goToPage(target)
            },
        )
    }

    if (showInfo && pdfFile != null && documentSource != null) {
        val infoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showInfo = false },
            sheetState = infoSheetState,
            containerColor = colors.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            PdfDocumentInfoSheetContent(
                title = title.ifBlank { "PDF" },
                sourceUrl = sourceUrl,
                fileSizeBytes = pdfFile.length(),
                pageCount = pageCount,
                firstPageSizePoints = documentSource!!.pageSizePoints(0),
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        Surface(
            color = colors.surfaceContainerLow,
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                TonalIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    onClick = onBack,
                    size = 44.dp,
                    tint = colors.onSurface,
                )
                Spacer(Modifier.width(10.dp))
                ShapedIcon(
                    icon = Icons.Filled.PictureAsPdf,
                    shape = ExpressiveShapes.Cookie9,
                    container = colors.primaryContainer,
                    content = colors.onPrimaryContainer,
                    size = 38.dp,
                    iconSize = 20.dp,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title.ifBlank { "PDF" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (pageCount > 0) {
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            onClick = {
                                haptics.tick()
                                showGoToPage = true
                            },
                            shape = CircleShape,
                            color = colors.surfaceContainerHigh,
                        ) {
                            Text(
                                text = stringResource(R.string.pdf_viewer_page_of, currentPage, pageCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                if (canActOnDoc) {
                    TonalIconButton(
                        icon = Icons.Filled.FileDownload,
                        contentDescription = stringResource(R.string.pdf_viewer_save),
                        onClick = {
                            haptics.tick()
                            onSaveToDownloads()
                        },
                        size = 42.dp,
                        tint = colors.onSurface,
                    )
                }
                Box {
                    TonalIconButton(
                        icon = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.pdf_viewer_more),
                        onClick = {
                            haptics.tick()
                            showMenu = true
                        },
                        size = 42.dp,
                        tint = colors.onSurface,
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = colors.surfaceContainerHigh,
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pdf_viewer_share)) },
                            leadingIcon = { Icon(Icons.Filled.Share, null, tint = colors.onSurface) },
                            enabled = canActOnDoc,
                            onClick = {
                                showMenu = false
                                onShare()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pdf_viewer_open_external)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = colors.onSurface) },
                            enabled = pdfFile != null && !isLoading && !loadError && !openError && !isEncryptedError,
                            onClick = {
                                showMenu = false
                                onOpenExternal()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pdf_viewer_print)) },
                            leadingIcon = { Icon(Icons.Filled.Print, null, tint = colors.onSurface) },
                            enabled = canActOnDoc,
                            onClick = {
                                showMenu = false
                                onPrint()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pdf_viewer_night_mode)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.DarkMode,
                                    null,
                                    tint = if (nightMode) colors.primary else colors.onSurface,
                                )
                            },
                            trailingIcon = {
                                if (nightMode) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.primaryContainer,
                                        modifier = Modifier.size(24.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Filled.Check,
                                                null,
                                                tint = colors.onPrimaryContainer,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                haptics.tick()
                                showMenu = false
                                onToggleNightMode()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pdf_viewer_info)) },
                            leadingIcon = { Icon(Icons.Filled.Info, null, tint = colors.onSurface) },
                            enabled = canActOnDoc,
                            onClick = {
                                showMenu = false
                                showInfo = true
                            },
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .onSizeChanged { viewportSize = it }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            haptics.tick()
                            if (scale > 1.1f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                val targetScale = 2f
                                val centerX = viewportSize.width / 2f
                                val centerY = viewportSize.height / 2f
                                val factor = targetScale / scale
                                val rawX = offsetX * factor + (tapOffset.x - centerX) * (1f - factor)
                                val rawY = offsetY * factor + (tapOffset.y - centerY) * (1f - factor)
                                val (cx, cy) = clampOffsets(targetScale, rawX, rawY)
                                scale = targetScale
                                offsetX = cx
                                offsetY = cy
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val pressedCount = event.changes.count { it.pressed }
                            if (pressedCount >= 2) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = true)
                                val nextScale = (scale * zoom).coerceIn(0.35f, 5f)
                                val factor = if (scale > 0f) nextScale / scale else 1f
                                val centerX = viewportSize.width / 2f
                                val centerY = viewportSize.height / 2f
                                val rawX = (offsetX + pan.x) * factor + (centroid.x - centerX) * (1f - factor)
                                val rawY = (offsetY + pan.y) * factor + (centroid.y - centerY) * (1f - factor)
                                val (cx, cy) = clampOffsets(nextScale, rawX, rawY)
                                scale = nextScale
                                offsetX = cx
                                offsetY = cy
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (scale > 1f) {
                                val pan = event.calculatePan()
                                if (pan.x != 0f || pan.y != 0f) {
                                    val (cx, cy) = clampOffsets(scale, offsetX + pan.x, offsetY + pan.y)
                                    offsetX = cx
                                    offsetY = cy
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        MorphLoader(
                            description = stringResource(R.string.pdf_viewer_loading),
                            size = 88.dp,
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = stringResource(R.string.pdf_viewer_loading),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                    }
                }
                isEncryptedError -> {
                    ExpressivePdfStateCard(
                        icon = Icons.Filled.Lock,
                        message = stringResource(R.string.pdf_viewer_encrypted),
                        actionLabel = null,
                        onAction = null,
                    )
                }
                loadError || openError || documentSource == null -> {
                    ExpressivePdfStateCard(
                        icon = Icons.Filled.ErrorOutline,
                        message = stringResource(R.string.pdf_viewer_error),
                        actionLabel = stringResource(R.string.pdf_viewer_retry),
                        onAction = {
                            haptics.tick()
                            onRetry()
                        },
                    )
                }
                else -> {
                    val holder = documentSource!!
                    val visualScale = scale.coerceAtLeast(1f)
                    val cardFraction = scale.coerceIn(0.35f, 1f)
                    val baseWidthDp = with(density) { baseWidthPx.toDp() }
                    val cardWidthDp = baseWidthDp * cardFraction
                    val pageSpacing = (14f * cardFraction.coerceAtLeast(0.6f)).dp
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = visualScale
                                scaleY = visualScale
                                translationX = offsetX
                                translationY = offsetY
                            },
                    ) {
                        LazyColumn(
                            state = listState,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(pageSpacing),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(count = holder.pageCount, key = { it }) { pageIndex ->
                                PdfPageCard(
                                    holder = holder,
                                    pageIndex = pageIndex,
                                    renderWidthPx = renderWidthPx,
                                    nightMode = nightMode,
                                    nightFilter = nightFilter,
                                    modifier = Modifier.width(cardWidthDp),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (canActOnDoc) {
            PdfBottomToolbar(
                currentPage = currentPage,
                pageCount = pageCount,
                scale = scale,
                isFitPage = isFitPageActive,
                onPrevPage = {
                    haptics.tick()
                    goToPage(currentPage - 1)
                },
                onNextPage = {
                    haptics.tick()
                    goToPage(currentPage + 1)
                },
                onPagePillClick = {
                    haptics.tick()
                    showGoToPage = true
                },
                onScrubFinished = { target ->
                    haptics.tick()
                    goToPage(target)
                },
                onZoomOut = {
                    haptics.tick()
                    applyScaleCentered(scale - 0.25f)
                },
                onZoomReset = {
                    haptics.tick()
                    applyScaleCentered(1f)
                },
                onZoomIn = {
                    haptics.tick()
                    applyScaleCentered(scale + 0.25f)
                },
                onToggleFit = {
                    haptics.tick()
                    if (isFitPageActive) {
                        applyScaleCentered(1f)
                    } else {
                        applyScaleCentered(fitPageScale)
                    }
                },
            )
        }
    }
}

@Composable
private fun ExpressivePdfStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    val colors = LightningTheme.colors
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = Modifier
            .padding(horizontal = 28.dp)
            .fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            ShapedIcon(
                icon = icon,
                shape = ExpressiveShapes.Burst8,
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
                size = 64.dp,
                iconSize = 28.dp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onAction,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfBottomToolbar(
    currentPage: Int,
    pageCount: Int,
    scale: Float,
    isFitPage: Boolean,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onPagePillClick: () -> Unit,
    onScrubFinished: (Int) -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    onZoomIn: () -> Unit,
    onToggleFit: () -> Unit,
) {
    val colors = LightningTheme.colors
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(currentPage.toFloat()) }

    LaunchedEffect(currentPage, scrubbing) {
        if (!scrubbing) {
            scrubValue = currentPage.toFloat()
        }
    }

    val displayedPage = if (scrubbing) scrubValue.roundToInt().coerceIn(1, pageCount) else currentPage

    val fitContainer by animateColorAsState(
        if (isFitPage) colors.primary else colors.surfaceContainerHigh,
        LightningMotion.defaultEffects(),
        label = "fitBg",
    )
    val fitContent by animateColorAsState(
        if (isFitPage) colors.onPrimary else colors.onSurfaceVariant,
        LightningMotion.defaultEffects(),
        label = "fitFg",
    )
    val fitRadius by animateDpAsState(
        if (isFitPage) 18.dp else 12.dp,
        LightningMotion.defaultSpatial(),
        label = "fitRadius",
    )

    val pagePillSource = remember { MutableInteractionSource() }
    val zoomPillSource = remember { MutableInteractionSource() }

    Surface(
        color = colors.surfaceContainerLow,
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (pageCount > 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                ) {
                    Slider(
                        value = scrubValue.coerceIn(1f, pageCount.toFloat()),
                        onValueChange = {
                            scrubbing = true
                            scrubValue = it
                        },
                        onValueChangeFinished = {
                            val target = scrubValue.roundToInt().coerceIn(1, pageCount)
                            scrubbing = false
                            onScrubFinished(target)
                        },
                        valueRange = 1f..pageCount.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = colors.surfaceContainerHighest,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp),
                    )
                    AnimatedVisibility(
                        visible = scrubbing,
                        enter = fadeIn() + scaleIn(initialScale = 0.85f),
                        exit = fadeOut() + scaleOut(targetScale = 0.85f),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.primaryContainer,
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Text(
                                text = displayedPage.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.surfaceContainerHigh,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        TonalIconButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.find_in_page_prev),
                            onClick = onPrevPage,
                            enabled = currentPage > 1,
                            size = 36.dp,
                            iconSize = 20.dp,
                            tint = if (currentPage > 1) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                        Surface(
                            onClick = onPagePillClick,
                            interactionSource = pagePillSource,
                            shape = CircleShape,
                            color = colors.surfaceContainerHighest,
                            modifier = Modifier.pressScale(pagePillSource),
                        ) {
                            Text(
                                text = stringResource(R.string.pdf_viewer_page_pill, displayedPage, pageCount),
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                        TonalIconButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.find_in_page_next),
                            onClick = onNextPage,
                            enabled = currentPage < pageCount,
                            size = 36.dp,
                            iconSize = 20.dp,
                            tint = if (currentPage < pageCount) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceContainerHigh,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        ) {
                            TonalIconButton(
                                icon = Icons.Filled.Remove,
                                contentDescription = stringResource(R.string.pdf_viewer_zoom_out),
                                onClick = onZoomOut,
                                enabled = scale > 0.36f,
                                size = 34.dp,
                                iconSize = 18.dp,
                                tint = if (scale > 0.36f) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                            Surface(
                                onClick = onZoomReset,
                                interactionSource = zoomPillSource,
                                shape = CircleShape,
                                color = colors.surfaceContainerHighest,
                                modifier = Modifier.pressScale(zoomPillSource),
                            ) {
                                Text(
                                    text = "${(scale * 100f).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurface,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                )
                            }
                            TonalIconButton(
                                icon = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.pdf_viewer_zoom_in),
                                onClick = onZoomIn,
                                enabled = scale < 4.99f,
                                size = 34.dp,
                                iconSize = 18.dp,
                                tint = if (scale < 4.99f) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                        }
                    }

                    Surface(
                        onClick = onToggleFit,
                        shape = RoundedCornerShape(fitRadius),
                        color = fitContainer,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.FitScreen,
                                contentDescription = stringResource(
                                    if (isFitPage) R.string.pdf_viewer_fit_width else R.string.pdf_viewer_fit_page,
                                ),
                                tint = fitContent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPageCard(
    holder: PdfDocumentSource,
    pageIndex: Int,
    renderWidthPx: Int,
    nightMode: Boolean,
    nightFilter: ColorFilter,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    val rendered by produceState<RenderedPdfPage?>(initialValue = null, holder, pageIndex, renderWidthPx) {
        value = holder.renderPage(pageIndex, renderWidthPx)
    }

    val page = rendered
    DisposableEffect(page) {
        onDispose {
            page?.bitmap?.let { bmp ->
                if (!bmp.isRecycled) bmp.recycle()
            }
        }
    }

    val cardColor = if (nightMode) Color.Black else Color.White

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.35f)),
        shadowElevation = 3.dp,
        modifier = modifier
            .then(
                if (page != null) {
                    Modifier.aspectRatio(page.width.toFloat() / page.height.toFloat().coerceAtLeast(1f))
                } else {
                    Modifier.aspectRatio(holder.pageAspect(pageIndex).coerceIn(0.4f, 2f))
                },
            ),
    ) {
        if (page != null && !page.bitmap.isRecycled) {
            val imageBitmap = remember(page) { page.bitmap.asImageBitmap() }
            Image(
                bitmap = imageBitmap,
                contentDescription = stringResource(R.string.pdf_viewer_page_description, pageIndex + 1),
                contentScale = ContentScale.FillWidth,
                colorFilter = if (nightMode) nightFilter else null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cardColor),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = colors.primary,
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 2.5.dp,
                )
            }
        }
    }
}

@Composable
private fun GoToPageDialog(
    currentPage: Int,
    pageCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val colors = LightningTheme.colors
    var input by remember { mutableStateOf(currentPage.toString()) }
    val parsed = input.trim().toIntOrNull()
    val valid = parsed != null && parsed in 1..pageCount

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = stringResource(R.string.pdf_viewer_go_to_page),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { next ->
                    if (next.length <= 6 && next.all { it.isDigit() }) {
                        input = next
                    }
                },
                label = { Text(stringResource(R.string.pdf_viewer_page_number, pageCount)) },
                singleLine = true,
                isError = input.isNotEmpty() && !valid,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (valid && parsed != null) onConfirm(parsed) },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                enabled = valid,
                shape = CircleShape,
                onClick = { if (valid && parsed != null) onConfirm(parsed) },
            ) {
                Text(stringResource(R.string.pdf_viewer_go))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pdf_viewer_cancel))
            }
        },
    )
}

@Composable
private fun PdfDocumentInfoSheetContent(
    title: String,
    sourceUrl: String?,
    fileSizeBytes: Long,
    pageCount: Int,
    firstPageSizePoints: Pair<Int, Int>,
) {
    val colors = LightningTheme.colors
    val context = LocalContext.current
    val formattedSize = remember(fileSizeBytes) { Formatter.formatShortFileSize(context, fileSizeBytes) }
    val (wPt, hPt) = firstPageSizePoints
    val wMm = (wPt * 25.4 / 72.0).roundToInt()
    val hMm = (hPt * 25.4 / 72.0).roundToInt()
    val dimText = String.format(Locale.US, "%d × %d pt (%d × %d mm)", wPt, hPt, wMm, hMm)

    val rowCount = if (!sourceUrl.isNullOrBlank()) 4 else 3

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            ShapedIcon(
                icon = Icons.Filled.PictureAsPdf,
                shape = ExpressiveShapes.Cookie9,
                container = colors.primaryContainer,
                content = colors.onPrimaryContainer,
                size = 48.dp,
                iconSize = 24.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.pdf_viewer_info).uppercase(),
                    style = EyebrowTextStyle,
                    color = colors.primary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            InfoRow(
                label = stringResource(R.string.pdf_viewer_info_pages),
                value = pageCount.toString(),
                shape = ExpressiveShapes.grouped(0, rowCount),
            )
            InfoRow(
                label = stringResource(R.string.pdf_viewer_info_size),
                value = formattedSize,
                shape = ExpressiveShapes.grouped(1, rowCount),
            )
            InfoRow(
                label = stringResource(R.string.pdf_viewer_info_page_size),
                value = dimText,
                shape = ExpressiveShapes.grouped(2, rowCount),
            )
            if (!sourceUrl.isNullOrBlank()) {
                Surface(
                    shape = ExpressiveShapes.grouped(3, rowCount),
                    color = colors.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = stringResource(R.string.pdf_viewer_info_source),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        SelectionContainer {
                            Text(
                                text = sourceUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    shape: androidx.compose.ui.graphics.Shape,
) {
    val colors = LightningTheme.colors
    Surface(
        shape = shape,
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
        }
    }
}

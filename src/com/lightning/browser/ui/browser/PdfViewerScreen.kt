package com.lightning.browser.ui.browser

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.components.TonalIconButton
import com.lightning.browser.ui.theme.LightningTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Composable
fun PdfViewerScreen(
    title: String,
    pdfFile: File?,
    isLoading: Boolean,
    loadError: Boolean,
    onSaveToDownloads: () -> Unit,
    onOpenExternal: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    var rendererHolder by remember(pdfFile) { mutableStateOf<PdfRendererHolder?>(null) }
    var openError by remember(pdfFile) { mutableStateOf(false) }

    DisposableEffect(pdfFile) {
        val holder = if (pdfFile != null && pdfFile.exists()) {
            runCatching {
                val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                PdfRendererHolder(fd, renderer)
            }.onFailure { openError = true }.getOrNull()
        } else {
            null
        }
        rendererHolder = holder
        onDispose {
            holder?.close()
        }
    }

    val listState = rememberLazyListState()
    val pageCount = rendererHolder?.pageCount ?: 0
    val currentPage by remember(pageCount) {
        derivedStateOf {
            if (pageCount == 0) 0 else (listState.firstVisibleItemIndex + 1).coerceIn(1, pageCount)
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = nextScale
        if (nextScale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
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
                    tint = colors.onSurface,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title.ifBlank { "PDF" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (pageCount > 0) {
                        Text(
                            text = stringResource(R.string.pdf_viewer_page_of, currentPage, pageCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                if (pdfFile != null && !isLoading && !loadError && !openError) {
                    IconButton(onClick = onSaveToDownloads) {
                        Icon(
                            imageVector = Icons.Filled.FileDownload,
                            contentDescription = stringResource(R.string.pdf_viewer_save),
                            tint = colors.onSurface,
                        )
                    }
                    IconButton(onClick = onOpenExternal) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = stringResource(R.string.pdf_viewer_open_external),
                            tint = colors.onSurface,
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
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.1f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 2f
                            }
                        },
                    )
                }
                .transformable(state = transformState, enabled = scale > 1f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.pdf_viewer_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                loadError || openError || rendererHolder == null -> {
                    Text(
                        text = stringResource(R.string.pdf_viewer_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                }
                else -> {
                    val holder = rendererHolder!!
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            },
                    ) {
                        val targetWidthPx = with(LocalDensity.current) {
                            (maxWidth - 24.dp).roundToPx().coerceIn(480, 2048)
                        }
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(count = holder.pageCount, key = { it }) { pageIndex ->
                                PdfPageCard(
                                    holder = holder,
                                    pageIndex = pageIndex,
                                    targetWidthPx = targetWidthPx,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPageCard(
    holder: PdfRendererHolder,
    pageIndex: Int,
    targetWidthPx: Int,
) {
    val colors = LightningTheme.colors
    val rendered by produceState<RenderedPdfPage?>(initialValue = null, holder, pageIndex, targetWidthPx) {
        value = holder.renderPage(pageIndex, targetWidthPx)
    }

    val page = rendered
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = androidx.compose.ui.graphics.Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (page != null) {
                    Modifier.aspectRatio(page.width.toFloat() / page.height.toFloat().coerceAtLeast(1f))
                } else {
                    Modifier.height(420.dp)
                },
            ),
    ) {
        if (page != null) {
            Image(
                bitmap = page.bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = colors.primary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

private data class RenderedPdfPage(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
)

private class PdfRendererHolder(
    private val fd: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) {
    val pageCount: Int = renderer.pageCount
    private val mutex = Mutex()
    private var closed = false

    suspend fun renderPage(index: Int, targetWidthPx: Int): RenderedPdfPage? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (closed || index !in 0 until pageCount) return@withContext null
            runCatching {
                renderer.openPage(index).use { page ->
                    val ratio = page.height.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                    val w = targetWidthPx
                    val h = (w * ratio).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    Canvas(bitmap).drawColor(AndroidColor.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    RenderedPdfPage(bitmap, w, h)
                }
            }.getOrNull()
        }
    }

    fun close() {
        closed = true
        runCatching { renderer.close() }
        runCatching { fd.close() }
    }
}

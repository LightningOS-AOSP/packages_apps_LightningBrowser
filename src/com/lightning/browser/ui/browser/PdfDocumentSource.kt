package com.lightning.browser.ui.browser

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class RenderedPdfPage(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
)

class PdfDocumentSource(file: File) {
    private val fd: ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer: PdfRenderer = try {
        PdfRenderer(fd)
    } catch (t: Throwable) {
        runCatching { fd.close() }
        throw t
    }

    val pageCount: Int = renderer.pageCount
    private val mutex = Mutex()
    private val lock = Any()
    @Volatile
    private var closed = false

    val firstPageAspect: Float by lazy {
        pageAspect(0)
    }

    fun pageSizePoints(index: Int): Pair<Int, Int> = synchronized(lock) {
        if (closed || index !in 0 until pageCount) return@synchronized 612 to 792
        runCatching {
            renderer.openPage(index).use { page ->
                page.width.coerceAtLeast(1) to page.height.coerceAtLeast(1)
            }
        }.getOrDefault(612 to 792)
    }

    fun pageAspect(index: Int): Float {
        val (w, h) = pageSizePoints(index)
        return w.toFloat() / h.toFloat().coerceAtLeast(1f)
    }

    suspend fun renderPage(
        index: Int,
        targetWidthPx: Int,
        mode: Int = PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
    ): RenderedPdfPage? = withContext(Dispatchers.IO) {
        mutex.withLock {
            synchronized(lock) {
                if (closed || index !in 0 until pageCount) return@synchronized null
                runCatching {
                    renderer.openPage(index).use { page ->
                        val ratio = page.height.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                        val w = targetWidthPx.coerceAtLeast(1)
                        val h = (w * ratio).roundToInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        Canvas(bitmap).drawColor(AndroidColor.WHITE)
                        page.render(bitmap, null, null, mode)
                        RenderedPdfPage(bitmap, w, h)
                    }
                }.getOrNull()
            }
        }
    }

    fun renderPageFitSync(
        index: Int,
        destWidthPx: Int,
        destHeightPx: Int,
        monochrome: Boolean = false,
        mode: Int = PdfRenderer.Page.RENDER_MODE_FOR_PRINT,
    ): Bitmap? = synchronized(lock) {
        if (closed || index !in 0 until pageCount) return@synchronized null
        val w = destWidthPx.coerceAtLeast(1)
        val h = destHeightPx.coerceAtLeast(1)
        runCatching {
            renderer.openPage(index).use { page ->
                val pageW = page.width.toFloat().coerceAtLeast(1f)
                val pageH = page.height.toFloat().coerceAtLeast(1f)
                val scale = minOf(w / pageW, h / pageH)
                val dx = (w - pageW * scale) / 2f
                val dy = (h - pageH * scale) / 2f
                val matrix = Matrix().apply {
                    postScale(scale, scale)
                    postTranslate(dx, dy)
                }
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                Canvas(bitmap).drawColor(AndroidColor.WHITE)
                page.render(bitmap, null, matrix, mode)
                if (monochrome) {
                    val monoBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val monoPaint = Paint().apply {
                        colorFilter = ColorMatrixColorFilter(
                            ColorMatrix().apply { setSaturation(0f) },
                        )
                    }
                    Canvas(monoBitmap).drawBitmap(bitmap, 0f, 0f, monoPaint)
                    bitmap.recycle()
                    monoBitmap
                } else {
                    bitmap
                }
            }
        }.getOrNull()
    }

    fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            runCatching { renderer.close() }
            runCatching { fd.close() }
        }
    }
}

package com.lightning.browser.ui.browser

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object PdfPrint {

    fun exportPdfToDownloads(
        context: Context,
        scope: CoroutineScope,
        pdfFile: File,
        title: String,
        domain: String,
        mediaSize: PrintAttributes.MediaSize,
        monochrome: Boolean,
        selectedPages: Set<Int>,
        onComplete: (savedName: String?) -> Unit,
    ) {
        // Open synchronously so cache cleanup cannot invalidate the file descriptor.
        val source = runCatching { PdfDocumentSource(pdfFile) }.getOrNull()
        if (source == null) {
            onComplete(null)
            return
        }

        scope.launch(Dispatchers.IO) {
            val bytes = try {
                val total = source.pageCount.coerceAtLeast(1)
                val pagesToWrite = selectedPages.filter { it in 1..total }.sorted()
                    .ifEmpty { listOf(1) }

                val widthPt = (mediaSize.widthMils * 72f / 1000f).roundToInt().coerceAtLeast(100)
                val heightPt = (mediaSize.heightMils * 72f / 1000f).roundToInt().coerceAtLeast(100)
                val dpiScale = 200f / 72f
                val bmpW = (widthPt * dpiScale).roundToInt().coerceIn(200, 2400)
                val bmpH = (heightPt * dpiScale).roundToInt().coerceIn(200, 3200)

                val outPdf = PdfDocument()
                val drawPaint = Paint(Paint.FILTER_BITMAP_FLAG)
                val destRect = Rect(0, 0, widthPt, heightPt)

                try {
                    pagesToWrite.forEachIndexed { idx, pageNumber ->
                        val pageIndex = pageNumber - 1
                        val pageBitmap = source.renderPageFitSync(
                            index = pageIndex,
                            destWidthPx = bmpW,
                            destHeightPx = bmpH,
                            monochrome = monochrome,
                            mode = PdfRenderer.Page.RENDER_MODE_FOR_PRINT,
                        )
                        if (pageBitmap != null) {
                            val info = PdfDocument.PageInfo.Builder(widthPt, heightPt, idx + 1).create()
                            val page = outPdf.startPage(info)
                            page.canvas.drawBitmap(pageBitmap, null, destRect, drawPaint)
                            outPdf.finishPage(page)
                            pageBitmap.recycle()
                        }
                    }

                    val baos = ByteArrayOutputStream()
                    outPdf.writeTo(baos)
                    baos.toByteArray()
                } finally {
                    outPdf.close()
                }
            } catch (_: Throwable) {
                ByteArray(0)
            } finally {
                source.close()
            }

            withContext(Dispatchers.Main) {
                if (bytes.isEmpty()) {
                    onComplete(null)
                } else {
                    val baseTitle = title.ifBlank { "document" }
                    BrowserDownloads.savePdfBytes(context, baseTitle, domain, bytes) { savedName ->
                        onComplete(savedName)
                    }
                }
            }
        }
    }
}

class PdfFilePrintAdapter(
    private val file: File,
    private val jobName: String,
    private val selectedPages: Set<Int>,
) : PrintDocumentAdapter() {

    private var source: PdfDocumentSource? = null
    private var latestAttributes: PrintAttributes = PrintAttributes.Builder().build()
    private var pagesToPrint: List<Int> = listOf(1)

    override fun onStart() {
        source = runCatching { PdfDocumentSource(file) }.getOrNull()
    }

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        latestAttributes = newAttributes
        val src = source
        if (src == null || src.pageCount <= 0) {
            callback.onLayoutFailed("Could not open PDF")
            return
        }
        pagesToPrint = selectedPages.filter { it in 1..src.pageCount }.sorted()
            .ifEmpty { (1..src.pageCount).toList() }

        val info = PrintDocumentInfo.Builder(jobName.ifBlank { "document.pdf" })
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(pagesToPrint.size)
            .build()
        callback.onLayoutFinished(info, newAttributes != oldAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        val src = source
        if (src == null) {
            callback.onWriteFailed("PDF source unavailable")
            return
        }
        val mediaSize = latestAttributes.mediaSize ?: PrintAttributes.MediaSize.ISO_A4
        val monochrome = latestAttributes.colorMode == PrintAttributes.COLOR_MODE_MONOCHROME
        val widthPt = (mediaSize.widthMils * 72f / 1000f).roundToInt().coerceAtLeast(100)
        val heightPt = (mediaSize.heightMils * 72f / 1000f).roundToInt().coerceAtLeast(100)
        val dpiScale = 200f / 72f
        val bmpW = (widthPt * dpiScale).roundToInt().coerceIn(200, 2400)
        val bmpH = (heightPt * dpiScale).roundToInt().coerceIn(200, 3200)

        val outPdf = PdfDocument()
        val drawPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        val destRect = Rect(0, 0, widthPt, heightPt)
        val writtenIndices = mutableListOf<Int>()

        try {
            pagesToPrint.forEachIndexed { outIdx, docPageNum ->
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                if (!isPageInRanges(outIdx, pages)) return@forEachIndexed

                val bitmap = src.renderPageFitSync(
                    index = docPageNum - 1,
                    destWidthPx = bmpW,
                    destHeightPx = bmpH,
                    monochrome = monochrome,
                    mode = PdfRenderer.Page.RENDER_MODE_FOR_PRINT,
                )
                if (bitmap != null) {
                    val info = PdfDocument.PageInfo.Builder(widthPt, heightPt, outIdx + 1).create()
                    val page = outPdf.startPage(info)
                    page.canvas.drawBitmap(bitmap, null, destRect, drawPaint)
                    outPdf.finishPage(page)
                    bitmap.recycle()
                    writtenIndices.add(outIdx)
                }
            }

            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                return
            }

            FileOutputStream(destination.fileDescriptor).use { fos ->
                outPdf.writeTo(fos)
            }
            val writtenRanges = if (writtenIndices.size == pagesToPrint.size) {
                arrayOf(PageRange.ALL_PAGES)
            } else {
                writtenIndices.map { PageRange(it, it) }.toTypedArray()
            }
            callback.onWriteFinished(writtenRanges)
        } catch (t: Throwable) {
            callback.onWriteFailed(t.message)
        } finally {
            outPdf.close()
        }
    }

    override fun onFinish() {
        source?.close()
        source = null
    }

    private fun isPageInRanges(pageIndex: Int, ranges: Array<out PageRange>): Boolean {
        if (ranges.isEmpty()) return true
        return ranges.any { r -> pageIndex in r.start..r.end }
    }
}

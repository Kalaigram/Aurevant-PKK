package com.aurevant.pkk.ui.features.pos

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.FileInputStream
import java.io.FileOutputStream

@Composable
fun PosRoute(vm: PosViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.handleAction(PosAction.Load) }
    PosScreen(
        state = state,
        onAction = vm::handleAction,
        onPrint = {
            state.receiptFile?.let { printPdf(context, it.name, it.absolutePath) }
        },
    )
}

/** Cetak PDF struk lewat layanan cetak Android (printer jaringan / simpan PDF). */
private fun printPdf(context: Context, jobName: String, pdfPath: String) {
    val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    manager.print(
        jobName,
        object : PrintDocumentAdapter() {
            override fun onLayout(
                old: PrintAttributes?,
                new: PrintAttributes?,
                cancellation: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?,
            ) {
                if (cancellation?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                } else {
                    callback?.onLayoutFinished(
                        PrintDocumentInfo.Builder("struk.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1).build(),
                        new != old,
                    )
                }
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellation: CancellationSignal?,
                callback: WriteResultCallback?,
            ) {
                try {
                    FileInputStream(pdfPath).use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output -> input.copyTo(output) }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        },
        null,
    )
}

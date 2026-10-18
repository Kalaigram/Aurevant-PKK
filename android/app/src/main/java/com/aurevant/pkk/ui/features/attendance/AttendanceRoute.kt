package com.aurevant.pkk.ui.features.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.util.concurrent.Executors

@Composable
fun AttendanceRoute(vm: AttendanceViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    var showScanner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.handleAction(AttendanceAction.Load)
        val device = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "android"
        vm.handleAction(AttendanceAction.SetDevice(device))
    }

    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) fetchLocation(locationClient) { lat, lng ->
            vm.handleAction(AttendanceAction.SetLocation(lat, lng))
        }
    }

    var selfieFile by remember { mutableStateOf<File?>(null) }
    val selfieShot = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) selfieFile?.let { vm.handleAction(AttendanceAction.SetSelfie(it)) }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showScanner = true
    }
    val selfiePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) return@rememberLauncherForActivityResult
        val file = File(context.cacheDir, "swafoto-${System.currentTimeMillis()}.jpg")
        selfieFile = file
        val uri = FileProvider.getUriForFile(context, "com.aurevant.pkk.fileprovider", file)
        selfieShot.launch(uri)
    }

    AttendanceScreen(
        state = state,
        onAction = vm::handleAction,
        onScanQr = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                showScanner = true
            } else {
                cameraPermission.launch(Manifest.permission.CAMERA)
            }
        },
        onTakeSelfie = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                val file = File(context.cacheDir, "swafoto-${System.currentTimeMillis()}.jpg")
                selfieFile = file
                val uri = FileProvider.getUriForFile(context, "com.aurevant.pkk.fileprovider", file)
                selfieShot.launch(uri)
            } else {
                selfiePermission.launch(Manifest.permission.CAMERA)
            }
        },
        onFetchLocation = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                fetchLocation(locationClient) { lat, lng ->
                    vm.handleAction(AttendanceAction.SetLocation(lat, lng))
                }
            } else {
                locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        },
    )

    if (showScanner) {
        AlertDialog(
            onDismissRequest = { showScanner = false },
            title = { Text("Pindai QR cabang") },
            text = {
                Column {
                    QrScannerView(onFound = {
                        vm.handleAction(AttendanceAction.QrScanned(it))
                        showScanner = false
                    })
                    Text("Arahkan kamera ke QR dinamis di layar cabang.")
                }
            },
            confirmButton = { TextButton(onClick = { showScanner = false }) { Text("Tutup") } },
        )
    }
}

private fun fetchLocation(
    client: com.google.android.gms.location.FusedLocationProviderClient,
    onFix: (Double, Double) -> Unit,
) {
    try {
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) onFix(loc.latitude, loc.longitude)
        }
    } catch (_: SecurityException) {
    }
}

@Composable
private fun QrScannerView(onFound: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
            val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val scanner = BarcodeScanning.getClient()
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media != null) {
                    val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { codes ->
                            codes.firstNotNullOfOrNull { it.rawValue }?.let(onFound)
                        }
                        .addOnCompleteListener { proxy.close() }
                } else {
                    proxy.close()
                }
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycle,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
            previewView
        },
    )
}

package com.aile.takip.ui.screens

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * ScanMode determines what the scanner is looking for
 */
enum class ScanMode(val title: String, val icon: String, val description: String) {
    BARCODE("Barkod Tara", "📦", "Ürün barkodunu kameraya gösterin"),
    QR_CODE("QR Kod Tara", "📱", "QR kodu kameraya gösterin"),
    ANY("Barkod / QR Tara", "🔍", "Barkod veya QR kodunu kameraya gösterin")
}

/**
 * Result returned when a barcode/QR is scanned
 */
data class ScanResult(
    val rawValue: String,
    val format: Int,
    val scanMode: ScanMode
)

/**
 * Full-screen barcode/QR scanner composable.
 * Uses CameraX for camera preview and ML Kit for barcode detection.
 *
 * @param scanMode What to scan for (barcode, QR, or any)
 * @param onResult Called when a barcode/QR is detected
 * @param onBack Called when user presses back button
 */
@Composable
fun BarcodeScannerScreen(
    scanMode: ScanMode = ScanMode.ANY,
    onResult: (ScanResult) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var lastScannedValue by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var flashEnabled by remember { mutableStateOf(false) }

    // Check camera permission
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (perm == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            // Camera preview with barcode detection
            CameraPreviewWithDetection(
                scanMode = scanMode,
                flashEnabled = flashEnabled,
                onBarcodeDetected = { barcode ->
                    if (isScanning && barcode != null && barcode != lastScannedValue) {
                        lastScannedValue = barcode
                        isScanning = false
                        onResult(ScanResult(barcode, 0, scanMode))
                    }
                }
            )

            // Viewfinder overlay
            Box(modifier = Modifier.fillMaxSize()) {
                // Semi-transparent background with clear center
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(280.dp)
                        .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                )

                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(Icons.Default.ArrowBack, "Geri", tint = Color.White)
                    }

                    Text(
                        scanMode.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    FilledIconButton(
                        onClick = { flashEnabled = !flashEnabled },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (flashEnabled) Color.Yellow.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            "Flaş",
                            tint = if (flashEnabled) Color.Black else Color.White
                        )
                    }
                }

                // Bottom instruction
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                scanMode.icon,
                                fontSize = 32.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                scanMode.description,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Scanning indicator animation
                AnimatedVisibility(
                    visible = isScanning,
                    modifier = Modifier.align(Alignment.Center),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
            }
        } else {
            // No camera permission
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Kamera İzni Gerekli",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Barkod/QR kod tarama için kamera erişimi gereklidir. Lütfen ayarlardan kamera iznini verin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("İzin Ver")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onBack) {
                    Text("Geri Dön")
                }
            }
        }
    }
}

/**
 * CameraX Preview + ML Kit barcode detection wrapped in AndroidView
 */
@Composable
fun CameraPreviewWithDetection(
    scanMode: ScanMode,
    flashEnabled: Boolean,
    onBarcodeDetected: (String?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Preview use case
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                // Image analysis use case for barcode detection
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                val scanner = BarcodeScanning.getClient()
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            val rawValue = barcode.rawValue
                                            if (rawValue != null) {
                                                val shouldProcess = when (scanMode) {
                                                    ScanMode.BARCODE -> barcode.format in listOf(
                                                        Barcode.FORMAT_EAN_13,
                                                        Barcode.FORMAT_EAN_8,
                                                        Barcode.FORMAT_UPC_A,
                                                        Barcode.FORMAT_UPC_E,
                                                        Barcode.FORMAT_CODE_128,
                                                        Barcode.FORMAT_CODE_39
                                                    )
                                                    ScanMode.QR_CODE -> barcode.format == Barcode.FORMAT_QR_CODE
                                                    ScanMode.ANY -> true
                                                }
                                                if (shouldProcess) {
                                                    onBarcodeDetected(rawValue)
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("BarcodeScanner", "Detection failed", e)
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                // Bind use cases
                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    // Flash control
                    camera.cameraControl.enableTorch(flashEnabled)
                } catch (e: Exception) {
                    Log.e("BarcodeScanner", "Camera bind failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

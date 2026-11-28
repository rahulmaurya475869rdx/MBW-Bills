package com.mbwbill.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasPermission = isGranted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission required")
        }
        return
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isScanning by remember { mutableStateOf(true) }
    var imageAnalysisRef by remember { mutableStateOf<ImageAnalysis?>(null) }
    
    // Zoom & Focus states
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var maxZoom by remember { mutableFloatStateOf(1f) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }

    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_DATA_MATRIX
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineY"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    imageAnalysisRef = imageAnalysis

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && isScanning) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    if (barcodes.isNotEmpty()) {
                                        val rawValue = barcodes.first().rawValue
                                        if (!rawValue.isNullOrBlank()) {
                                            isScanning = false
                                            imageAnalysis.clearAnalyzer()
                                            onScanned(rawValue)
                                        }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        cameraControl = camera.cameraControl
                        maxZoom = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                        
                        // Tap to focus implementation
                        previewView.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_UP) {
                                val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                                val action = FocusMeteringAction.Builder(point).build()
                                cameraControl?.startFocusAndMetering(action)
                                focusPoint = Offset(event.x, event.y)
                            }
                            true
                        }
                    } catch (exc: Exception) {}
                }, ContextCompat.getMainExecutor(context))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Dark Overlay & Scan Box
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxSize = size.width * 0.75f
            val left = (size.width - boxSize) / 2
            val top = (size.height - boxSize) / 2
            
            val darkColor = Color.Black.copy(alpha = 0.95f)

            drawRect(darkColor, size = Size(size.width, top))
            drawRect(darkColor, topLeft = Offset(0f, top + boxSize), size = Size(size.width, size.height - top - boxSize))
            drawRect(darkColor, topLeft = Offset(0f, top), size = Size(left, boxSize))
            drawRect(darkColor, topLeft = Offset(left + boxSize, top), size = Size(size.width - left - boxSize, boxSize))

            drawRect(Color.Green, topLeft = Offset(left, top), size = Size(boxSize, boxSize), style = Stroke(width = 5.dp.toPx()))

            val lineY = top + (boxSize * scanLineY)
            drawLine(Color.Green, start = Offset(left, lineY), end = Offset(left + boxSize, lineY), strokeWidth = 4.dp.toPx())
            
            // Focus Circle
            focusPoint?.let { point ->
                drawCircle(color = Color.Yellow, radius = 30.dp.toPx(), center = point, style = Stroke(width = 2.dp.toPx()))
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.7f), shape = CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
        
        Text(
            text = "Scan Barcode / QR Code",
            color = Color.White,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
            style = MaterialTheme.typography.titleMedium
        )
        
        // Zoom Slider
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Zoom: ${String.format("%.1f", zoomRatio)}x", color = Color.White, fontSize = 12.sp)
            Slider(
                value = zoomRatio,
                onValueChange = { 
                    zoomRatio = it
                    cameraControl?.setZoomRatio(it)
                },
                valueRange = 1f..maxZoom,
                colors = SliderDefaults.colors(
                    thumbColor = Color.Green,
                    activeTrackColor = Color.Green
                )
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            imageAnalysisRef?.clearAnalyzer()
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }
}

package com.harouat.syncedauth.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.android.gms.tasks.Task
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.harouat.syncedauth.MainViewModel
import com.harouat.syncedauth.data.Account
import com.harouat.syncedauth.otpauth.OtpAuthParser
import com.harouat.syncedauth.otpauth.OtpParseResult
import com.harouat.syncedauth.ui.components.BrandIcons
import com.harouat.syncedauth.ui.components.GradientButton
import com.harouat.syncedauth.ui.components.SecondaryButton
import com.harouat.syncedauth.ui.components.ServiceTile
import com.harouat.syncedauth.ui.theme.LocalSaColors
import com.harouat.syncedauth.ui.theme.SaType
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Live QR scanner: back-camera preview via CameraX + on-device ML Kit barcode
 * analysis. Recognizes `otpauth://…` setup codes (GitHub, Discord…) and Google
 * Authenticator's `otpauth-migration://…` transfer QR; anything else explains
 * itself instead of failing silently. A gallery route lets users scan a
 * screenshot or photo too.
 */
@Composable
fun QrScanScreen(
    vm: MainViewModel,
    onClose: () -> Unit,
) {
    val sa = LocalSaColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    // Prompt once on entry; the answer arrives through the callback above.
    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_DATA_MATRIX)
                .build()
        )
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraFailed by remember { mutableStateOf(false) }
    var bindGeneration by remember { mutableStateOf(0) } // bumped by "Try again"
    var providerRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    var torchOn by remember { mutableStateOf(false) }
    var captured by remember { mutableStateOf(false) } // a code was recognized; pause scanning
    var result by remember { mutableStateOf<OtpParseResult?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun showNotice(text: String) {
        notice = text
    }

    LaunchedEffect(notice) {
        if (notice != null) {
            delay(2600)
            notice = null
        }
    }

    fun onBarcodeFound(barcode: Barcode) {
        if (captured) return
        val raw = barcode.rawValue ?: return
        captured = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        result = runCatching { OtpAuthParser.parse(raw) }.getOrElse { OtpParseResult() }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            scanner.close()
            providerRef?.unbindAll()
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val input = InputImage.fromFilePath(context, uri)
                val codes = scanner.process(input).awaitTask()
                codes.firstOrNull()?.let { onBarcodeFound(it) }
                    ?: showNotice("No QR code found in that image")
            } catch (e: Exception) {
                showNotice("Couldn't read that image")
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF141311))) {
        if (granted && !cameraFailed) {
            val previewView = remember {
                PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
            }
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            val analysis = remember {
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(cameraExecutor) { proxy ->
                            try {
                                val media = proxy.image ?: error("already closed")
                                val input =
                                    InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                                scanner.process(input)
                                    .addOnSuccessListener { codes ->
                                        codes.firstOrNull()?.let { onBarcodeFound(it) }
                                    }
                                    .addOnCompleteListener { proxy.close() }
                            } catch (t: Throwable) {
                                proxy.close()
                            }
                        }
                    }
            }

            LaunchedEffect(granted, bindGeneration) {
                try {
                    cameraFailed = false
                    val provider = ProcessCameraProvider
                        .getInstance(context)
                        .awaitFuture(ContextCompat.getMainExecutor(context))
                    providerRef = provider
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                } catch (t: Throwable) {
                    cameraFailed = true
                }
            }

            ScanOverlay(
                torchOn = torchOn,
                onToggleTorch = {
                    torchOn = !torchOn
                    camera?.cameraControl?.enableTorch(torchOn)
                },
                onPickImage = {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onClose = onClose,
                notice = notice,
            )
        } else {
            NoCameraPane(
                cameraFailed = cameraFailed,
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onRetry = {
                    cameraFailed = false
                    bindGeneration++
                },
                onClose = onClose,
            )
        }
    }

    result?.let { parsed ->
        ResultSheet(
            parsed = parsed,
            onDismiss = { result = null; captured = false },
            onAdd = { drafts ->
                vm.addAccounts(drafts)
                result = null
                captured = false
                onClose()
            },
        )
    }
}

/* --------------------------------------------------------------- scanner UI */

@Composable
private fun ScanOverlay(
    torchOn: Boolean,
    onToggleTorch: () -> Unit,
    onPickImage: () -> Unit,
    onClose: () -> Unit,
    notice: String?,
) {
    val sa = LocalSaColors.current
    var windowRootRect by remember { mutableStateOf<Rect?>(null) }
    val beamTransition = rememberInfiniteTransition(label = "beam")
    val beamY by beamTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = LinearEasing),
            RepeatMode.Reverse,
        ),
        label = "beamY",
    )

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(Modifier.fillMaxSize()) {
            // Top bar: close | title | torch
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(Icons.Outlined.Close, "Close", onClick = onClose)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Scan QR code",
                    style = SaType.body18.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                Spacer(Modifier.weight(1f))
                CircleIconButton(
                    icon = if (torchOn) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff,
                    contentDescription = "Toggle torch",
                    background = if (torchOn) Color(0x4DFFFFFF) else Color(0x1FFFFFFF),
                    onClick = onToggleTorch,
                )
            }

            Spacer(Modifier.weight(0.42f))

            // The scan window itself; its on-screen bounds feed the scrim canvas.
            Box(
                Modifier
                    .fillMaxWidth(0.80f)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
                    .onGloballyPositioned { coords ->
                        windowRootRect = coords.boundsInRoot()
                    }
            )

            Spacer(Modifier.weight(0.34f))

            Text(
                text = "Point the camera at a setup or transfer QR code",
                style = SaType.body2,
                color = Color(0xB3FFFFFF),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.weight(0.14f))

            if (notice != null) {
                Text(
                    text = notice,
                    style = SaType.body2Medium,
                    color = sa.warning,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp),
                )
            }

            Text(
                text = "Scan from a screenshot instead",
                style = SaType.body1Medium,
                color = sa.accent,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(100))
                    .clickable { onPickImage() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    // Scrim with a carved window + corner brackets + sweeping beam.
    Canvas(Modifier.fillMaxSize()) {
        val rootRect = windowRootRect ?: return@Canvas

        val scrim = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(rootRect, CornerRadius(24.dp.toPx(), 24.dp.toPx()))
            )
        }
        drawPath(scrim, Color(0xB2000000))

        val bracket = 26.dp.toPx()
        val stroke = 4.dp.toPx()
        val inset = 2.dp.toPx()
        val white = Color.White
        // top-left
        drawLine(white, Offset(rootRect.left + inset, rootRect.top + bracket), Offset(rootRect.left + inset, rootRect.top + inset), stroke)
        drawLine(white, Offset(rootRect.left + inset, rootRect.top + inset), Offset(rootRect.left + bracket, rootRect.top + inset), stroke)
        // top-right
        drawLine(white, Offset(rootRect.right - bracket, rootRect.top + inset), Offset(rootRect.right - inset, rootRect.top + inset), stroke)
        drawLine(white, Offset(rootRect.right - inset, rootRect.top + inset), Offset(rootRect.right - inset, rootRect.top + bracket), stroke)
        // bottom-right
        drawLine(white, Offset(rootRect.right - inset, rootRect.bottom - bracket), Offset(rootRect.right - inset, rootRect.bottom - inset), stroke)
        drawLine(white, Offset(rootRect.right - bracket, rootRect.bottom - inset), Offset(rootRect.right - inset, rootRect.bottom - inset), stroke)
        // bottom-left
        drawLine(white, Offset(rootRect.left + inset, rootRect.bottom - bracket), Offset(rootRect.left + inset, rootRect.bottom - inset), stroke)
        drawLine(white, Offset(rootRect.left + bracket, rootRect.bottom - inset), Offset(rootRect.left + inset, rootRect.bottom - inset), stroke)

        // Beam sweeping the window interior.
        val beamThickness = 2.4.dp.toPx()
        val y = rootRect.top + beamY * (rootRect.height - beamThickness)
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, sa.accent, Color.Transparent),
                startX = rootRect.left,
                endX = rootRect.right,
            ),
            topLeft = Offset(rootRect.left, y),
            size = Size(rootRect.width, beamThickness),
        )
    }
}

/* --------------------------------------------------------------- smallparts */

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    background: Color = Color(0x1FFFFFFF),
) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun NoCameraPane(
    cameraFailed: Boolean,
    onRequest: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val sa = LocalSaColors.current
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.QrCodeScanner,
                contentDescription = null,
                tint = Color(0x59FFFFFF),
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = if (cameraFailed) "Camera isn't available" else "Camera access needed",
                style = SaType.headline,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (cameraFailed) {
                    "The preview couldn't start on this device. Try again or add accounts manually."
                } else {
                    "Scanning needs the camera. Recognition runs entirely on-device — " +
                        "no image ever leaves the phone."
                },
                style = SaType.body2,
                color = Color(0xB3FFFFFF),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            if (cameraFailed) {
                SecondaryButton(label = "Try again", onClick = onRetry, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(sa.buttonGradient)
                    .clickable { onRequest() },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Allow camera access", style = SaType.body1Bold, color = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Cancel",
                style = SaType.body1Medium,
                color = Color(0xB3FFFFFF),
                modifier = Modifier
                    .clip(RoundedCornerShape(100))
                    .clickable { onClose() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultSheet(
    parsed: OtpParseResult,
    onDismiss: () -> Unit,
    onAdd: (List<Account>) -> Unit,
) {
    val sa = LocalSaColors.current
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sa.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            if (parsed.drafts.isEmpty()) {
                Text(
                    text = if (parsed.skipped.isEmpty()) "This isn't an authenticator code"
                    else "Nothing can be added from it",
                    style = SaType.headline,
                    color = sa.textNormal,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = parsed.skipped.joinToString("\n").ifBlank {
                        "The scanned code doesn't contain two-factor accounts. " +
                            "Setup or Google Authenticator transfer QR codes are recognized."
                    },
                    style = SaType.body2,
                    color = sa.textWeak,
                )
                Spacer(Modifier.height(20.dp))
                SecondaryButton(label = "Try again", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            } else {
                Text(text = "Accounts found", style = SaType.headline, color = sa.textNormal)
                Spacer(Modifier.height(12.dp))
                parsed.drafts.forEach { draft ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ServiceTile(
                            initial = draft.name.ifBlank { draft.issuer }.take(1),
                            brand = BrandIcons.forAccount(draft.issuer, draft.name),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = draft.issuer.ifBlank { draft.name },
                                style = SaType.body1Medium,
                                color = sa.textNormal,
                                maxLines = 1,
                            )
                            if (draft.issuer.isNotBlank() && draft.name.isNotBlank()) {
                                Text(
                                    text = draft.name,
                                    style = SaType.body2,
                                    color = sa.textWeak,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                if (parsed.skipped.isNotEmpty()) {
                    Text(
                        text = "Skipped — " + parsed.skipped.joinToString("; "),
                        style = SaType.body2,
                        color = sa.textPlaceholder,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(16.dp))
                GradientButton(
                    label = if (parsed.drafts.size == 1) "Add account"
                    else "Add ${parsed.drafts.size} accounts",
                    onClick = { onAdd(parsed.drafts) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                SecondaryButton(label = "Cancel", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/* ----------------------------------------------------------------- helpers */

/** gms Task as suspend — ML Kit one-shot decode of a picked image. */
private suspend fun <T> Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }

/** ListenableFuture as suspend — CameraX provider lookup. */
private suspend fun <T> ListenableFuture<T>.awaitFuture(executor: Executor): T =
    suspendCancellableCoroutine { cont ->
        addListener({
            try {
                cont.resume(get())
            } catch (t: Throwable) {
                cont.resumeWithException(t)
            }
        }, executor)
    }

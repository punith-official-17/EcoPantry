package com.example.ui.scan

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.PantryIntelligenceService
import com.example.model.PantryCategory
import com.example.model.ReceiptScanResult
import com.example.model.ScannedItemCandidate
import com.example.ui.components.CategoryIcon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MintContainer
import com.example.viewmodel.PantryViewModel
import java.io.InputStream
import java.util.concurrent.Executors

enum class ScanMode {
    RECEIPT,
    ITEM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scannedResult by viewModel.scannedResult.collectAsStateWithLifecycle()

    var scanMode by remember { mutableStateOf(ScanMode.RECEIPT) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.processReceiptImage(bitmap)
                }
            } catch (e: Exception) {
                // Fallback
                viewModel.processReceiptText("Sample Receipt")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("scan_receipt_screen")
    ) {
        if (hasCameraPermission) {
            // Live Camera Viewfinder
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            // Camera error handling
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fallback when camera permission is pending or denied
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E2820))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = EmeraldGreenLight,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Access Needed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Allow camera access to instantly scan grocery receipts or food packaging.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Grant Camera Permission")
                    }
                }
            }
        }

        // Viewfinder Guide Overlay & Laser Beam
        ViewfinderOverlay(scanMode = scanMode, isScanning = isScanning)

        // Top Controls: Mode Switcher & Quick Demo Test button
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 28.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Smart Scanner",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Quick Demo scan button (loads realistic sample grocery receipt instantly)
                Surface(
                    color = EmeraldGreen,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clickable {
                            val sample = PantryIntelligenceService.generateFallbackScannedReceipt()
                            viewModel.processReceiptText(sample.rawText)
                        }
                        .testTag("demo_receipt_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Load Sample Receipt",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Selector Pill (Receipt vs Single Item)
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        FilterChip(
                            selected = scanMode == ScanMode.RECEIPT,
                            onClick = { scanMode = ScanMode.RECEIPT },
                            label = { Text("Grocery Receipt") },
                            leadingIcon = {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color.Transparent,
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("mode_receipt_chip")
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        FilterChip(
                            selected = scanMode == ScanMode.ITEM,
                            onClick = { scanMode = ScanMode.ITEM },
                            label = { Text("Single Item") },
                            leadingIcon = {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color.Transparent,
                                labelColor = Color.White
                            ),
                            modifier = Modifier.testTag("mode_item_chip")
                        )
                    }
                }
            }
        }

        // Bottom Capture Controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 96.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Gallery button
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(52.dp)
                        .clickable { galleryLauncher.launch("image/*") }
                        .testTag("gallery_picker_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Pick Image",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Shutter Capture Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                        .clickable {
                            if (isScanning) return@clickable
                            if (hasCameraPermission) {
                                imageCapture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val buffer = image.planes[0].buffer
                                            val bytes = ByteArray(buffer.remaining())
                                            buffer.get(bytes)
                                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            image.close()
                                            if (bitmap != null) {
                                                viewModel.processReceiptImage(bitmap)
                                            } else {
                                                viewModel.processReceiptText("Pantry Item")
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            viewModel.processReceiptText("Pantry Item")
                                        }
                                    }
                                )
                            } else {
                                viewModel.processReceiptText("Pantry Item")
                            }
                        }
                        .testTag("capture_shutter_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(if (isScanning) Color.Gray else EmeraldGreen)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Capture",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(28.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }

                // Flip Camera
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(52.dp)
                        .clickable {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Switch Camera",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Scanning Status Toast
        if (isScanning) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(color = EmeraldGreenLight)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AI Analyzing Receipt...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Extracting items & predicting shelf life",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // Scanned Result Review Modal Bottom Sheet
    scannedResult?.let { result ->
        ScannedItemsReviewSheet(
            result = result,
            onDismiss = { viewModel.clearScannedResult() },
            onConfirm = { items -> viewModel.confirmScannedItems(items) }
        )
    }
}

@Composable
fun ViewfinderOverlay(scanMode: ScanMode, isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        val widthFraction = if (scanMode == ScanMode.RECEIPT) 0.82f else 0.72f
        val heightFraction = if (scanMode == ScanMode.RECEIPT) 0.52f else 0.40f

        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(if (scanMode == ScanMode.RECEIPT) 340.dp else 260.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
        ) {
            if (isScanning) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val y = size.height * laserPosition
                    drawLine(
                        color = EmeraldGreenLight,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 6f
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedItemsReviewSheet(
    result: ReceiptScanResult,
    onDismiss: () -> Unit,
    onConfirm: (List<ScannedItemCandidate>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val candidates = remember {
        mutableStateListOf<ScannedItemCandidate>().apply {
            addAll(result.items)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("scanned_review_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = result.storeName ?: "Scanned Receipt",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${candidates.count { it.isSelected }} of ${candidates.size} items selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                itemsIndexed(candidates) { index, item ->
                    ScannedItemRow(
                        item = item,
                        onCheckedChange = { isChecked ->
                            candidates[index] = item.copy(isSelected = isChecked)
                        },
                        onCategoryChange = { newCat ->
                            candidates[index] = item.copy(category = newCat, estimatedShelfLifeDays = newCat.defaultShelfLifeDays)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onConfirm(candidates) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_scanned_items_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save ${candidates.count { it.isSelected }} Items to Pantry",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ScannedItemRow(
    item: ScannedItemCandidate,
    onCheckedChange: (Boolean) -> Unit,
    onCategoryChange: (PantryCategory) -> Unit
) {
    var showCatMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) MintContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = EmeraldGreen)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.quantity} ${item.unit} • Expires ~${item.estimatedShelfLifeDays}d",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldGreenDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Category selector pill
            Box {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier.clickable { showCatMenu = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        CategoryIcon(category = item.category, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.category.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                DropdownMenu(
                    expanded = showCatMenu,
                    onDismissRequest = { showCatMenu = false }
                ) {
                    PantryCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.displayName) },
                            leadingIcon = { CategoryIcon(category = cat, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showCatMenu = false
                                onCategoryChange(cat)
                            }
                        )
                    }
                }
            }
        }
    }
}

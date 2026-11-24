package com.tailgunnerx.piccollage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ─── Palette ────────────────────────────────────────────────────────────────
val Pink500   = Color(0xFFE91E8C)
val Purple600 = Color(0xFF6A1BDB)
val Dark900   = Color(0xFF0D0D14)
val Dark800   = Color(0xFF16161F)
val Dark700   = Color(0xFF1E1E2C)
val Surface   = Color(0xFF242435)
val Subtle    = Color(0xFF9090A8)
val White     = Color(0xFFFFFFFF)

enum class GradientDir { DIAGONAL, REVERSE_DIAGONAL, TOP_BOTTOM, LEFT_RIGHT, RADIAL_CENTER, RADIAL_CORNER }

sealed class BorderStyle {
    data class SolidColor(val color: Color) : BorderStyle()
    data class GradientPattern(val colors: List<Color>, val label: String, val dir: GradientDir = GradientDir.DIAGONAL) : BorderStyle()
}

val AppGradient = Brush.linearGradient(listOf(Pink500, Purple600))

class PhotoState(initialBitmap: Bitmap) {
    var bitmap by mutableStateOf(initialBitmap)
    var panX by mutableStateOf(0f)
    var panY by mutableStateOf(0f)
    var zoom by mutableStateOf(1f)

    fun toPhotoData() = PhotoData(bitmap, panX, panY, zoom)
}

// ─── Activity ────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            PicCollageApp()
        }
    }
}

/**
 * Decodes an image downsampled so its longest side is at most [maxDim].
 * Full-resolution camera photos are ~50 MB each in memory, which would quickly
 * run out of memory with 9+ photo layouts.
 */
fun decodeSampledBitmap(context: Context, uri: Uri, maxDim: Int = 2400): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDim) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
}

fun BorderStyle.GradientPattern.toBrush(radialCornerRadius: Float): Brush = when (dir) {
    GradientDir.TOP_BOTTOM -> Brush.verticalGradient(colors)
    GradientDir.LEFT_RIGHT -> Brush.horizontalGradient(colors)
    GradientDir.REVERSE_DIAGONAL -> Brush.linearGradient(colors, start = Offset(Float.POSITIVE_INFINITY, 0f), end = Offset(0f, Float.POSITIVE_INFINITY))
    GradientDir.RADIAL_CENTER -> Brush.radialGradient(colors)
    GradientDir.RADIAL_CORNER -> Brush.radialGradient(colors, center = Offset(0f, 0f), radius = radialCornerRadius)
    else -> Brush.linearGradient(colors)
}

val GradientPatterns = listOf(
        BorderStyle.GradientPattern(listOf(Color(0xFF00FFFF), Color(0xFF7B00FF), Color(0xFFFF00FF), Color(0xFF00FFCC), Color(0xFF0055FF)), "Neon Night", GradientDir.DIAGONAL),
        BorderStyle.GradientPattern(listOf(Color(0xFF4B0082), Color(0xFF7B00FF), Color(0xFF00FFD5), Color(0xFF00BFFF), Color(0xFF8A2BE2)), "Cyber Violet", GradientDir.TOP_BOTTOM),
        BorderStyle.GradientPattern(listOf(Color(0xFFFF6600), Color(0xFFFF0066), Color(0xFFFFD700), Color(0xFFFF3300), Color(0xFFFF9900)), "Blaze", GradientDir.LEFT_RIGHT),
        BorderStyle.GradientPattern(listOf(Color(0xFF003366), Color(0xFF00BFFF), Color(0xFF7FFFD4), Color(0xFF00FFFF), Color(0xFF1E90FF)), "Electric Ocean", GradientDir.REVERSE_DIAGONAL),
        BorderStyle.GradientPattern(listOf(Color(0xFF39FF14), Color(0xFF00FF7F), Color(0xFF00FFCC), Color(0xFFADFF2F), Color(0xFF39FF14)), "Matrix Green", GradientDir.RADIAL_CENTER),
        BorderStyle.GradientPattern(listOf(Color(0xFFFF007F), Color(0xFFFF4500), Color(0xFFFFD700), Color(0xFFFF69B4), Color(0xFFFF6347)), "Sunset Rave", GradientDir.DIAGONAL),
        BorderStyle.GradientPattern(listOf(Color(0xFF00F5D4), Color(0xFF7209B7), Color(0xFFF72585), Color(0xFF4361EE), Color(0xFF4CC9F0)), "Aurora", GradientDir.TOP_BOTTOM),
        BorderStyle.GradientPattern(listOf(Color(0xFFFFB3C6), Color(0xFFCDB4DB), Color(0xFFA2D2FF), Color(0xFFBDE0FE), Color(0xFFFFAFCC)), "Cotton Candy", GradientDir.LEFT_RIGHT),
        BorderStyle.GradientPattern(listOf(Color(0xFFFF0080), Color(0xFF8B00FF), Color(0xFF001AFF), Color(0xFF00BFFF), Color(0xFFFF0080)), "Retrowave", GradientDir.REVERSE_DIAGONAL),
        BorderStyle.GradientPattern(listOf(Color(0xFFFF4500), Color(0xFFFF8C00), Color(0xFFFFD700), Color(0xFFFFFFE0), Color(0xFFFF6347)), "Lava Core", GradientDir.RADIAL_CENTER),
        BorderStyle.GradientPattern(listOf(Color(0xFF00FF00), Color(0xFF00FFAA), Color(0xFF0088FF), Color(0xFFAA00FF), Color(0xFFFF0088)), "Tropic Fusion", GradientDir.DIAGONAL),
        BorderStyle.GradientPattern(listOf(Color(0xFFFF8C00), Color(0xFFFFB300), Color(0xFFFFD700), Color(0xFFFFEA00), Color(0xFFFF6500)), "Golden Hour", GradientDir.LEFT_RIGHT),
        BorderStyle.GradientPattern(listOf(Color(0xFFE0F7FA), Color(0xFF80DEEA), Color(0xFF00BCD4), Color(0xFF006064), Color(0xFF80CBC4)), "Ice Storm", GradientDir.TOP_BOTTOM),
        BorderStyle.GradientPattern(listOf(Color(0xFFFF0000), Color(0xFFFF7700), Color(0xFFFFFF00), Color(0xFF00FF00), Color(0xFF0000FF), Color(0xFF8B00FF)), "Carnival", GradientDir.REVERSE_DIAGONAL),
        BorderStyle.GradientPattern(listOf(Color(0xFFB76E79), Color(0xFFD4A017), Color(0xFFE8C4A0), Color(0xFFC9956C), Color(0xFFB76E79)), "Rose Gold", GradientDir.RADIAL_CORNER),
        BorderStyle.GradientPattern(listOf(Color(0xFF2E0047), Color(0xFF7B2D8B), Color(0xFFE040FB), Color(0xFFFF6E40), Color(0xFFFFD740)), "Deep Galaxy", GradientDir.RADIAL_CENTER),
        // 5 new dynamic entries
        BorderStyle.GradientPattern(listOf(Color(0xFF0FF0FC), Color(0xFF00B4D8), Color(0xFF9B5DE5), Color(0xFFF15BB5), Color(0xFFFEE440), Color(0xFF00F5D4)), "Prism", GradientDir.DIAGONAL),
        BorderStyle.GradientPattern(listOf(Color(0xFFFF1744), Color(0xFFFF6D00), Color(0xFFFFEA00), Color(0xFF76FF03), Color(0xFF00E5FF), Color(0xFFD500F9)), "Full Spectrum", GradientDir.LEFT_RIGHT),
        BorderStyle.GradientPattern(listOf(Color(0xFF212121), Color(0xFF37474F), Color(0xFF00E5FF), Color(0xFF1DE9B6), Color(0xFF212121)), "Obsidian Glow", GradientDir.RADIAL_CENTER),
        BorderStyle.GradientPattern(listOf(Color(0xFFAA00FF), Color(0xFFFF6EC7), Color(0xFFFFD700), Color(0xFFFF4081), Color(0xFF40C4FF), Color(0xFFAA00FF)), "Holographic", GradientDir.REVERSE_DIAGONAL),
        BorderStyle.GradientPattern(listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53), Color(0xFFFF6B6B), Color(0xFFFF8E53), Color(0xFFFECE00), Color(0xFFFF6B6B)), "Phoenix", GradientDir.RADIAL_CORNER),
        // 5 additional dynamic gradients
        BorderStyle.GradientPattern(listOf(Color(0xFF0B0033), Color(0xFF3700B3), Color(0xFF6200EE), Color(0xFFBB86FC), Color(0xFFFF00FF)), "Starry Nebula", GradientDir.DIAGONAL),
        BorderStyle.GradientPattern(listOf(Color(0xFF1A1A1A), Color(0xFF32CD32), Color(0xFF00CED1), Color(0xFF1A1A1A)), "Cyber Lime", GradientDir.LEFT_RIGHT),
        BorderStyle.GradientPattern(listOf(Color(0xFFFF4E50), Color(0xFFFC913A), Color(0xFFF9D423), Color(0xFFEDE574)), "Sunset Silk", GradientDir.TOP_BOTTOM),
        BorderStyle.GradientPattern(listOf(Color(0xFF000428), Color(0xFF004e92), Color(0xFF0083B0), Color(0xFF00B4DB)), "Oceanic Drift", GradientDir.REVERSE_DIAGONAL),
        BorderStyle.GradientPattern(listOf(Color(0xFF000000), Color(0xFF4B0000), Color(0xFFB22222), Color(0xFFFF4500), Color(0xFFFFD700)), "Solar Eclipse", GradientDir.RADIAL_CENTER)
)

// ─── Root composable ─────────────────────────────────────────────────────────
@Composable
fun PicCollageApp() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var settings by remember { mutableStateOf(SettingsStore.load(context)) }
    var showSettings by remember { mutableStateOf(false) }
    val customGrid = remember { SettingsStore.loadCustomGrid(context) }

    // Persist the custom grid (debounced, since dragging lines fires many updates).
    LaunchedEffect(customGrid) {
        snapshotFlow { listOf(customGrid.cols, customGrid.rows, customGrid.colStops.toList(), customGrid.rowStops.toList()) }
            .collectLatest {
                delay(400)
                SettingsStore.saveCustomGrid(context, customGrid)
            }
    }

    var selectedType by remember { mutableStateOf(CollageType.GRID_2X2) }
    val photos = remember { mutableStateListOf<PhotoState?>() }
    var isSaving by remember { mutableStateOf(false) }
    var borderPx by remember { mutableStateOf(8f) }
    var borderStyle by remember { mutableStateOf<BorderStyle>(BorderStyle.SolidColor(Color.White)) }
    var previewWidthDp by remember { mutableStateOf(320f) }

    val cells = cellsFor(selectedType, customGrid)
    val requiredCount = cells.size
    val previewBitmaps = List(requiredCount) { photos.getOrNull(it) }

    var activeIndexToFill by remember { mutableStateOf(-1) }

    // Single-photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val target = activeIndexToFill
        if (uri != null && target >= 0) {
            scope.launch(Dispatchers.IO) {
                try {
                    val bmp = decodeSampledBitmap(context, uri) ?: return@launch
                    withContext(Dispatchers.Main) {
                        while (photos.size <= target) photos.add(null)
                        photos[target] = PhotoState(bmp)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            onChange = { settings = it; SettingsStore.save(context, it) },
            onDismiss = { showSettings = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Dark900)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✦", color = White, fontSize = 16.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "PicCollage",
                    color = White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = SettingsIcon,
                        contentDescription = "Settings",
                        tint = SettingsAccent,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // ── Layout picker ────────────────────────────────────────────────
            Text(
                "Choose a Layout",
                color = Subtle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 20.dp, bottom = 10.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CollageType.entries) { type ->
                    LayoutChip(
                        type = type,
                        cells = cellsFor(type, customGrid),
                        isSelected = type == selectedType,
                        onClick = { selectedType = type }
                    )
                }
            }

            // ── Custom grid controls ─────────────────────────────────────────
            if (selectedType == CollageType.CUSTOM) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Dark700)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Stepper(
                            label = "Columns",
                            value = customGrid.cols,
                            onMinus = { customGrid.changeCols(customGrid.cols - 1) },
                            onPlus = { customGrid.changeCols(customGrid.cols + 1) },
                            minusEnabled = customGrid.cols > 1,
                            plusEnabled = customGrid.cols < CustomGridState.MAX_LINES
                        )
                        Stepper(
                            label = "Rows",
                            value = customGrid.rows,
                            onMinus = { customGrid.changeRows(customGrid.rows - 1) },
                            onPlus = { customGrid.changeRows(customGrid.rows + 1) },
                            minusEnabled = customGrid.rows > 1,
                            plusEnabled = customGrid.rows < CustomGridState.MAX_LINES
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Drag the pink handles on the preview to move the lines.",
                            color = Subtle,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { customGrid.resetLines() }) {
                            Text("Even out", color = Pink500, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Preview canvas ───────────────────────────────────────────────
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                val ratio = settings.ratio
                val maxPreviewHeight = 520.dp
                val previewWidth = if (maxWidth / ratio > maxPreviewHeight) maxPreviewHeight * ratio else maxWidth
                SideEffect { previewWidthDp = previewWidth.value }
                Box(
                    modifier = Modifier
                        .width(previewWidth)
                        .aspectRatio(ratio)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Dark800)
                ) {
                    CollagePreview(
                        cells = cells,
                        photos = previewBitmaps,
                        borderPx = borderPx,
                        cornerRadius = settings.cornerRadius,
                        borderStyle = borderStyle,
                        customGrid = if (selectedType == CollageType.CUSTOM) customGrid else null,
                        onAddPhoto = { index ->
                            activeIndexToFill = index
                            photoPickerLauncher.launch("image/*")
                        },
                        onRemovePhoto = { index ->
                            if (index < photos.size) {
                                photos[index] = null
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "${settings.ratioLabel} · ${settings.outputSize().width}×${settings.outputSize().height}",
                color = Subtle,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSettings = true }
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // ── Border & Color Controls ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Border:", color = White, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = borderPx,
                    onValueChange = { borderPx = it },
                    valueRange = 0f..40f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Pink500, activeTrackColor = Pink500)
                )
            }
            var showColorPicker by remember { mutableStateOf(false) }

            if (showColorPicker) {
                val initColor = (borderStyle as? BorderStyle.SolidColor)?.color ?: Color.White
                AlertDialog(
                    onDismissRequest = { showColorPicker = false },
                    title = { Text("Pick Border Color", color = White) },
                    text = {
                        Column {
                            Box(Modifier.fillMaxWidth().height(40.dp).background(initColor).clip(RoundedCornerShape(8.dp)))
                            Spacer(Modifier.height(16.dp))
                            ColorSpectrumPicker(
                                initialColor = initColor,
                                onColorChanged = { borderStyle = BorderStyle.SolidColor(it) }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showColorPicker = false }) { Text("Done", color = Pink500) }
                    },
                    containerColor = Dark800
                )
            }

            Spacer(Modifier.height(12.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentStyle = borderStyle
                val availableColors = listOf(Color.White, Color.Black, Pink500, Purple600, Color(0xFFF44336), Color(0xFF4CAF50), Color(0xFF2196F3))
                val isCustomSelected = currentStyle is BorderStyle.SolidColor && !availableColors.contains(currentStyle.color)
                item {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                            .border(2.dp, if (isCustomSelected) White else Color.Transparent, CircleShape)
                            .clickable { showColorPicker = true }
                    )
                }
                items(availableColors) { c ->
                    val isSel = currentStyle is BorderStyle.SolidColor && currentStyle.color == c
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(2.dp, if (isSel) White else Color.Transparent, CircleShape)
                            .clickable { borderStyle = BorderStyle.SolidColor(c) }
                    )
                }
            }

            var showGradients by remember { mutableStateOf(false) }

            // ── Gradients ────────────────────────────────────────────────────
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Dark700)
                    .clickable { showGradients = !showGradients }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(settings.e("✨", "Gradient Borders"), color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(if (showGradients) "▲" else "▼", color = Subtle, fontSize = 11.sp)
            }
            androidx.compose.animation.AnimatedVisibility(visible = showGradients) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(GradientPatterns) { p ->
                        val isSel = borderStyle == p
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(p.toBrush(radialCornerRadius = 200f))
                                .border(2.dp, if (isSel) White else Color(0x44FFFFFF), CircleShape)
                                .clickable { borderStyle = p }
                        )
                    }
                }
            }

            } // Close scrollable Column

            // ── Action buttons ───────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Save collage
                val canSave = previewBitmaps.all { it != null } && !isSaving
                Button(
                    onClick = {
                        if (!canSave) return@Button
                        scope.launch {
                            isSaving = true
                            try {
                                val uri = renderAndSaveCollage(
                                    context = context,
                                    cells = cells,
                                    photos = previewBitmaps.filterNotNull().map { it.toPhotoData() },
                                    outputSize = settings.outputSize(),
                                    borderPx = borderPx,
                                    cornerRadius = settings.cornerRadius,
                                    previewWidthDp = previewWidthDp,
                                    borderStyle = borderStyle
                                )
                                withContext(Dispatchers.Main) {
                                    if (uri != null) {
                                        Toast.makeText(context, settings.e("✅", "Saved to Pictures/PicCollage!"), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, settings.e("❌", "Save failed"), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, settings.e("❌", "Save failed: ${e.message}"), Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (canSave) AppGradient else Brush.linearGradient(listOf(Surface, Surface))),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text(settings.e("💾", "Save Collage"), color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ─── Layout chip ─────────────────────────────────────────────────────────────
@Composable
fun LayoutChip(type: CollageType, cells: List<CellRect>, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        if (isSelected) Pink500 else Dark700,
        animationSpec = tween(200), label = "chip_bg"
    )
    val scale by animateFloatAsState(
        if (isSelected) 1.06f else 1f,
        animationSpec = tween(200), label = "chip_scale"
    )
    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LayoutThumbnail(cells = cells, isSelected = isSelected)
        Spacer(Modifier.height(6.dp))
        Text(
            type.displayName,
            color = White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Mini thumbnail showing the grid shape ───────────────────────────────────
@Composable
fun LayoutThumbnail(cells: List<CellRect>, isSelected: Boolean) {
    val cellColor = if (isSelected) Color.White.copy(alpha = 0.35f) else Color(0xFF3A3A55)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Dark900)
    ) {
        Canvas(Modifier.fillMaxSize().padding(1.dp)) {
            val gap = 2.dp.toPx()
            val corner = CornerRadius(3.dp.toPx())
            cells.forEach { c ->
                val r = c.toPx(size.width, size.height, gap)
                drawRoundRect(cellColor, topLeft = Offset(r.left, r.top), size = Size(r.width, r.height), cornerRadius = corner)
            }
        }
    }
}

// ─── Live collage preview (same aspect as saved output) ──────────────────────
@Composable
fun CollagePreview(
    cells: List<CellRect>,
    photos: List<PhotoState?>,
    borderPx: Float,
    cornerRadius: Float,
    borderStyle: BorderStyle,
    customGrid: CustomGridState?,
    onAddPhoto: (Int) -> Unit,
    onRemovePhoto: (Int) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        when (val style = borderStyle) {
            is BorderStyle.SolidColor -> Spacer(Modifier.fillMaxSize().background(style.color))
            is BorderStyle.GradientPattern -> Spacer(Modifier.fillMaxSize().background(style.toBrush(radialCornerRadius = 800f)))
        }

        val density = LocalDensity.current
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val gapPx = with(density) { borderPx.dp.toPx() }

        cells.forEachIndexed { i, cell ->
            val r = cell.toPx(wPx, hPx, gapPx)
            key(i) {
                PhotoCellBox(
                    state = photos.getOrNull(i),
                    index = i,
                    cornerRadius = cornerRadius,
                    onAddPhoto = onAddPhoto,
                    onRemovePhoto = onRemovePhoto,
                    modifier = Modifier
                        .offset { IntOffset(r.left.roundToInt(), r.top.roundToInt()) }
                        .size(with(density) { r.width.toDp() }, with(density) { r.height.toDp() })
                )
            }
        }

        // Draggable line handles for the custom grid
        if (customGrid != null) {
            val handleTouch = 28.dp
            val handleTouchPx = with(density) { handleTouch.toPx() }
            customGrid.colStops.forEachIndexed { i, stop ->
                Box(
                    modifier = Modifier
                        .offset { IntOffset((stop * wPx - handleTouchPx / 2f).roundToInt(), 0) }
                        .width(handleTouch)
                        .fillMaxHeight()
                        .pointerInput(customGrid, i, wPx) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                customGrid.moveColStop(i, drag.x / wPx)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { GridHandle(vertical = true) }
            }
            customGrid.rowStops.forEachIndexed { i, stop ->
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, (stop * hPx - handleTouchPx / 2f).roundToInt()) }
                        .height(handleTouch)
                        .fillMaxWidth()
                        .pointerInput(customGrid, i, hPx) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                customGrid.moveRowStop(i, drag.y / hPx)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { GridHandle(vertical = false) }
            }
        }
    }
}

@Composable
private fun GridHandle(vertical: Boolean) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .size(if (vertical) 8.dp else 36.dp, if (vertical) 36.dp else 8.dp)
            .clip(shape)
            .background(Pink500)
            .border(1.5.dp, White, shape)
    )
}

@Composable
fun PhotoCellBox(
    state: PhotoState?,
    index: Int,
    cornerRadius: Float,
    onAddPhoto: (Int) -> Unit,
    onRemovePhoto: (Int) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius.dp)).background(Dark700)) {
        if (state != null) {
            PhotoCell(state = state, modifier = Modifier.fillMaxSize())
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable {
                            val matrix = android.graphics.Matrix().apply { postRotate(90f) }
                            state.bitmap = Bitmap.createBitmap(state.bitmap, 0, 0, state.bitmap.width, state.bitmap.height, matrix, true)
                            state.panX = 0f
                            state.panY = 0f
                            state.zoom = 1f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("↻", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-1).dp))
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { onRemovePhoto(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().clickable { onAddPhoto(index) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text("＋", color = Pink500, fontSize = 22.sp)
                }
            }
        }
    }
}

// ─── Color Spectrum Picker ───────────────────────────────────────────────────
@Composable
fun ColorSpectrumPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit
) {
    var hsv by remember { 
        val hsvArr = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsvArr)
        mutableStateOf(hsvArr) 
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.isNotEmpty()) {
                                val pos = event.changes.first().position
                                val sat = (pos.x / size.width).coerceIn(0f, 1f)
                                val value = 1f - (pos.y / size.height).coerceIn(0f, 1f)
                                hsv = floatArrayOf(hsv[0], sat, value)
                                onColorChanged(Color(android.graphics.Color.HSVToColor(hsv)))
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], 1f, 1f)))
                drawRect(brush = Brush.horizontalGradient(colors = listOf(Color.White, hueColor)))
                drawRect(brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black)))

                val thumbX = hsv[1] * size.width
                val thumbY = (1f - hsv[2]) * size.height
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(thumbX, thumbY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.isNotEmpty()) {
                                val pos = event.changes.first().position
                                val hue = (pos.x / size.width).coerceIn(0f, 1f) * 360f
                                hsv = floatArrayOf(hue, hsv[1], hsv[2])
                                onColorChanged(Color(android.graphics.Color.HSVToColor(hsv)))
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                            Color.Blue, Color.Magenta, Color.Red
                        )
                    )
                )
                val thumbX = (hsv[0] / 360f) * size.width
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(thumbX, size.height / 2f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun PhotoCell(state: PhotoState, modifier: Modifier) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    Canvas(
        modifier = modifier
            .clipToBounds()
            .pointerInput(state) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (canvasSize.width > 0 && canvasSize.height > 0) {
                        state.zoom = (state.zoom * zoom).coerceIn(1f, 5f)
                        state.panX += pan.x / canvasSize.width
                        state.panY += pan.y / canvasSize.height
                    }
                }
            }
    ) {
        canvasSize = IntSize(size.width.toInt(), size.height.toInt())
        val bmp = state.bitmap
        val bmpWidth = bmp.width.toFloat()
        val bmpHeight = bmp.height.toFloat()
        val dstWidth = size.width
        val dstHeight = size.height

        val srcAspect = bmpWidth / bmpHeight
        val dstAspect = dstWidth / dstHeight

        val newW: Float
        val newH: Float
        if (srcAspect > dstAspect) {
            newW = bmpHeight * dstAspect
            newH = bmpHeight
        } else {
            newW = bmpWidth
            newH = bmpWidth / dstAspect
        }

        val scaledW = newW / state.zoom
        val scaledH = newH / state.zoom

        val dxOffset = state.panX * scaledW
        val dyOffset = state.panY * scaledH

        val dx = ((bmpWidth - scaledW) / 2) - dxOffset
        val clampedDx = dx.coerceIn(0f, bmpWidth - scaledW)

        val dy = ((bmpHeight - scaledH) / 2) - dyOffset
        val clampedDy = dy.coerceIn(0f, bmpHeight - scaledH)

        val srcOffset = IntOffset(clampedDx.toInt(), clampedDy.toInt())
        val srcSize = IntSize(scaledW.toInt(), scaledH.toInt())
        
        drawImage(
            image = bmp.asImageBitmap(),
            srcOffset = srcOffset,
            srcSize = srcSize,
            dstOffset = IntOffset.Zero,
            dstSize = canvasSize
        )
    }
}

package com.example.piccollage

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
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

// ─── Root composable ─────────────────────────────────────────────────────────
@Composable
fun PicCollageApp() {
    var selectedType by remember { mutableStateOf(CollageType.GRID_2X2) }
    val photos = remember { mutableStateListOf<PhotoState?>() }
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var borderPx by remember { mutableStateOf(8f) }
    var borderStyle by remember { mutableStateOf<BorderStyle>(BorderStyle.SolidColor(Color.White)) }

    val requiredCount = selectedType.photoCount
    val previewBitmaps = List(requiredCount) { photos.getOrNull(it) }

    var activeIndexToFill by remember { mutableStateOf(-1) }

    // Single-photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && activeIndexToFill in 0 until requiredCount) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        withContext(Dispatchers.Main) {
                            while (photos.size <= activeIndexToFill) photos.add(null)
                            photos[activeIndexToFill] = PhotoState(bmp)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
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
                    fontWeight = FontWeight.Bold
                )
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
                        isSelected = type == selectedType,
                        onClick = { selectedType = type }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Preview canvas ───────────────────────────────────────────────

            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Dark800),
                contentAlignment = Alignment.Center
            ) {
                CollagePreview(
                    type = selectedType,
                    photos = previewBitmaps,
                    borderPx = borderPx,
                    borderStyle = borderStyle,
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

            Spacer(Modifier.height(14.dp))

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
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                            .border(2.dp, if (isCustomSelected) White else Color.Transparent, androidx.compose.foundation.shape.CircleShape)
                            .clickable { showColorPicker = true }
                    )
                }
                items(availableColors) { c ->
                    val isSel = currentStyle is BorderStyle.SolidColor && currentStyle.color == c
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(c)
                            .border(2.dp, if (isSel) White else Color.Transparent, androidx.compose.foundation.shape.CircleShape)
                            .clickable { borderStyle = BorderStyle.SolidColor(c) }
                    )
                }
            }

            var showGradients by remember { mutableStateOf(false) }
            
            // ── Gradients ────────────────────────────────────────────────────
            @Composable
            fun GradientBubble(p: BorderStyle.GradientPattern) {
                val isSel = borderStyle == p
                val brush = when (p.dir) {
                    GradientDir.TOP_BOTTOM -> Brush.verticalGradient(p.colors)
                    GradientDir.LEFT_RIGHT -> Brush.horizontalGradient(p.colors)
                    GradientDir.REVERSE_DIAGONAL -> Brush.linearGradient(p.colors, start = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f), end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY))
                    GradientDir.RADIAL_CENTER -> Brush.radialGradient(p.colors)
                    GradientDir.RADIAL_CORNER -> Brush.radialGradient(p.colors, center = androidx.compose.ui.geometry.Offset(0f, 0f), radius = 200f)
                    else -> Brush.linearGradient(p.colors)
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(brush)
                        .border(2.dp, if (isSel) White else Color(0x44FFFFFF), androidx.compose.foundation.shape.CircleShape)
                        .clickable { borderStyle = p }
                )
            }

            val gradientPatterns = listOf(
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✨", fontSize = 16.sp)
                    Text("Gradient Borders", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(if (showGradients) "▲" else "▼", color = Subtle, fontSize = 11.sp)
            }
            androidx.compose.animation.AnimatedVisibility(visible = showGradients) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(gradientPatterns) { p -> GradientBubble(p) }
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
                                    type = selectedType,
                                    photos = previewBitmaps.filterNotNull().map { it.toPhotoData() },
                                    borderPx = borderPx.toInt(),
                                    borderStyle = borderStyle
                                )
                                withContext(Dispatchers.Main) {
                                    if (uri != null) {
                                        Toast.makeText(context, "✅ Saved to Pictures/PicCollage!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "❌ Save failed", Toast.LENGTH_SHORT).show()
                                    }
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
                            Text("💾  Save Collage", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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

// ─── Layout chip ─────────────────────────────────────────────────────────────
@Composable
fun LayoutChip(type: CollageType, isSelected: Boolean, onClick: () -> Unit) {
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
        LayoutThumbnail(type = type, isSelected = isSelected)
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
fun LayoutThumbnail(type: CollageType, isSelected: Boolean) {
    val cellColor = if (isSelected) Color.White.copy(alpha = 0.35f) else Color(0xFF3A3A55)
    val size = 48.dp
    val gap = 2.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Dark900)
    ) {
        when (type) {
            CollageType.SPLIT_VERTICAL -> {
                Row(Modifier.fillMaxSize().padding(3.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(cellColor, RoundedCornerShape(3.dp)))
                    Box(Modifier.weight(1f).fillMaxHeight().background(cellColor, RoundedCornerShape(3.dp)))
                }
            }
            CollageType.SPLIT_HORIZONTAL -> {
                Column(Modifier.fillMaxSize().padding(3.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Box(Modifier.weight(1f).fillMaxWidth().background(cellColor, RoundedCornerShape(3.dp)))
                    Box(Modifier.weight(1f).fillMaxWidth().background(cellColor, RoundedCornerShape(3.dp)))
                }
            }
            CollageType.BIG_LEFT -> {
                Row(Modifier.fillMaxSize().padding(3.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Box(Modifier.weight(1.2f).fillMaxHeight().background(cellColor, RoundedCornerShape(3.dp)))
                    Column(Modifier.weight(0.8f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                        Box(Modifier.weight(1f).fillMaxWidth().background(cellColor, RoundedCornerShape(3.dp)))
                        Box(Modifier.weight(1f).fillMaxWidth().background(cellColor, RoundedCornerShape(3.dp)))
                    }
                }
            }
            CollageType.BIG_RIGHT -> {
                Row(Modifier.fillMaxSize().padding(3.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Column(Modifier.weight(0.8f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                        Box(Modifier.weight(1f).fillMaxWidth().background(cellColor, RoundedCornerShape(3.dp)))
                        Box(Modifier.weight(1f).fillMaxWidth().background(cellColor, RoundedCornerShape(3.dp)))
                    }
                    Box(Modifier.weight(1.2f).fillMaxHeight().background(cellColor, RoundedCornerShape(3.dp)))
                }
            }
            CollageType.GRID_2X2 -> {
                Column(Modifier.fillMaxSize().padding(3.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        Box(Modifier.weight(1f).fillMaxHeight().background(cellColor, RoundedCornerShape(3.dp)))
                        Box(Modifier.weight(1f).fillMaxHeight().background(cellColor, RoundedCornerShape(3.dp)))
                    }
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        Box(Modifier.weight(1f).fillMaxHeight().background(cellColor, RoundedCornerShape(3.dp)))
                        Box(Modifier.weight(1f).fillMaxHeight().background(cellColor, RoundedCornerShape(3.dp)))
                    }
                }
            }
            CollageType.BIG_TOP -> {
                Column(Modifier.fillMaxSize().padding(3.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Box(Modifier.weight(1.2f).fillMaxWidth().background(cellColor, RoundedCornerShape(3.dp)))
                    Row(Modifier.weight(0.8f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(3) { Box(Modifier.weight(1f).fillMaxHeight().background(cellColor, RoundedCornerShape(3.dp))) }
                    }
                }
            }
            CollageType.BIG_BOTTOM -> {
                Column(Modifier.fillMaxSize().padding(3.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(Modifier.weight(0.8f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(3) { Box(Modifier.weight(1f).fillMaxHeight().background(cellColor, RoundedCornerShape(3.dp))) }
                    }
                    Box(Modifier.weight(1.2f).fillMaxWidth().background(cellColor, RoundedCornerShape(3.dp)))
                }
            }
            CollageType.GRID_ASYMMETRIC -> {
                Column(Modifier.fillMaxSize().padding(3.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(Modifier.weight(1.2f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        Box(Modifier.weight(1f).fillMaxHeight().background(cellColor, RoundedCornerShape(3.dp)))
                        Box(Modifier.weight(1f).fillMaxHeight().background(cellColor, RoundedCornerShape(3.dp)))
                    }
                    Row(Modifier.weight(0.8f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(3) { Box(Modifier.weight(1f).fillMaxHeight().background(cellColor, RoundedCornerShape(3.dp))) }
                    }
                }
            }
            CollageType.GRID_2X3 -> {
                Column(Modifier.fillMaxSize().padding(3.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(3) {
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            Box(Modifier.weight(1f).fillMaxHeight().background(cellColor, RoundedCornerShape(3.dp)))
                            Box(Modifier.weight(1f).fillMaxHeight().background(cellColor, RoundedCornerShape(3.dp)))
                        }
                    }
                }
            }
        }
    }
}

// ─── Live collage preview (same aspect as saved output) ──────────────────────
@Composable
fun CollagePreview(
    type: CollageType,
    photos: List<PhotoState?>,
    borderPx: Float,
    borderStyle: BorderStyle,
    onAddPhoto: (Int) -> Unit,
    onRemovePhoto: (Int) -> Unit
) {
    val gap = borderPx.dp
    val cell = @Composable { index: Int, mod: Modifier ->
        PhotoCellBox(photos.getOrNull(index), index, onAddPhoto, onRemovePhoto, mod)
    }
    Box(Modifier.fillMaxSize()) {
        when (val style = borderStyle) {
            is BorderStyle.SolidColor -> Spacer(Modifier.fillMaxSize().background(style.color))
            is BorderStyle.GradientPattern -> {
                val brush = when (style.dir) {
                    GradientDir.TOP_BOTTOM -> Brush.verticalGradient(style.colors)
                    GradientDir.LEFT_RIGHT -> Brush.horizontalGradient(style.colors)
                    GradientDir.REVERSE_DIAGONAL -> Brush.linearGradient(style.colors, start = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f), end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY))
                    GradientDir.RADIAL_CENTER -> Brush.radialGradient(style.colors)
                    GradientDir.RADIAL_CORNER -> Brush.radialGradient(style.colors, center = androidx.compose.ui.geometry.Offset(0f, 0f), radius = 800f)
                    else -> Brush.linearGradient(style.colors)
                }
                Spacer(Modifier.fillMaxSize().background(brush))
            }
        }

        Box(Modifier.fillMaxSize().padding(gap)) {
            when (type) {
            CollageType.SPLIT_VERTICAL -> {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    cell(0, Modifier.weight(1f).fillMaxHeight())
                    cell(1, Modifier.weight(1f).fillMaxHeight())
                }
            }
            CollageType.SPLIT_HORIZONTAL -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    cell(0, Modifier.weight(1f).fillMaxWidth())
                    cell(1, Modifier.weight(1f).fillMaxWidth())
                }
            }
            CollageType.BIG_LEFT -> {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    cell(0, Modifier.weight(1.2f).fillMaxHeight())
                    Column(Modifier.weight(0.8f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                        cell(1, Modifier.weight(1f).fillMaxWidth())
                        cell(2, Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
            CollageType.BIG_RIGHT -> {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Column(Modifier.weight(0.8f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                        cell(0, Modifier.weight(1f).fillMaxWidth())
                        cell(1, Modifier.weight(1f).fillMaxWidth())
                    }
                    cell(2, Modifier.weight(1.2f).fillMaxHeight())
                }
            }
            CollageType.GRID_2X2 -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        cell(0, Modifier.weight(1f).fillMaxHeight())
                        cell(1, Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        cell(2, Modifier.weight(1f).fillMaxHeight())
                        cell(3, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
            CollageType.BIG_TOP -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    cell(0, Modifier.weight(1.2f).fillMaxWidth())
                    Row(Modifier.weight(0.8f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        cell(1, Modifier.weight(1f).fillMaxHeight())
                        cell(2, Modifier.weight(1f).fillMaxHeight())
                        cell(3, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
            CollageType.BIG_BOTTOM -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(Modifier.weight(0.8f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        cell(0, Modifier.weight(1f).fillMaxHeight())
                        cell(1, Modifier.weight(1f).fillMaxHeight())
                        cell(2, Modifier.weight(1f).fillMaxHeight())
                    }
                    cell(3, Modifier.weight(1.2f).fillMaxWidth())
                }
            }
            CollageType.GRID_ASYMMETRIC -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(Modifier.weight(1.2f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        cell(0, Modifier.weight(1f).fillMaxHeight())
                        cell(1, Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(Modifier.weight(0.8f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        cell(2, Modifier.weight(1f).fillMaxHeight())
                        cell(3, Modifier.weight(1f).fillMaxHeight())
                        cell(4, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
            CollageType.GRID_2X3 -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        cell(0, Modifier.weight(1f).fillMaxHeight())
                        cell(1, Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        cell(2, Modifier.weight(1f).fillMaxHeight())
                        cell(3, Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        cell(4, Modifier.weight(1f).fillMaxHeight())
                        cell(5, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
        }
    }
}

@Composable
fun PhotoCellBox(
    state: PhotoState?,
    index: Int,
    onAddPhoto: (Int) -> Unit,
    onRemovePhoto: (Int) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier.clip(RoundedCornerShape(6.dp)).background(Dark700)) {
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
                        .clip(androidx.compose.foundation.shape.CircleShape)
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
                        .clip(androidx.compose.foundation.shape.CircleShape)
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
                    modifier = Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text("＋", color = Pink500, fontSize = 24.sp)
                }
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
            .pointerInput(Unit) {
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

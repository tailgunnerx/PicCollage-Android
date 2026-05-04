package com.tailgunnerx.piccollage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.SweepGradient
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

data class PhotoData(
    val bitmap: Bitmap,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val zoom: Float = 1f
)

/**
 * Renders the selected collage layout to a Bitmap at the given [outputSize],
 * then saves it to the device gallery and returns the saved Uri.
 */
suspend fun renderAndSaveCollage(
    context: Context,
    type: CollageType,
    photos: List<PhotoData>,
    outputSize: IntSize = IntSize(1080, 1080),
    borderPx: Int = 8,
    borderStyle: BorderStyle = BorderStyle.SolidColor(androidx.compose.ui.graphics.Color.White)
): Uri? = withContext(Dispatchers.IO) {

    val W = outputSize.width
    val H = outputSize.height

    val output = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    
    // Fill background with the border style
    when (val style = borderStyle) {
        is BorderStyle.SolidColor -> canvas.drawColor(style.color.toArgb())
        is BorderStyle.GradientPattern -> {
            val colors = style.colors.map { it.toArgb() }.toIntArray()
            val positions = FloatArray(colors.size) { i -> i.toFloat() / (colors.size - 1) }
            val shader = when (style.dir) {
                GradientDir.TOP_BOTTOM -> android.graphics.LinearGradient(0f, 0f, 0f, H.toFloat(), colors, positions, android.graphics.Shader.TileMode.CLAMP)
                GradientDir.LEFT_RIGHT -> android.graphics.LinearGradient(0f, 0f, W.toFloat(), 0f, colors, positions, android.graphics.Shader.TileMode.CLAMP)
                GradientDir.REVERSE_DIAGONAL -> android.graphics.LinearGradient(W.toFloat(), 0f, 0f, H.toFloat(), colors, positions, android.graphics.Shader.TileMode.CLAMP)
                GradientDir.RADIAL_CENTER -> android.graphics.RadialGradient(W / 2f, H / 2f, maxOf(W, H) / 2f, colors, positions, android.graphics.Shader.TileMode.CLAMP)
                GradientDir.RADIAL_CORNER -> android.graphics.RadialGradient(0f, 0f, maxOf(W, H).toFloat(), colors, positions, android.graphics.Shader.TileMode.CLAMP)
                else -> android.graphics.LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), colors, positions, android.graphics.Shader.TileMode.CLAMP)
            }
            val p = Paint().apply { this.shader = shader }
            canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        }
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    // Scale borderPx based on the ratio of Output Width vs standard 360dp screen width
    val bp = (borderPx * (W / 360f)).toInt()
    val halfBp = bp / 2

    fun drawPhoto(state: PhotoData, dst: Rect) {
        val bmp = state.bitmap
        // Scale-to-fill (center crop)
        val srcAspect = bmp.width.toFloat() / bmp.height
        val dstAspect = dst.width().toFloat() / dst.height()
        val src: Rect
        val newW: Float
        val newH: Float
        if (srcAspect > dstAspect) {
            newW = bmp.height * dstAspect
            newH = bmp.height.toFloat()
        } else {
            newW = bmp.width.toFloat()
            newH = bmp.width / dstAspect
        }

        val scaledW = newW / state.zoom
        val scaledH = newH / state.zoom

        val dxOffset = state.panX * scaledW
        val dyOffset = state.panY * scaledH

        val dx = ((bmp.width - scaledW) / 2) - dxOffset
        val clampedDx = dx.coerceIn(0f, bmp.width - scaledW)

        val dy = ((bmp.height - scaledH) / 2) - dyOffset
        val clampedDy = dy.coerceIn(0f, bmp.height - scaledH)

        src = Rect(clampedDx.toInt(), clampedDy.toInt(), (clampedDx + scaledW).toInt(), (clampedDy + scaledH).toInt())
        canvas.drawBitmap(bmp, src, dst, paint)
    }

    val b = photos
    when (type) {

        // ── 2 photos ─────────────────────────────────────────────────────────
        CollageType.SPLIT_VERTICAL -> {
            val mid = W / 2
            drawPhoto(b[0], Rect(bp, bp, mid - halfBp, H - bp))
            drawPhoto(b[1], Rect(mid + halfBp, bp, W - bp, H - bp))
        }
        CollageType.SPLIT_HORIZONTAL -> {
            val mid = H / 2
            drawPhoto(b[0], Rect(bp, bp, W - bp, mid - halfBp))
            drawPhoto(b[1], Rect(bp, mid + halfBp, W - bp, H - bp))
        }

        // ── 3 photos ─────────────────────────────────────────────────────────
        CollageType.BIG_LEFT -> {
            val split = (W * 0.55f).toInt()
            val midY = H / 2
            drawPhoto(b[0], Rect(bp, bp, split - halfBp, H - bp))
            drawPhoto(b[1], Rect(split + halfBp, bp, W - bp, midY - halfBp))
            drawPhoto(b[2], Rect(split + halfBp, midY + halfBp, W - bp, H - bp))
        }
        CollageType.BIG_RIGHT -> {
            val split = (W * 0.45f).toInt()
            val midY = H / 2
            drawPhoto(b[0], Rect(bp, bp, split - halfBp, midY - halfBp))
            drawPhoto(b[1], Rect(bp, midY + halfBp, split - halfBp, H - bp))
            drawPhoto(b[2], Rect(split + halfBp, bp, W - bp, H - bp))
        }

        // ── 4 photos ─────────────────────────────────────────────────────────
        CollageType.GRID_2X2 -> {
            val midX = W / 2; val midY = H / 2
            drawPhoto(b[0], Rect(bp, bp, midX - halfBp, midY - halfBp))
            drawPhoto(b[1], Rect(midX + halfBp, bp, W - bp, midY - halfBp))
            drawPhoto(b[2], Rect(bp, midY + halfBp, midX - halfBp, H - bp))
            drawPhoto(b[3], Rect(midX + halfBp, midY + halfBp, W - bp, H - bp))
        }
        CollageType.BIG_TOP -> {
            val splitY = (H * 0.55f).toInt()
            val t1 = W / 3; val t2 = (W * 2f / 3f).toInt()
            drawPhoto(b[0], Rect(bp, bp, W - bp, splitY - halfBp))
            drawPhoto(b[1], Rect(bp, splitY + halfBp, t1 - halfBp, H - bp))
            drawPhoto(b[2], Rect(t1 + halfBp, splitY + halfBp, t2 - halfBp, H - bp))
            drawPhoto(b[3], Rect(t2 + halfBp, splitY + halfBp, W - bp, H - bp))
        }
        CollageType.BIG_BOTTOM -> {
            val splitY = (H * 0.45f).toInt()
            val t1 = W / 3; val t2 = (W * 2f / 3f).toInt()
            drawPhoto(b[0], Rect(bp, bp, t1 - halfBp, splitY - halfBp))
            drawPhoto(b[1], Rect(t1 + halfBp, bp, t2 - halfBp, splitY - halfBp))
            drawPhoto(b[2], Rect(t2 + halfBp, bp, W - bp, splitY - halfBp))
            drawPhoto(b[3], Rect(bp, splitY + halfBp, W - bp, H - bp))
        }

        // ── 5 photos ─────────────────────────────────────────────────────────
        CollageType.GRID_ASYMMETRIC -> {
            val midX = W / 2; val midY = (H * 0.55f).toInt()
            val t1 = W / 3; val t2 = (W * 2f / 3f).toInt()
            drawPhoto(b[0], Rect(bp, bp, midX - halfBp, midY - halfBp))
            drawPhoto(b[1], Rect(midX + halfBp, bp, W - bp, midY - halfBp))
            drawPhoto(b[2], Rect(bp, midY + halfBp, t1 - halfBp, H - bp))
            drawPhoto(b[3], Rect(t1 + halfBp, midY + halfBp, t2 - halfBp, H - bp))
            drawPhoto(b[4], Rect(t2 + halfBp, midY + halfBp, W - bp, H - bp))
        }

        // ── 6 photos ─────────────────────────────────────────────────────────
        CollageType.GRID_2X3 -> {
            val midX = W / 2; val r1 = H / 3; val r2 = (H * 2f / 3f).toInt()
            drawPhoto(b[0], Rect(bp, bp, midX - halfBp, r1 - halfBp))
            drawPhoto(b[1], Rect(midX + halfBp, bp, W - bp, r1 - halfBp))
            drawPhoto(b[2], Rect(bp, r1 + halfBp, midX - halfBp, r2 - halfBp))
            drawPhoto(b[3], Rect(midX + halfBp, r1 + halfBp, W - bp, r2 - halfBp))
            drawPhoto(b[4], Rect(bp, r2 + halfBp, midX - halfBp, H - bp))
            drawPhoto(b[5], Rect(midX + halfBp, r2 + halfBp, W - bp, H - bp))
        }
    }

    // Save to gallery
    val filename = "PicCollage_${System.currentTimeMillis()}.jpg"
    val mimeType = "image/jpeg"
    val stream: OutputStream?
    val uri: Uri?

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PicCollage")
        }
        uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        stream = uri?.let { context.contentResolver.openOutputStream(it) }
    } else {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = java.io.File(dir, filename)
        stream = java.io.FileOutputStream(file)
        uri = Uri.fromFile(file)
    }

    stream?.use { out ->
        output.compress(Bitmap.CompressFormat.JPEG, 95, out)
    }
    output.recycle()
    uri
}

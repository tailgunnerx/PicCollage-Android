package com.tailgunnerx.piccollage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
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
 * Renders the collage to a Bitmap at [outputSize] and saves it to the gallery.
 *
 * [borderPx] and [cornerRadius] are in preview dp; they're scaled by
 * `outputWidth / previewWidthDp` so the saved image matches what's on screen.
 */
suspend fun renderAndSaveCollage(
    context: Context,
    cells: List<CellRect>,
    photos: List<PhotoData>,
    outputSize: IntSize = IntSize(1080, 1080),
    borderPx: Float = 8f,
    cornerRadius: Float = 0f,
    previewWidthDp: Float = 360f,
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
                GradientDir.TOP_BOTTOM -> android.graphics.LinearGradient(0f, 0f, 0f, H.toFloat(), colors, positions, Shader.TileMode.CLAMP)
                GradientDir.LEFT_RIGHT -> android.graphics.LinearGradient(0f, 0f, W.toFloat(), 0f, colors, positions, Shader.TileMode.CLAMP)
                GradientDir.REVERSE_DIAGONAL -> android.graphics.LinearGradient(W.toFloat(), 0f, 0f, H.toFloat(), colors, positions, Shader.TileMode.CLAMP)
                GradientDir.RADIAL_CENTER -> android.graphics.RadialGradient(W / 2f, H / 2f, maxOf(W, H) / 2f, colors, positions, Shader.TileMode.CLAMP)
                GradientDir.RADIAL_CORNER -> android.graphics.RadialGradient(0f, 0f, maxOf(W, H).toFloat(), colors, positions, Shader.TileMode.CLAMP)
                GradientDir.SWEEP -> android.graphics.SweepGradient(W / 2f, H / 2f, colors, positions)
                else -> android.graphics.LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), colors, positions, Shader.TileMode.CLAMP)
            }
            val p = Paint().apply { this.shader = shader }
            canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        }
    }

    val scale = W / previewWidthDp.coerceAtLeast(1f)
    val gap = borderPx * scale
    val radius = cornerRadius * scale

    fun drawPhoto(state: PhotoData, dst: RectF) {
        val bmp = state.bitmap
        // Scale-to-fill (center crop), then apply the user's zoom/pan.
        val srcAspect = bmp.width.toFloat() / bmp.height
        val dstAspect = dst.width() / dst.height()
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

        val dx = ((bmp.width - scaledW) / 2) - state.panX * scaledW
        val clampedDx = dx.coerceIn(0f, bmp.width - scaledW)
        val dy = ((bmp.height - scaledH) / 2) - state.panY * scaledH
        val clampedDy = dy.coerceIn(0f, bmp.height - scaledH)

        val src = RectF(clampedDx, clampedDy, clampedDx + scaledW, clampedDy + scaledH)

        // A bitmap shader lets us draw anti-aliased rounded corners.
        val matrix = Matrix().apply { setRectToRect(src, dst, Matrix.ScaleToFit.FILL) }
        val shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(matrix) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
        if (radius > 0f) canvas.drawRoundRect(dst, radius, radius, paint) else canvas.drawRect(dst, paint)
    }

    cells.forEachIndexed { i, cell ->
        val photo = photos.getOrNull(i) ?: return@forEachIndexed
        val r = cell.toPx(W.toFloat(), H.toFloat(), gap)
        drawPhoto(photo, RectF(r.left, r.top, r.right, r.bottom))
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

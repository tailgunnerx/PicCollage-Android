package com.tailgunnerx.piccollage

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class HolidayTheme(
    val label: String,
    val season: String,
    val emoji: String
) {
    CANDY_CANE("Candy Cane", "Christmas", "🍬"),
    SNOWY_PINE("Snowy Pine", "Christmas", "🎄"),
    WINTER_FROST("Winter Frost", "Winter", "❄️"),
    NEW_YEARS_GLAM("New Year's", "New Year", "🥂"),
    SWEETHEART("Sweetheart", "Valentine's", "💘"),
    LUCKY_CLOVER("Lucky Clover", "St. Patrick's", "🍀"),
    EASTER_PASTEL("Easter Egg", "Easter", "🐣"),
    STAR_SPANGLED("Star-Spangled", "4th of July", "🎆"),
    SPOOKY_NIGHT("Spooky Night", "Halloween", "🎃"),
    PUMPKIN_STRIPE("Witchy Stripe", "Halloween", "🦇"),
    HARVEST_PLAID("Harvest Plaid", "Thanksgiving", "🍂"),
    HANUKKAH_LIGHTS("Hanukkah", "Hanukkah", "🕎")
}

/**
 * Draws a procedural holiday border background onto [canvas].
 * Shared by the thumbnail chips, the live Compose preview, and the high-res JPEG renderer
 * so every stripe, snowflake, heart, clover, star, and tartan weave matches 1-to-1.
 */
fun drawHolidayBackground(
    canvas: Canvas,
    w: Float,
    h: Float,
    theme: HolidayTheme,
    patternScale: Float = 1f
) {
    if (w <= 0f || h <= 0f) return
    val minSide = minOf(w, h)
    val unit = (minSide / 10.5f) * patternScale

    when (theme) {
        HolidayTheme.CANDY_CANE -> {
            drawDiagonalStripes(
                canvas, w, h, period = unit * 0.95f,
                bands = listOf(
                    0xFFFFFDF9.toInt() to 0.36f,
                    0xFFD90429.toInt() to 0.30f,
                    0xFFFFFDF9.toInt() to 0.10f,
                    0xFF1B7A43.toInt() to 0.10f,
                    0xFFFFFDF9.toInt() to 0.08f,
                    0xFFD90429.toInt() to 0.06f
                )
            )
        }

        HolidayTheme.SNOWY_PINE -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF092618.toInt(), 0xFF164A30.toInt(), 0xFF0D3521.toInt()))
            val snowPaint = strokePaint(0xFFF2F8FF.toInt(), unit * 0.045f)
            val goldPaint = fillPaint(0xFFFFD54F.toInt())
            val berryPaint = fillPaint(0xFFE53935.toInt())
            val leafPaint = fillPaint(0xFF2E8B57.toInt())
            forEachTile(w, h, unit) { r, c, cx, cy ->
                when ((r * 3 + c) and 3) {
                    0 -> drawSnowflake(canvas, cx, cy, unit * 0.27f, snowPaint)
                    1 -> drawSparkle4(canvas, cx, cy, unit * 0.18f, goldPaint)
                    2 -> drawHollyBerries(canvas, cx, cy, unit * 0.16f, berryPaint, leafPaint)
                    else -> {
                        canvas.drawCircle(cx - unit * 0.12f, cy - unit * 0.1f, unit * 0.055f, fillPaint(0xCCF2F8FF.toInt()))
                        drawSparkle4(canvas, cx + unit * 0.14f, cy + unit * 0.12f, unit * 0.13f, goldPaint)
                    }
                }
            }
        }

        HolidayTheme.WINTER_FROST -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF0A2240.toInt(), 0xFF1B5288.toInt(), 0xFF4392C9.toInt(), 0xFF164270.toInt()))
            val whiteStroke = strokePaint(0xFFF5FBFF.toInt(), unit * 0.045f)
            val iceStroke = strokePaint(0xFFB3E5FC.toInt(), unit * 0.038f)
            val dotPaint = fillPaint(0xDDE1F5FE.toInt())
            forEachTile(w, h, unit * 0.95f) { r, c, cx, cy ->
                if ((r + c) % 2 == 0) {
                    drawSnowflake(canvas, cx, cy, unit * 0.29f, whiteStroke)
                } else {
                    drawSnowflake(canvas, cx, cy, unit * 0.19f, iceStroke)
                    canvas.drawCircle(cx + unit * 0.24f, cy - unit * 0.22f, unit * 0.045f, dotPaint)
                }
            }
        }

        HolidayTheme.NEW_YEARS_GLAM -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF09090F.toInt(), 0xFF191726.toInt(), 0xFF0D0C14.toInt()))
            val gold = fillPaint(0xFFFFD700.toInt())
            val champagne = fillPaint(0xFFF7E7CE.toInt())
            val roseGold = fillPaint(0xFFE8A598.toInt())
            val rect = RectF()
            forEachTile(w, h, unit * 0.9f) { r, c, cx, cy ->
                val kind = (r * 5 + c * 3).mod(4)
                when (kind) {
                    0 -> drawSparkle4(canvas, cx, cy, unit * 0.26f, gold)
                    1 -> {
                        canvas.save()
                        canvas.translate(cx, cy)
                        canvas.rotate(((r * 37 + c * 53) % 180).toFloat())
                        rect.set(-unit * 0.16f, -unit * 0.055f, unit * 0.16f, unit * 0.055f)
                        canvas.drawRoundRect(rect, unit * 0.03f, unit * 0.03f, champagne)
                        canvas.restore()
                    }
                    2 -> drawSparkle4(canvas, cx, cy, unit * 0.18f, roseGold)
                    else -> {
                        canvas.drawCircle(cx - unit * 0.1f, cy, unit * 0.07f, gold)
                        canvas.drawCircle(cx + unit * 0.15f, cy + unit * 0.12f, unit * 0.045f, champagne)
                    }
                }
            }
        }

        HolidayTheme.SWEETHEART -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF590D22.toInt(), 0xFFA4133C.toInt(), 0xFFFF4D6D.toInt(), 0xFFFF8FA3.toInt()))
            val blush = fillPaint(0xFFFFF0F3.toInt())
            val pink = fillPaint(0xFFFFB3C1.toInt())
            val ruby = fillPaint(0xFFFFCCD5.toInt())
            forEachTile(w, h, unit * 0.92f) { r, c, cx, cy ->
                val tilt = if ((r + c) % 2 == 0) -14f else 14f
                val p = when ((r + c * 2).mod(3)) {
                    0 -> blush
                    1 -> pink
                    else -> ruby
                }
                val scale = if ((r + c) % 2 == 0) 0.24f else 0.17f
                drawHeart(canvas, cx, cy, unit * scale, tilt, p)
            }
        }

        HolidayTheme.LUCKY_CLOVER -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF06371B.toInt(), 0xFF116936.toInt(), 0xFF1E8449.toInt(), 0xFF0A4824.toInt()))
            val lightClover = fillPaint(0xFF82E0AA.toInt())
            val mintClover = fillPaint(0xFFABEBC6.toInt())
            val goldCoin = fillPaint(0xFFFFD54F.toInt())
            forEachTile(w, h, unit * 0.95f) { r, c, cx, cy ->
                if ((r + c) % 2 == 0) {
                    drawShamrock(canvas, cx, cy, unit * 0.24f, 15f, if (r % 2 == 0) lightClover else mintClover)
                } else {
                    drawSparkle4(canvas, cx, cy, unit * 0.15f, goldCoin)
                    canvas.drawCircle(cx + unit * 0.2f, cy - unit * 0.18f, unit * 0.055f, goldCoin)
                }
            }
        }

        HolidayTheme.EASTER_PASTEL -> {
            val bandColors = intArrayOf(
                0xFFFFD6E0.toInt(),
                0xFFFFF3B0.toInt(),
                0xFFC8F7DC.toInt(),
                0xFFC4ECFF.toInt(),
                0xFFE5D4FF.toInt()
            )
            val bandH = unit * 0.82f
            val rows = ceil(h / bandH).toInt() + 1
            val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val dotWhite = fillPaint(0xEEFFFFFF.toInt())
            val zigStroke = strokePaint(0xCCFFFFFF.toInt(), unit * 0.045f)
            for (r in 0 until rows) {
                val top = r * bandH
                bandPaint.color = bandColors[r.mod(bandColors.size)]
                canvas.drawRect(0f, top, w, top + bandH + 1f, bandPaint)
                val cy = top + bandH * 0.5f
                if (r % 2 == 0) {
                    val step = unit * 0.55f
                    var x = (r % 3) * unit * 0.15f
                    while (x < w + step) {
                        canvas.drawCircle(x, cy, unit * 0.11f, dotWhite)
                        x += step
                    }
                } else {
                    val path = Path()
                    val step = unit * 0.36f
                    val amp = unit * 0.11f
                    path.moveTo(0f, cy)
                    var x = 0f
                    var up = true
                    while (x < w + step) {
                        path.lineTo(x + step * 0.5f, if (up) cy - amp else cy + amp)
                        path.lineTo(x + step, cy)
                        x += step
                        up = !up
                    }
                    canvas.drawPath(path, zigStroke)
                }
            }
        }

        HolidayTheme.STAR_SPANGLED -> {
            // Diagonal crimson/white stripes on one layer + deep navy starfield overlay feel:
            // Deep patriotic gradient with crisp 5-point white, crimson-accent & gold stars.
            drawLinearBg(canvas, w, h, intArrayOf(0xFF0A2463.toInt(), 0xFF1E3F8F.toInt(), 0xFF9E1B32.toInt(), 0xFF0A2463.toInt()))
            val whiteStar = fillPaint(0xFFF8F9FA.toInt())
            val redStar = fillPaint(0xFFFF4D5E.toInt())
            val goldStar = fillPaint(0xFFFFD166.toInt())
            forEachTile(w, h, unit * 0.9f) { r, c, cx, cy ->
                when ((r + c * 2).mod(3)) {
                    0 -> drawStar5(canvas, cx, cy, unit * 0.25f, unit * 0.105f, -90f, whiteStar)
                    1 -> drawStar5(canvas, cx, cy, unit * 0.19f, unit * 0.08f, -72f, redStar)
                    else -> drawSparkle4(canvas, cx, cy, unit * 0.15f, goldStar)
                }
            }
        }

        HolidayTheme.SPOOKY_NIGHT -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF120524.toInt(), 0xFF3C096C.toInt(), 0xFF6A1B9A.toInt(), 0xFFE65100.toInt()))
            val moonPaint = fillPaint(0xFFFFD54F.toInt())
            val pumpkinPaint = fillPaint(0xFFFF7900.toInt())
            val starPaint = fillPaint(0xFFE0AAFF.toInt())
            forEachTile(w, h, unit * 0.95f) { r, c, cx, cy ->
                when ((r * 3 + c).mod(3)) {
                    0 -> drawCrescentMoon(canvas, cx, cy, unit * 0.23f, -25f, moonPaint)
                    1 -> drawBat(canvas, cx, cy, unit * 0.26f, fillPaint(0xFF100018.toInt()))
                    else -> {
                        drawSparkle4(canvas, cx, cy, unit * 0.16f, pumpkinPaint)
                        canvas.drawCircle(cx + unit * 0.18f, cy - unit * 0.18f, unit * 0.045f, starPaint)
                    }
                }
            }
        }

        HolidayTheme.PUMPKIN_STRIPE -> {
            drawDiagonalStripes(
                canvas, w, h, period = unit * 0.95f,
                bands = listOf(
                    0xFF141118.toInt() to 0.36f,
                    0xFFFF6D00.toInt() to 0.32f,
                    0xFFFFD166.toInt() to 0.08f,
                    0xFF7B2CBF.toInt() to 0.24f
                )
            )
        }

        HolidayTheme.HARVEST_PLAID -> {
            // Authentic woven autumn tartan
            canvas.drawColor(0xFF36150E.toInt())
            val period = unit * 1.15f
            val orangeBand = fillPaint(0x88E06D1B.toInt())
            val cranberryBand = fillPaint(0x889E2A2B.toInt())
            val sageBand = fillPaint(0x66588157.toInt())
            val goldLine = strokePaint(0xCCF4C95D.toInt(), maxOf(1.5f, unit * 0.035f))
            val creamLine = strokePaint(0x99FFF3B0.toInt(), maxOf(1f, unit * 0.022f))

            var x = 0f
            while (x < w + period) {
                canvas.drawRect(x, 0f, x + period * 0.34f, h, orangeBand)
                canvas.drawRect(x + period * 0.50f, 0f, x + period * 0.72f, h, cranberryBand)
                canvas.drawRect(x + period * 0.78f, 0f, x + period * 0.88f, h, sageBand)
                canvas.drawLine(x + period * 0.17f, 0f, x + period * 0.17f, h, goldLine)
                canvas.drawLine(x + period * 0.61f, 0f, x + period * 0.61f, h, creamLine)
                x += period
            }
            var y = 0f
            while (y < h + period) {
                canvas.drawRect(0f, y, w, y + period * 0.34f, orangeBand)
                canvas.drawRect(0f, y + period * 0.50f, w, y + period * 0.72f, cranberryBand)
                canvas.drawRect(0f, y + period * 0.78f, w, y + period * 0.88f, sageBand)
                canvas.drawLine(0f, y + period * 0.17f, w, y + period * 0.17f, goldLine)
                canvas.drawLine(0f, y + period * 0.61f, w, y + period * 0.61f, creamLine)
                y += period
            }
        }

        HolidayTheme.HANUKKAH_LIGHTS -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF041C4A.toInt(), 0xFF0C3C8C.toInt(), 0xFF1D63C2.toInt(), 0xFF062663.toInt()))
            val silverStroke = strokePaint(0xFFEAF4FF.toInt(), unit * 0.042f)
            val goldFill = fillPaint(0xFFFFDF73.toInt())
            val iceFill = fillPaint(0xFFBBE1FA.toInt())
            forEachTile(w, h, unit * 0.95f) { r, c, cx, cy ->
                if ((r + c) % 2 == 0) {
                    drawStarOfDavid(canvas, cx, cy, unit * 0.25f, silverStroke)
                } else {
                    drawSparkle4(canvas, cx, cy, unit * 0.17f, goldFill)
                    canvas.drawCircle(cx - unit * 0.18f, cy + unit * 0.16f, unit * 0.045f, iceFill)
                }
            }
        }
    }
}

// ─── Pattern primitives ──────────────────────────────────────────────────────

private fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    this.color = color
}

private fun strokePaint(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
    strokeWidth = width.coerceAtLeast(1.2f)
    this.color = color
}

private fun drawLinearBg(canvas: Canvas, w: Float, h: Float, colors: IntArray) {
    val positions = FloatArray(colors.size) { i -> i.toFloat() / (colors.size - 1) }
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, w, h, colors, positions, Shader.TileMode.CLAMP)
    }
    canvas.drawRect(0f, 0f, w, h, p)
}

private fun drawDiagonalStripes(
    canvas: Canvas,
    w: Float,
    h: Float,
    period: Float,
    bands: List<Pair<Int, Float>>
) {
    val diag = hypot(w, h)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    canvas.save()
    canvas.translate(w / 2f, h / 2f)
    canvas.rotate(-45f)
    var x = -diag
    val end = diag
    val totalWeight = bands.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(0.01f)
    while (x < end) {
        var cur = x
        for ((color, weight) in bands) {
            val bw = period * (weight / totalWeight)
            paint.color = color
            canvas.drawRect(cur, -diag, cur + bw + 0.8f, diag, paint)
            cur += bw
        }
        x += period
    }
    canvas.restore()
}

private inline fun forEachTile(
    w: Float,
    h: Float,
    step: Float,
    block: (row: Int, col: Int, cx: Float, cy: Float) -> Unit
) {
    val rowH = step * 0.86f
    val rows = ceil(h / rowH).toInt() + 1
    val cols = ceil(w / step).toInt() + 1
    for (r in 0 until rows) {
        val xOffset = if (r % 2 == 0) step * 0.35f else step * 0.85f
        val cy = r * rowH + rowH * 0.42f
        for (c in -1 until cols) {
            val cx = c * step + xOffset
            block(r, c + 1, cx, cy)
        }
    }
}

private fun drawSnowflake(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
    for (i in 0 until 6) {
        val ang = (i * 60.0) * PI / 180.0
        val dx = cos(ang).toFloat()
        val dy = sin(ang).toFloat()
        canvas.drawLine(cx, cy, cx + dx * r, cy + dy * r, paint)
        val bx = cx + dx * r * 0.56f
        val by = cy + dy * r * 0.56f
        val tick = r * 0.28f
        val a1 = ang + PI / 4.0
        val a2 = ang - PI / 4.0
        canvas.drawLine(bx, by, bx + cos(a1).toFloat() * tick, by + sin(a1).toFloat() * tick, paint)
        canvas.drawLine(bx, by, bx + cos(a2).toFloat() * tick, by + sin(a2).toFloat() * tick, paint)
    }
}

private fun drawSparkle4(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
    val p = Path().apply {
        moveTo(cx, cy - r)
        quadTo(cx, cy, cx + r, cy)
        quadTo(cx, cy, cx, cy + r)
        quadTo(cx, cy, cx - r, cy)
        quadTo(cx, cy, cx, cy - r)
        close()
    }
    canvas.drawPath(p, paint)
}

private fun drawStar5(canvas: Canvas, cx: Float, cy: Float, outerR: Float, innerR: Float, startDeg: Float, paint: Paint) {
    val path = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) outerR else innerR
        val ang = (startDeg + i * 36f) * (PI / 180.0)
        val px = cx + cos(ang).toFloat() * rad
        val py = cy + sin(ang).toFloat() * rad
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    canvas.drawPath(path, paint)
}

private fun drawStarOfDavid(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
    fun tri(offsetDeg: Float): Path {
        val p = Path()
        for (i in 0 until 3) {
            val ang = (offsetDeg + i * 120f) * (PI / 180.0)
            val px = cx + cos(ang).toFloat() * r
            val py = cy + sin(ang).toFloat() * r
            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
        }
        p.close()
        return p
    }
    canvas.drawPath(tri(-90f), paint)
    canvas.drawPath(tri(90f), paint)
}

private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, r: Float, rotDeg: Float, paint: Paint) {
    canvas.save()
    canvas.translate(cx, cy)
    canvas.rotate(rotDeg)
    val p = Path().apply {
        moveTo(0f, 0.82f * r)
        cubicTo(-0.15f * r, 0.55f * r, -1.05f * r, 0.15f * r, -0.95f * r, -0.32f * r)
        cubicTo(-0.85f * r, -0.78f * r, -0.18f * r, -0.82f * r, 0f, -0.32f * r)
        cubicTo(0.18f * r, -0.82f * r, 0.85f * r, -0.78f * r, 0.95f * r, -0.32f * r)
        cubicTo(1.05f * r, 0.15f * r, 0.15f * r, 0.55f * r, 0f, 0.82f * r)
        close()
    }
    canvas.drawPath(p, paint)
    canvas.restore()
}

private fun drawShamrock(canvas: Canvas, cx: Float, cy: Float, r: Float, rotDeg: Float, paint: Paint) {
    canvas.save()
    canvas.translate(cx, cy)
    canvas.rotate(rotDeg)
    val leaf = r * 0.54f
    for (i in 0 until 4) {
        canvas.save()
        canvas.rotate(i * 90f)
        drawHeart(canvas, 0f, -r * 0.46f, leaf, 0f, paint)
        canvas.restore()
    }
    val stem = strokePaint(paint.color, r * 0.16f)
    val stemPath = Path().apply {
        moveTo(0f, r * 0.1f)
        quadTo(r * 0.12f, r * 0.75f, r * 0.42f, r * 1.05f)
    }
    canvas.drawPath(stemPath, stem)
    canvas.restore()
}

private fun drawCrescentMoon(canvas: Canvas, cx: Float, cy: Float, r: Float, rotDeg: Float, paint: Paint) {
    canvas.save()
    canvas.translate(cx, cy)
    canvas.rotate(rotDeg)
    val outer = Path().apply { addCircle(0f, 0f, r, Path.Direction.CW) }
    val inner = Path().apply { addCircle(r * 0.40f, -r * 0.12f, r * 0.84f, Path.Direction.CW) }
    outer.op(inner, Path.Op.DIFFERENCE)
    canvas.drawPath(outer, paint)
    canvas.restore()
}

private fun drawBat(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
    val p = Path().apply {
        moveTo(cx, cy - r * 0.22f)
        lineTo(cx - r * 0.16f, cy - r * 0.42f) // left ear
        lineTo(cx - r * 0.24f, cy - r * 0.18f)
        quadTo(cx - r * 0.72f, cy - r * 0.55f, cx - r, cy + r * 0.05f) // left wingtip
        quadTo(cx - r * 0.65f, cy - r * 0.05f, cx - r * 0.48f, cy + r * 0.26f)
        quadTo(cx - r * 0.22f, cy + r * 0.05f, cx, cy + r * 0.34f) // tail
        quadTo(cx + r * 0.22f, cy + r * 0.05f, cx + r * 0.48f, cy + r * 0.26f)
        quadTo(cx + r * 0.65f, cy - r * 0.05f, cx + r, cy + r * 0.05f) // right wingtip
        quadTo(cx + r * 0.72f, cy - r * 0.55f, cx + r * 0.24f, cy - r * 0.18f)
        lineTo(cx + r * 0.16f, cy - r * 0.42f) // right ear
        close()
    }
    canvas.drawPath(p, paint)
}

private fun drawHollyBerries(canvas: Canvas, cx: Float, cy: Float, r: Float, berryPaint: Paint, leafPaint: Paint) {
    for (dir in intArrayOf(-1, 1)) {
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(dir * 38f)
        val leaf = Path().apply {
            moveTo(0f, 0f)
            quadTo(dir * r * 0.9f, -r * 0.7f, dir * r * 1.55f, 0f)
            quadTo(dir * r * 0.9f, r * 0.7f, 0f, 0f)
            close()
        }
        canvas.drawPath(leaf, leafPaint)
        canvas.restore()
    }
    val br = r * 0.36f
    canvas.drawCircle(cx - br * 0.85f, cy + br * 0.45f, br, berryPaint)
    canvas.drawCircle(cx + br * 0.85f, cy + br * 0.45f, br, berryPaint)
    canvas.drawCircle(cx, cy - br * 0.55f, br, berryPaint)
}

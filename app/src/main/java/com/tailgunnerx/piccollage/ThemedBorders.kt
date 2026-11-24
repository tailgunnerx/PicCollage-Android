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

enum class PatternTheme(
    val label: String,
    val emoji: String
) {
    // Spooky & Gothic
    BONE_YARD("Bone Yard", "💀"),
    COBWEB_LACE("Cobweb", "🕸️"),
    GHOSTLY_MIST("Ghostly", "👻"),
    SPOOKY_NIGHT("Spooky Night", "🎃"),
    PUMPKIN_STRIPE("Witchy Stripe", "🦇"),

    // Outdoors, Coastal & Vibes
    BEACH_VIBES("Beach Vibes", "🌊"),
    TROPICAL_PALM("Tropical", "🌴"),
    CAMPING_NIGHT("Camping", "🏕️"),
    CELESTIAL("Celestial", "🪐"),
    WILDFLOWER("Wildflower", "🌼"),

    // Baby, Pets, Milestones & Aesthetic
    BABY_BLUE("Baby Blue", "☁️"),
    BABY_PINK("Baby Pink", "🌸"),
    BIRTHDAY("Birthday", "🎉"),
    PET_PAWS("Pet Paws", "🐾"),
    CHECKERBOARD("Checker", "🏁"),

    // Seasonal & Holidays
    CANDY_CANE("Candy Cane", "🍬"),
    SNOWY_PINE("Snowy Pine", "🎄"),
    WINTER_FROST("Winter Frost", "❄️"),
    HARVEST_PLAID("Harvest Plaid", "🍂"),
    NEW_YEARS_GLAM("New Year's", "🥂"),
    SWEETHEART("Sweetheart", "💘"),
    LUCKY_CLOVER("Lucky Clover", "🍀"),
    EASTER_PASTEL("Easter Egg", "🐣"),
    STAR_SPANGLED("Star-Spangled", "🎆")
}

/**
 * Draws a procedural themed border background onto [canvas].
 * Shared by the thumbnail cards, the live Compose preview, and the high-res JPEG renderer
 * so every motif, stripe, wave, skull, cloud, tent, and plaid weave matches 1-to-1.
 */
fun drawThemedBackground(
    canvas: Canvas,
    w: Float,
    h: Float,
    theme: PatternTheme,
    patternScale: Float = 1f
) {
    if (w <= 0f || h <= 0f) return
    val minSide = minOf(w, h)
    val unit = (minSide / 10.5f) * patternScale

    when (theme) {
        // ─── Spooky & Gothic ──────────────────────────────────────────────────
        PatternTheme.BONE_YARD -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF0B0B10.toInt(), 0xFF191722.toInt(), 0xFF0E0D14.toInt()))
            val bone = fillPaint(0xFFF2EDE2.toInt())
            val boneMuted = fillPaint(0xFFD8D0C1.toInt())
            val dark = fillPaint(0xFF121018.toInt())
            val darkStroke = strokePaint(0xFF121018.toInt(), unit * 0.032f)
            forEachTile(w, h, unit * 0.98f) { r, c, cx, cy ->
                if ((r + c) % 2 == 0) {
                    val tilt = if (r % 2 == 0) -10f else 10f
                    drawSkull(canvas, cx, cy, unit * 0.27f, tilt, bone, dark, darkStroke)
                } else {
                    drawCrossbones(canvas, cx, cy, unit * 0.26f, boneMuted)
                }
            }
        }

        PatternTheme.COBWEB_LACE -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF090510.toInt(), 0xFF1D0E2E.toInt(), 0xFF0E0718.toInt()))
            val webStroke = strokePaint(0xBBD8C8EE.toInt(), unit * 0.028f)
            val sparkle = fillPaint(0xFFC77DFF.toInt())
            forEachTile(w, h, unit * 1.05f) { r, c, cx, cy ->
                if ((r + c) % 2 == 0) {
                    drawSpiderweb(canvas, cx, cy, unit * 0.38f, webStroke)
                } else {
                    drawSparkle4(canvas, cx, cy, unit * 0.14f, sparkle)
                }
            }
        }

        PatternTheme.GHOSTLY_MIST -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF0B132B.toInt(), 0xFF1C2541.toInt(), 0xFF28385E.toInt(), 0xFF0F172A.toInt()))
            val ghostWhite = fillPaint(0xEEF4F8FF.toInt())
            val eyeDark = fillPaint(0xFF111936.toInt())
            val wisp = fillPaint(0x99A5F3FC.toInt())
            forEachTile(w, h, unit * 0.96f) { r, c, cx, cy ->
                if ((r + c) % 2 == 0) {
                    val tilt = if (r % 2 == 0) -12f else 12f
                    drawGhost(canvas, cx, cy, unit * 0.27f, tilt, ghostWhite, eyeDark)
                } else {
                    drawSparkle4(canvas, cx - unit * 0.08f, cy, unit * 0.14f, wisp)
                    canvas.drawCircle(cx + unit * 0.16f, cy - unit * 0.14f, unit * 0.045f, wisp)
                }
            }
        }

        PatternTheme.SPOOKY_NIGHT -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF120524.toInt(), 0xFF3C096C.toInt(), 0xFF6A1B9A.toInt(), 0xFFE65100.toInt()))
            val moonPaint = fillPaint(0xFFFFD54F.toInt())
            val batPaint = fillPaint(0xFF100018.toInt())
            val pumpkinPaint = fillPaint(0xFFFF7900.toInt())
            val starPaint = fillPaint(0xFFE0AAFF.toInt())
            forEachTile(w, h, unit * 0.95f) { r, c, cx, cy ->
                when ((r * 3 + c).mod(3)) {
                    0 -> drawCrescentMoon(canvas, cx, cy, unit * 0.23f, -25f, moonPaint)
                    1 -> drawBat(canvas, cx, cy, unit * 0.26f, batPaint)
                    else -> {
                        drawSparkle4(canvas, cx, cy, unit * 0.16f, pumpkinPaint)
                        canvas.drawCircle(cx + unit * 0.18f, cy - unit * 0.18f, unit * 0.045f, starPaint)
                    }
                }
            }
        }

        PatternTheme.PUMPKIN_STRIPE -> {
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

        // ─── Outdoors, Coastal & Vibes ────────────────────────────────────────
        PatternTheme.BEACH_VIBES -> {
            // Turquoise ocean to sunlit warm sand along diagonal
            drawLinearBg(
                canvas, w, h,
                intArrayOf(0xFF005F8F.toInt(), 0xFF0096C7.toInt(), 0xFF48CAE4.toInt(), 0xFFF4D58D.toInt())
            )
            val foamStroke = strokePaint(0xEEFFFFFF.toInt(), unit * 0.055f)
            val aquaStroke = strokePaint(0xBBB8F2FF.toInt(), unit * 0.040f)
            val coralStar = fillPaint(0xFFFF7B54.toInt())
            val sunGold = fillPaint(0xFFFFD166.toInt())

            val rowH = unit * 0.72f
            val rows = ceil(h / rowH).toInt() + 1
            for (r in 0 until rows) {
                val y = r * rowH + rowH * 0.5f
                val path = Path()
                val waveW = unit * 0.78f
                val amp = unit * 0.12f
                var x = if (r % 2 == 0) 0f else -waveW * 0.5f
                path.moveTo(x, y)
                while (x < w + waveW) {
                    path.quadTo(x + waveW * 0.25f, y - amp, x + waveW * 0.5f, y)
                    path.quadTo(x + waveW * 0.75f, y + amp, x + waveW, y)
                    x += waveW
                }
                canvas.drawPath(path, if (r % 2 == 0) foamStroke else aquaStroke)
            }
            forEachTile(w, h, unit * 1.35f) { r, c, cx, cy ->
                if ((r + c) % 2 == 0) {
                    drawStar5(canvas, cx, cy, unit * 0.21f, unit * 0.10f, -90f, coralStar)
                } else {
                    canvas.drawCircle(cx, cy, unit * 0.08f, sunGold)
                }
            }
        }

        PatternTheme.TROPICAL_PALM -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF052E2B.toInt(), 0xFF0A5C46.toInt(), 0xFF138A63.toInt(), 0xFF084C41.toInt()))
            val frondLight = fillPaint(0xFF74E39A.toInt())
            val frondTeal = fillPaint(0xFF48CAE4.toInt())
            val coralHibiscus = fillPaint(0xFFFF6B6B.toInt())
            val sunCenter = fillPaint(0xFFFFE66D.toInt())
            forEachTile(w, h, unit * 1.0f) { r, c, cx, cy ->
                when ((r + c * 2).mod(3)) {
                    0 -> drawPalmLeaf(canvas, cx, cy, unit * 0.30f, -30f, frondLight)
                    1 -> drawPalmLeaf(canvas, cx, cy, unit * 0.26f, 35f, frondTeal)
                    else -> drawFlower(canvas, cx, cy, unit * 0.21f, coralHibiscus, sunCenter)
                }
            }
        }

        PatternTheme.CAMPING_NIGHT -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF07131D.toInt(), 0xFF0E241B.toInt(), 0xFF193829.toInt(), 0xFF0A1922.toInt()))
            val pineDark = fillPaint(0xFF2D6A4F.toInt())
            val pineLight = fillPaint(0xFF52B788.toInt())
            val trunk = fillPaint(0xFF6C462F.toInt())
            val tentOchre = fillPaint(0xFFE9C46A.toInt())
            val tentDoor = fillPaint(0xFF102019.toInt())
            val ember = fillPaint(0xFFFF7B00.toInt())
            val starCream = fillPaint(0xFFFFF3B0.toInt())
            forEachTile(w, h, unit * 1.0f) { r, c, cx, cy ->
                when ((r * 3 + c).mod(4)) {
                    0 -> drawPineTree(canvas, cx, cy, unit * 0.30f, pineLight, trunk)
                    1 -> drawTent(canvas, cx, cy, unit * 0.27f, tentOchre, tentDoor, ember)
                    2 -> drawPineTree(canvas, cx, cy, unit * 0.25f, pineDark, trunk)
                    else -> {
                        drawSparkle4(canvas, cx - unit * 0.1f, cy - unit * 0.08f, unit * 0.13f, starCream)
                        canvas.drawCircle(cx + unit * 0.16f, cy + unit * 0.14f, unit * 0.045f, ember)
                    }
                }
            }
        }

        PatternTheme.CELESTIAL -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF06091E.toInt(), 0xFF111945.toInt(), 0xFF1E2761.toInt(), 0xFF080D2A.toInt()))
            val gold = fillPaint(0xFFFFD670.toInt())
            val ringStroke = strokePaint(0xFFE9C46A.toInt(), unit * 0.036f)
            val paleBlue = fillPaint(0xFFA9D6E5.toInt())
            forEachTile(w, h, unit * 0.98f) { r, c, cx, cy ->
                when ((r * 2 + c).mod(3)) {
                    0 -> drawPlanet(canvas, cx, cy, unit * 0.28f, paleBlue, ringStroke)
                    1 -> drawCrescentMoon(canvas, cx, cy, unit * 0.21f, -30f, gold)
                    else -> {
                        drawSparkle4(canvas, cx, cy, unit * 0.17f, gold)
                        canvas.drawCircle(cx + unit * 0.2f, cy - unit * 0.16f, unit * 0.04f, paleBlue)
                    }
                }
            }
        }

        PatternTheme.WILDFLOWER -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF1E2D1B.toInt(), 0xFF2F4831.toInt(), 0xFF3E5C3E.toInt(), 0xFF243823.toInt()))
            val creamPetal = fillPaint(0xFFFFFBF2.toInt())
            val blushPetal = fillPaint(0xFFFFD6E0.toInt())
            val goldCenter = fillPaint(0xFFF4B942.toInt())
            val sageLeaf = fillPaint(0xFF84A98C.toInt())
            forEachTile(w, h, unit * 0.95f) { r, c, cx, cy ->
                when ((r + c * 2).mod(3)) {
                    0 -> drawFlower(canvas, cx, cy, unit * 0.24f, creamPetal, goldCenter)
                    1 -> drawFlower(canvas, cx, cy, unit * 0.19f, blushPetal, goldCenter)
                    else -> {
                        canvas.save()
                        canvas.translate(cx, cy)
                        canvas.rotate(if (r % 2 == 0) 35f else -35f)
                        canvas.drawOval(RectF(-unit * 0.18f, -unit * 0.08f, unit * 0.18f, unit * 0.08f), sageLeaf)
                        canvas.restore()
                    }
                }
            }
        }

        // ─── Baby, Pets, Milestones & Aesthetic ──────────────────────────────
        PatternTheme.BABY_BLUE -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFFB8DCFA.toInt(), 0xFFD4ECFF.toInt(), 0xFFC2E2FC.toInt(), 0xFFE3F2FD.toInt()))
            val cloudWhite = fillPaint(0xFAFFFFFF.toInt())
            val moonYellow = fillPaint(0xFFFFE082.toInt())
            val starBlue = fillPaint(0xFF64B5F6.toInt())
            forEachTile(w, h, unit * 0.98f) { r, c, cx, cy ->
                when ((r + c).mod(3)) {
                    0 -> drawCloud(canvas, cx, cy, unit * 0.29f, cloudWhite)
                    1 -> drawCrescentMoon(canvas, cx, cy, unit * 0.20f, -25f, moonYellow)
                    else -> {
                        drawSparkle4(canvas, cx - unit * 0.06f, cy, unit * 0.16f, cloudWhite)
                        drawSparkle4(canvas, cx + unit * 0.16f, cy - unit * 0.14f, unit * 0.10f, starBlue)
                    }
                }
            }
        }

        PatternTheme.BABY_PINK -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFFFAD2E1.toInt(), 0xFFFDE2E4.toInt(), 0xFFF9C6D4.toInt(), 0xFFFFF0F3.toInt()))
            val cloudWhite = fillPaint(0xFAFFFFFF.toInt())
            val roseHeart = fillPaint(0xFFFF758F.toInt())
            val softGold = fillPaint(0xFFF4C095.toInt())
            forEachTile(w, h, unit * 0.98f) { r, c, cx, cy ->
                when ((r + c).mod(3)) {
                    0 -> drawCloud(canvas, cx, cy, unit * 0.29f, cloudWhite)
                    1 -> drawHeart(canvas, cx, cy, unit * 0.19f, if (r % 2 == 0) -10f else 10f, roseHeart)
                    else -> {
                        drawSparkle4(canvas, cx - unit * 0.06f, cy, unit * 0.16f, cloudWhite)
                        drawSparkle4(canvas, cx + unit * 0.16f, cy - unit * 0.14f, unit * 0.10f, softGold)
                    }
                }
            }
        }

        PatternTheme.BIRTHDAY -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFFFFFDF7.toInt(), 0xFFFFF6E8.toInt(), 0xFFFFF9F2.toInt()))
            val sprinklePalette = arrayOf(
                fillPaint(0xFFFF595E.toInt()),
                fillPaint(0xFFFFCA3A.toInt()),
                fillPaint(0xFF2EC4B6.toInt()),
                fillPaint(0xFF3A86FF.toInt()),
                fillPaint(0xFFFF70A6.toInt()),
                fillPaint(0xFF8AC926.toInt())
            )
            val rect = RectF()
            forEachTile(w, h, unit * 0.68f) { r, c, cx, cy ->
                val p = sprinklePalette[(r * 5 + c * 3).mod(sprinklePalette.size)]
                if ((r + c) % 4 == 0) {
                    drawSparkle4(canvas, cx, cy, unit * 0.12f, p)
                } else {
                    canvas.save()
                    canvas.translate(cx, cy)
                    canvas.rotate(((r * 47 + c * 73) % 180).toFloat())
                    rect.set(-unit * 0.15f, -unit * 0.048f, unit * 0.15f, unit * 0.048f)
                    canvas.drawRoundRect(rect, unit * 0.05f, unit * 0.05f, p)
                    canvas.restore()
                }
            }
        }

        PatternTheme.PET_PAWS -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF2D1E18.toInt(), 0xFF432D24.toInt(), 0xFF36241C.toInt()))
            val creamPaw = fillPaint(0xFFEADBC8.toInt())
            val caramelPaw = fillPaint(0xFFC8A987.toInt())
            forEachTile(w, h, unit * 0.96f) { r, c, cx, cy ->
                val tilt = if ((r + c) % 2 == 0) -18f else 18f
                val paint = if ((r + c) % 2 == 0) creamPaw else caramelPaw
                drawPawPrint(canvas, cx, cy, unit * 0.24f, tilt, paint)
            }
        }

        PatternTheme.CHECKERBOARD -> {
            val cell = unit * 0.56f
            val dark = fillPaint(0xFF1A191E.toInt())
            val cream = fillPaint(0xFFF3EFE6.toInt())
            val rows = ceil(h / cell).toInt() + 1
            val cols = ceil(w / cell).toInt() + 1
            for (r in 0 until rows) {
                val top = r * cell
                for (c in 0 until cols) {
                    val left = c * cell
                    canvas.drawRect(left, top, left + cell + 0.6f, top + cell + 0.6f, if ((r + c) % 2 == 0) dark else cream)
                }
            }
        }

        // ─── Seasonal & Holidays ─────────────────────────────────────────────
        PatternTheme.CANDY_CANE -> {
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

        PatternTheme.SNOWY_PINE -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF092618.toInt(), 0xFF164A30.toInt(), 0xFF0D3521.toInt()))
            val snowPaint = strokePaint(0xFFF2F8FF.toInt(), unit * 0.045f)
            val goldPaint = fillPaint(0xFFFFD54F.toInt())
            val berryPaint = fillPaint(0xFFE53935.toInt())
            val leafPaint = fillPaint(0xFF2E8B57.toInt())
            val softSnow = fillPaint(0xCCF2F8FF.toInt())
            forEachTile(w, h, unit) { r, c, cx, cy ->
                when ((r * 3 + c) and 3) {
                    0 -> drawSnowflake(canvas, cx, cy, unit * 0.27f, snowPaint)
                    1 -> drawSparkle4(canvas, cx, cy, unit * 0.18f, goldPaint)
                    2 -> drawHollyBerries(canvas, cx, cy, unit * 0.16f, berryPaint, leafPaint)
                    else -> {
                        canvas.drawCircle(cx - unit * 0.12f, cy - unit * 0.1f, unit * 0.055f, softSnow)
                        drawSparkle4(canvas, cx + unit * 0.14f, cy + unit * 0.12f, unit * 0.13f, goldPaint)
                    }
                }
            }
        }

        PatternTheme.WINTER_FROST -> {
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

        PatternTheme.HARVEST_PLAID -> {
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

        PatternTheme.NEW_YEARS_GLAM -> {
            drawLinearBg(canvas, w, h, intArrayOf(0xFF09090F.toInt(), 0xFF191726.toInt(), 0xFF0D0C14.toInt()))
            val gold = fillPaint(0xFFFFD700.toInt())
            val champagne = fillPaint(0xFFF7E7CE.toInt())
            val roseGold = fillPaint(0xFFE8A598.toInt())
            val rect = RectF()
            forEachTile(w, h, unit * 0.9f) { r, c, cx, cy ->
                when ((r * 5 + c * 3).mod(4)) {
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

        PatternTheme.SWEETHEART -> {
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

        PatternTheme.LUCKY_CLOVER -> {
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

        PatternTheme.EASTER_PASTEL -> {
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

        PatternTheme.STAR_SPANGLED -> {
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
    }
}

// ─── Vector motif primitives ─────────────────────────────────────────────────

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
    val totalWeight = bands.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(0.01f)
    while (x < diag) {
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

private fun drawSkull(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    rotDeg: Float,
    bonePaint: Paint,
    socketPaint: Paint,
    teethStroke: Paint
) {
    canvas.save()
    canvas.translate(cx, cy)
    canvas.rotate(rotDeg)
    // Cranium + jaw
    canvas.drawCircle(0f, -r * 0.16f, r * 0.72f, bonePaint)
    canvas.drawRoundRect(RectF(-r * 0.42f, r * 0.12f, r * 0.42f, r * 0.74f), r * 0.16f, r * 0.16f, bonePaint)
    // Eye sockets
    canvas.drawCircle(-r * 0.26f, -r * 0.10f, r * 0.18f, socketPaint)
    canvas.drawCircle(r * 0.26f, -r * 0.10f, r * 0.18f, socketPaint)
    // Nasal cavity
    val nose = Path().apply {
        moveTo(0f, r * 0.08f)
        lineTo(-r * 0.11f, r * 0.30f)
        lineTo(r * 0.11f, r * 0.30f)
        close()
    }
    canvas.drawPath(nose, socketPaint)
    // Teeth lines
    for (tx in floatArrayOf(-0.20f, 0f, 0.20f)) {
        canvas.drawLine(r * tx, r * 0.46f, r * tx, r * 0.72f, teethStroke)
    }
    canvas.restore()
}

private fun drawCrossbones(canvas: Canvas, cx: Float, cy: Float, r: Float, bonePaint: Paint) {
    val len = r * 0.80f
    val shaft = r * 0.13f
    val knob = r * 0.19f
    for (ang in floatArrayOf(-36f, 36f)) {
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(ang)
        canvas.drawRect(-len, -shaft, len, shaft, bonePaint)
        canvas.drawCircle(-len, -shaft * 1.3f, knob, bonePaint)
        canvas.drawCircle(-len, shaft * 1.3f, knob, bonePaint)
        canvas.drawCircle(len, -shaft * 1.3f, knob, bonePaint)
        canvas.drawCircle(len, shaft * 1.3f, knob, bonePaint)
        canvas.restore()
    }
}

private fun drawSpiderweb(canvas: Canvas, cx: Float, cy: Float, r: Float, webStroke: Paint) {
    val spokes = 8
    for (i in 0 until spokes) {
        val ang = (i * 45.0) * PI / 180.0
        canvas.drawLine(cx, cy, cx + cos(ang).toFloat() * r, cy + sin(ang).toFloat() * r, webStroke)
    }
    for (ring in floatArrayOf(0.48f, 0.86f)) {
        val rad = r * ring
        val web = Path()
        for (i in 0 until spokes) {
            val a1 = (i * 45.0) * PI / 180.0
            val a2 = ((i + 1) * 45.0) * PI / 180.0
            val mid = (a1 + a2) * 0.5
            val x1 = cx + cos(a1).toFloat() * rad
            val y1 = cy + sin(a1).toFloat() * rad
            val x2 = cx + cos(a2).toFloat() * rad
            val y2 = cy + sin(a2).toFloat() * rad
            val qx = cx + cos(mid).toFloat() * rad * 0.74f
            val qy = cy + sin(mid).toFloat() * rad * 0.74f
            if (i == 0) web.moveTo(x1, y1)
            web.quadTo(qx, qy, x2, y2)
        }
        web.close()
        canvas.drawPath(web, webStroke)
    }
}

private fun drawGhost(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    rotDeg: Float,
    ghostPaint: Paint,
    eyePaint: Paint
) {
    canvas.save()
    canvas.translate(cx, cy)
    canvas.rotate(rotDeg)
    val w = r * 0.62f
    val topY = -r * 0.80f
    val botY = r * 0.66f
    val p = Path().apply {
        arcTo(RectF(-w, topY, w, topY + 2f * w), 180f, 180f, true)
        lineTo(w, botY)
        // 3 wavy scallops along bottom hem
        val seg = (2f * w) / 3f
        quadTo(w - seg * 0.5f, botY - r * 0.24f, w - seg, botY)
        quadTo(0f, botY + r * 0.16f, -w + seg, botY)
        quadTo(-w + seg * 0.5f, botY - r * 0.24f, -w, botY)
        close()
    }
    canvas.drawPath(p, ghostPaint)
    canvas.drawOval(RectF(-r * 0.30f, -r * 0.32f, -r * 0.11f, -r * 0.06f), eyePaint)
    canvas.drawOval(RectF(r * 0.11f, -r * 0.32f, r * 0.30f, -r * 0.06f), eyePaint)
    canvas.restore()
}

private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
    canvas.drawRoundRect(RectF(cx - r * 0.84f, cy - r * 0.06f, cx + r * 0.84f, cy + r * 0.42f), r * 0.24f, r * 0.24f, paint)
    canvas.drawCircle(cx - r * 0.38f, cy - r * 0.06f, r * 0.34f, paint)
    canvas.drawCircle(cx + r * 0.04f, cy - r * 0.22f, r * 0.44f, paint)
    canvas.drawCircle(cx + r * 0.46f, cy - r * 0.02f, r * 0.30f, paint)
}

private fun drawPalmLeaf(canvas: Canvas, cx: Float, cy: Float, r: Float, rotDeg: Float, paint: Paint) {
    canvas.save()
    canvas.translate(cx, cy)
    canvas.rotate(rotDeg)
    val stem = strokePaint(paint.color, r * 0.12f)
    canvas.drawLine(0f, r * 0.85f, 0f, -r * 0.85f, stem)
    for (i in -2..2) {
        val y = i * r * 0.30f
        val span = r * (0.78f - kotlin.math.abs(i) * 0.12f)
        for (dir in intArrayOf(-1, 1)) {
            val leaf = Path().apply {
                moveTo(0f, y)
                quadTo(dir * span * 0.6f, y - r * 0.26f, dir * span, y - r * 0.12f)
                quadTo(dir * span * 0.5f, y + r * 0.06f, 0f, y + r * 0.08f)
                close()
            }
            canvas.drawPath(leaf, paint)
        }
    }
    canvas.restore()
}

private fun drawPineTree(canvas: Canvas, cx: Float, cy: Float, r: Float, foliagePaint: Paint, trunkPaint: Paint) {
    canvas.drawRect(cx - r * 0.14f, cy + r * 0.46f, cx + r * 0.14f, cy + r * 0.84f, trunkPaint)
    val tiers = arrayOf(
        Triple(-0.88f, -0.14f, 0.48f),
        Triple(-0.44f, 0.24f, 0.68f),
        Triple(-0.04f, 0.56f, 0.86f)
    )
    for ((topF, botF, halfWF) in tiers) {
        val tri = Path().apply {
            moveTo(cx, cy + r * topF)
            lineTo(cx - r * halfWF, cy + r * botF)
            lineTo(cx + r * halfWF, cy + r * botF)
            close()
        }
        canvas.drawPath(tri, foliagePaint)
    }
}

private fun drawTent(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    tentPaint: Paint,
    doorPaint: Paint,
    glowPaint: Paint
) {
    val outer = Path().apply {
        moveTo(cx, cy - r * 0.68f)
        lineTo(cx - r * 0.88f, cy + r * 0.56f)
        lineTo(cx + r * 0.88f, cy + r * 0.56f)
        close()
    }
    canvas.drawPath(outer, tentPaint)
    val door = Path().apply {
        moveTo(cx, cy - r * 0.04f)
        lineTo(cx - r * 0.30f, cy + r * 0.56f)
        lineTo(cx + r * 0.30f, cy + r * 0.56f)
        close()
    }
    canvas.drawPath(door, doorPaint)
    canvas.drawCircle(cx, cy + r * 0.40f, r * 0.11f, glowPaint)
}

private fun drawPlanet(canvas: Canvas, cx: Float, cy: Float, r: Float, planetPaint: Paint, ringStroke: Paint) {
    canvas.drawCircle(cx, cy, r * 0.50f, planetPaint)
    canvas.save()
    canvas.translate(cx, cy)
    canvas.rotate(-24f)
    canvas.drawOval(RectF(-r * 1.02f, -r * 0.28f, r * 1.02f, r * 0.28f), ringStroke)
    canvas.restore()
}

private fun drawFlower(canvas: Canvas, cx: Float, cy: Float, r: Float, petalPaint: Paint, centerPaint: Paint) {
    for (i in 0 until 6) {
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(i * 60f)
        canvas.drawOval(RectF(-r * 0.24f, -r * 0.92f, r * 0.24f, -r * 0.16f), petalPaint)
        canvas.restore()
    }
    canvas.drawCircle(cx, cy, r * 0.28f, centerPaint)
}

private fun drawPawPrint(canvas: Canvas, cx: Float, cy: Float, r: Float, rotDeg: Float, paint: Paint) {
    canvas.save()
    canvas.translate(cx, cy)
    canvas.rotate(rotDeg)
    // Main pad
    canvas.drawOval(RectF(-r * 0.48f, -r * 0.06f, r * 0.48f, r * 0.64f), paint)
    // 4 toe beans
    val toes = arrayOf(
        Triple(-0.56f, -0.18f, -22f),
        Triple(-0.20f, -0.46f, -8f),
        Triple(0.20f, -0.46f, 8f),
        Triple(0.56f, -0.18f, 22f)
    )
    for ((tx, ty, tang) in toes) {
        canvas.save()
        canvas.translate(r * tx, r * ty)
        canvas.rotate(tang)
        canvas.drawOval(RectF(-r * 0.16f, -r * 0.24f, r * 0.16f, r * 0.24f), paint)
        canvas.restore()
    }
    canvas.restore()
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
        lineTo(cx - r * 0.16f, cy - r * 0.42f)
        lineTo(cx - r * 0.24f, cy - r * 0.18f)
        quadTo(cx - r * 0.72f, cy - r * 0.55f, cx - r, cy + r * 0.05f)
        quadTo(cx - r * 0.65f, cy - r * 0.05f, cx - r * 0.48f, cy + r * 0.26f)
        quadTo(cx - r * 0.22f, cy + r * 0.05f, cx, cy + r * 0.34f)
        quadTo(cx + r * 0.22f, cy + r * 0.05f, cx + r * 0.48f, cy + r * 0.26f)
        quadTo(cx + r * 0.65f, cy - r * 0.05f, cx + r, cy + r * 0.05f)
        quadTo(cx + r * 0.72f, cy - r * 0.55f, cx + r * 0.24f, cy - r * 0.18f)
        lineTo(cx + r * 0.16f, cy - r * 0.42f)
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

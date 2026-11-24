package com.tailgunnerx.piccollage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

const val GITHUB_URL = "https://github.com/tailgunnerx/PicCollage-Android"

data class ChangelogEntry(val version: String, val date: String, val notes: String)

val CHANGELOG = listOf(
    ChangelogEntry(
        version = "1.1",
        date = "2026-11-23",
        notes = "• Added Settings popup with canvas aspect ratios: Portrait, Landscape, Story, Wide & Custom W:H (#1)\n" +
            "• Added Grid 3×3 preset and Custom grid builder with draggable row & column lines (#2)\n" +
            "• Added Photo Corners slider (0 = square corners) rendered identically in preview & saved image (#1)\n" +
            "• Added Export Resolution selector (Standard 1080p, High 1440p, Ultra 2160p)\n" +
            "• Added Show Emojis toggle for a clean text-only interface (#1)\n" +
            "• Expanded Gradient Borders library to 50 presets (added luxury metallics, pastels, botanicals & conical sweeps)\n" +
            "• Added Themed Borders drawer with 24 procedural vector designs (Spooky skulls & cobwebs, Beach Vibes, Camping, Baby Blue & Pink, Pets, Celestial & seasonal classics)\n" +
            "• Unified fractional layout engine & smart photo downsampling for 9+ photo grids\n" +
            "• Added in-app Changelog & GitHub Repository link"
    ),
    ChangelogEntry(
        version = "1.0",
        date = "2026-05-05",
        notes = "• Initial release"
    )
)

// ─── Settings model ──────────────────────────────────────────────────────────

enum class AspectRatioOption(val label: String, val ratioLabel: String, val w: Int, val h: Int) {
    SQUARE("Square", "1:1", 1, 1),
    PORTRAIT_4_5("Portrait", "4:5", 4, 5),
    PORTRAIT_3_4("Portrait", "3:4", 3, 4),
    PORTRAIT_2_3("Portrait", "2:3", 2, 3),
    STORY_9_16("Story", "9:16", 9, 16),
    LANDSCAPE_5_4("Landscape", "5:4", 5, 4),
    LANDSCAPE_4_3("Landscape", "4:3", 4, 3),
    LANDSCAPE_3_2("Landscape", "3:2", 3, 2),
    WIDE_16_9("Wide", "16:9", 16, 9),
    CUSTOM("Custom", "W:H", 0, 0)
}

enum class ExportQuality(val label: String, val shortSide: Int) {
    STANDARD("Standard", 1080),
    HIGH("High", 1440),
    ULTRA("Ultra", 2160)
}

data class AppSettings(
    val aspect: AspectRatioOption = AspectRatioOption.SQUARE,
    val customW: Int = 3,
    val customH: Int = 2,
    /** Photo corner radius in preview dp. 0 = square corners. */
    val cornerRadius: Float = 0f,
    val quality: ExportQuality = ExportQuality.STANDARD,
    val showEmojis: Boolean = true
) {
    /** width / height, clamped to a sane range. */
    val ratio: Float
        get() {
            val r = if (aspect == AspectRatioOption.CUSTOM) customW.toFloat() / customH
            else aspect.w.toFloat() / aspect.h
            return r.coerceIn(0.25f, 4f)
        }

    val ratioLabel: String
        get() = if (aspect == AspectRatioOption.CUSTOM) "$customW:$customH" else aspect.ratioLabel

    fun outputSize(q: ExportQuality = quality): IntSize {
        val short = q.shortSide
        return if (ratio >= 1f) IntSize((short * ratio).roundToInt(), short)
        else IntSize(short, (short / ratio).roundToInt())
    }

    /** Prefixes [text] with [emoji] only if the user wants emojis in the UI. */
    fun e(emoji: String, text: String) = if (showEmojis) "$emoji  $text" else text

    companion object {
        const val CUSTOM_MAX = 20
        const val CORNER_MAX = 40f
    }
}

object SettingsStore {
    private const val PREFS = "piccollage_settings"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): AppSettings {
        val p = prefs(context)
        val d = AppSettings()
        return AppSettings(
            aspect = enumOrDefault(p.getString("aspect", null), d.aspect),
            customW = p.getInt("customW", d.customW).coerceIn(1, AppSettings.CUSTOM_MAX),
            customH = p.getInt("customH", d.customH).coerceIn(1, AppSettings.CUSTOM_MAX),
            cornerRadius = p.getFloat("cornerRadius", d.cornerRadius).coerceIn(0f, AppSettings.CORNER_MAX),
            quality = enumOrDefault(p.getString("quality", null), d.quality),
            showEmojis = p.getBoolean("showEmojis", d.showEmojis)
        )
    }

    fun save(context: Context, s: AppSettings) {
        prefs(context).edit()
            .putString("aspect", s.aspect.name)
            .putInt("customW", s.customW)
            .putInt("customH", s.customH)
            .putFloat("cornerRadius", s.cornerRadius)
            .putString("quality", s.quality.name)
            .putBoolean("showEmojis", s.showEmojis)
            .apply()
    }

    fun loadCustomGrid(context: Context): CustomGridState {
        val p = prefs(context)
        val grid = CustomGridState(p.getInt("gridCols", 3), p.getInt("gridRows", 3))
        grid.restoreStops(parseStops(p.getString("gridColStops", null)), parseStops(p.getString("gridRowStops", null)))
        return grid
    }

    fun saveCustomGrid(context: Context, grid: CustomGridState) {
        prefs(context).edit()
            .putInt("gridCols", grid.cols)
            .putInt("gridRows", grid.rows)
            .putString("gridColStops", grid.colStops.joinToString(","))
            .putString("gridRowStops", grid.rowStops.joinToString(","))
            .apply()
    }

    private fun parseStops(s: String?): List<Float> =
        s?.split(',')?.mapNotNull { it.trim().toFloatOrNull() } ?: emptyList()

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}

// ─── Settings window ─────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showChangelog by remember { mutableStateOf(false) }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: CHANGELOG.first().version
    }

    if (showChangelog) {
        ChangelogDialog(onBack = { showChangelog = false })
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Dark800)
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(22.dp))
        ) {
            // Title bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Settings", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Dark700)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) { Text("✕", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // ── Canvas shape ─────────────────────────────────────────────
                SectionTitle("Canvas shape", "Aspect ratio of the preview and the saved image")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AspectRatioOption.entries.forEach { opt ->
                        val r = if (opt == AspectRatioOption.CUSTOM) settings.copy(aspect = opt).ratio
                        else opt.w.toFloat() / opt.h
                        AspectChip(
                            label = opt.label,
                            ratioLabel = if (opt == AspectRatioOption.CUSTOM) "${settings.customW}:${settings.customH}" else opt.ratioLabel,
                            ratio = r,
                            selected = settings.aspect == opt,
                            onClick = { onChange(settings.copy(aspect = opt)) }
                        )
                    }
                }
                if (settings.aspect == AspectRatioOption.CUSTOM) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Dark700)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Stepper(
                            label = "Width",
                            value = settings.customW,
                            onMinus = { onChange(settings.copy(customW = settings.customW - 1)) },
                            onPlus = { onChange(settings.copy(customW = settings.customW + 1)) },
                            minusEnabled = settings.customW > 1,
                            plusEnabled = settings.customW < AppSettings.CUSTOM_MAX
                        )
                        Stepper(
                            label = "Height",
                            value = settings.customH,
                            onMinus = { onChange(settings.copy(customH = settings.customH - 1)) },
                            onPlus = { onChange(settings.copy(customH = settings.customH + 1)) },
                            minusEnabled = settings.customH > 1,
                            plusEnabled = settings.customH < AppSettings.CUSTOM_MAX
                        )
                    }
                }

                // ── Corners ──────────────────────────────────────────────────
                SectionTitle("Photo corners", "Set to 0 for square corners")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(36.dp)) {
                        val rr = (settings.cornerRadius / AppSettings.CORNER_MAX) * size.minDimension / 2f
                        drawRoundRect(color = Pink500, cornerRadius = CornerRadius(rr, rr))
                    }
                    Spacer(Modifier.width(12.dp))
                    Slider(
                        value = settings.cornerRadius,
                        onValueChange = { onChange(settings.copy(cornerRadius = it)) },
                        valueRange = 0f..AppSettings.CORNER_MAX,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Pink500, activeTrackColor = Pink500, inactiveTrackColor = Surface)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (settings.cornerRadius < 0.5f) "Square" else "${settings.cornerRadius.roundToInt()}",
                        color = White, fontSize = 13.sp, modifier = Modifier.widthIn(min = 48.dp), textAlign = TextAlign.End
                    )
                }

                // ── Export ───────────────────────────────────────────────────
                SectionTitle("Export resolution", "Size of the saved JPEG")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ExportQuality.entries.forEach { q ->
                        val sz = settings.outputSize(q)
                        OptionCard(
                            title = q.label,
                            subtitle = "${sz.width}×${sz.height}",
                            selected = settings.quality == q,
                            onClick = { onChange(settings.copy(quality = q)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Interface ────────────────────────────────────────────────
                SectionTitle("Interface", null)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Dark700)
                        .clickable { onChange(settings.copy(showEmojis = !settings.showEmojis)) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Show emojis", color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Decorative emojis on buttons and messages", color = Subtle, fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.showEmojis,
                        onCheckedChange = { onChange(settings.copy(showEmojis = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = White,
                            checkedTrackColor = Pink500,
                            uncheckedThumbColor = Subtle,
                            uncheckedTrackColor = Surface,
                            uncheckedBorderColor = Subtle
                        )
                    )
                }

                // ── About ────────────────────────────────────────────────────
                SectionTitle("About", null)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Dark700)
                ) {
                    AboutRow(
                        title = "Changelog",
                        subtitle = "View release notes & version history (v$versionName)",
                        onClick = { showChangelog = true }
                    ) {
                        Text("View", color = SettingsAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                    AboutRow(
                        title = "GitHub Repository",
                        subtitle = GITHUB_URL.removePrefix("https://"),
                        subtitleColor = SettingsAccent,
                        onClick = {
                            onDismiss()
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
                        }
                    ) {
                        Icon(
                            imageVector = OpenInNewIcon,
                            contentDescription = "Open GitHub Repository",
                            tint = SettingsAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onChange(AppSettings()) }) {
                    Text("Reset to defaults", color = Subtle, fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppGradient)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 28.dp, vertical = 12.dp)
                ) { Text("Done", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String?) {
    Spacer(Modifier.height(18.dp))
    Text(title, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    if (subtitle != null) Text(subtitle, color = Subtle, fontSize = 11.sp)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun AspectChip(label: String, ratioLabel: String, ratio: Float, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(66.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Pink500 else Dark700)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(Modifier.size(30.dp)) {
            val maxSide = size.minDimension
            val w = if (ratio >= 1f) maxSide else maxSide * ratio
            val h = if (ratio >= 1f) maxSide / ratio else maxSide
            drawRoundRect(
                color = if (selected) White else Color(0xFF5A5A7A),
                topLeft = Offset((size.width - w) / 2f, (size.height - h) / 2f),
                size = Size(w, h),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(ratioLabel, color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(label, color = if (selected) White else Subtle, fontSize = 9.sp)
    }
}

@Composable
private fun OptionCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Pink500 else Dark700)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = if (selected) White else Subtle, fontSize = 10.sp)
    }
}

@Composable
private fun AboutRow(
    title: String,
    subtitle: String,
    subtitleColor: Color = Subtle,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = subtitleColor, fontSize = 11.sp)
        }
        trailing()
    }
}

@Composable
fun ChangelogDialog(onBack: () -> Unit) {
    AlertDialog(
        onDismissRequest = onBack,
        containerColor = Dark800,
        titleContentColor = White,
        textContentColor = Subtle,
        title = {
            Text("Changelog", style = MaterialTheme.typography.titleLarge, color = White)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CHANGELOG.forEachIndexed { index, entry ->
                    if (index > 0) {
                        HorizontalDivider(color = Color(0x22FFFFFF))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "v${entry.version}",
                                style = MaterialTheme.typography.titleMedium,
                                color = White
                            )
                            Text(
                                text = entry.date,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (index == 0) SettingsAccent else Subtle
                            )
                        }
                        Text(
                            text = entry.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = Subtle
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onBack) {
                Text("Back", color = SettingsAccent)
            }
        }
    )
}

// ─── Shared small controls ───────────────────────────────────────────────────

@Composable
fun Stepper(
    label: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean,
    plusEnabled: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Subtle, fontSize = 12.sp)
        Spacer(Modifier.width(8.dp))
        StepButton("−", minusEnabled, onMinus)
        Text(
            "$value",
            color = White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 28.dp)
        )
        StepButton("+", plusEnabled, onPlus)
    }
}

@Composable
private fun StepButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (enabled) Surface else Dark900)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = if (enabled) White else Subtle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

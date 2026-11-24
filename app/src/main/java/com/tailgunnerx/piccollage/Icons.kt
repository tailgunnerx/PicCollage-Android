package com.tailgunnerx.piccollage

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Shared with FrameExtractor so both apps use the identical gear + accent.
val SettingsAccent = Color(0xFFE040FB)

val SettingsIcon = ImageVector.Builder("Settings", 24.dp, 24.dp, 24f, 24f).apply {
    path(
        fill = SolidColor(Color.White),
        pathFillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
    ) {
        moveTo(19.14f, 12.94f)
        curveToRelative(0.04f, -0.3f, 0.06f, -0.61f, 0.06f, -0.94f)
        curveToRelative(0f, -0.32f, -0.02f, -0.64f, -0.07f, -0.94f)
        lineToRelative(2.03f, -1.58f)
        curveToRelative(0.18f, -0.14f, 0.23f, -0.41f, 0.12f, -0.61f)
        lineToRelative(-1.92f, -3.32f)
        curveToRelative(-0.12f, -0.22f, -0.37f, -0.29f, -0.59f, -0.22f)
        lineToRelative(-2.39f, 0.96f)
        curveToRelative(-0.5f, -0.38f, -1.03f, -0.7f, -1.62f, -0.94f)
        lineToRelative(-0.36f, -2.54f)
        curveToRelative(-0.04f, -0.24f, -0.24f, -0.41f, -0.48f, -0.41f)
        horizontalLineToRelative(-3.84f)
        curveToRelative(-0.24f, 0f, -0.43f, 0.17f, -0.47f, 0.41f)
        lineToRelative(-0.36f, 2.54f)
        curveToRelative(-0.59f, 0.24f, -1.13f, 0.57f, -1.62f, 0.94f)
        lineToRelative(-2.39f, -0.96f)
        curveToRelative(-0.22f, -0.08f, -0.47f, 0f, -0.59f, 0.22f)
        lineToRelative(-1.92f, 3.32f)
        curveToRelative(-0.12f, 0.21f, -0.07f, 0.47f, 0.12f, 0.61f)
        lineToRelative(2.03f, 1.58f)
        curveToRelative(-0.05f, 0.3f, -0.09f, 0.63f, -0.09f, 0.94f)
        curveToRelative(0f, 0.31f, 0.02f, 0.64f, 0.07f, 0.94f)
        lineToRelative(-2.03f, 1.58f)
        curveToRelative(-0.18f, 0.14f, -0.23f, 0.41f, -0.12f, 0.61f)
        lineToRelative(1.92f, 3.32f)
        curveToRelative(0.12f, 0.22f, 0.37f, 0.29f, 0.59f, 0.22f)
        lineToRelative(2.39f, -0.96f)
        curveToRelative(0.5f, 0.38f, 1.03f, 0.7f, 1.62f, 0.94f)
        lineToRelative(0.36f, 2.54f)
        curveToRelative(0.05f, 0.24f, 0.24f, 0.41f, 0.48f, 0.41f)
        horizontalLineToRelative(3.84f)
        curveToRelative(0.24f, 0f, 0.44f, -0.17f, 0.47f, -0.41f)
        lineToRelative(0.36f, -2.54f)
        curveToRelative(0.59f, -0.24f, 1.13f, -0.56f, 1.62f, -0.94f)
        lineToRelative(2.39f, 0.96f)
        curveToRelative(0.22f, 0.08f, 0.47f, 0f, 0.59f, -0.22f)
        lineToRelative(1.92f, -3.32f)
        curveToRelative(0.12f, -0.22f, 0.07f, -0.47f, -0.12f, -0.61f)
        lineToRelative(-2.01f, -1.58f)
        close()
        moveTo(12f, 15.6f)
        curveToRelative(-1.98f, 0f, -3.6f, -1.62f, -3.6f, -3.6f)
        reflectiveCurveToRelative(1.62f, -3.6f, 3.6f, -3.6f)
        reflectiveCurveToRelative(3.6f, 1.62f, 3.6f, 3.6f)
        reflectiveCurveToRelative(-1.62f, 3.6f, -3.6f, 3.6f)
        close()
    }
}.build()

val OpenInNewIcon = ImageVector.Builder("OpenInNew", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(19f, 19f); horizontalLineTo(5f); verticalLineTo(5f); horizontalLineToRelative(7f); verticalLineTo(3f); horizontalLineTo(5f)
        curveToRelative(-1.11f, 0f, -2f, 0.9f, -2f, 2f); verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f); horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f); verticalLineToRelative(-7f); horizontalLineToRelative(-2f); verticalLineToRelative(7f); close()
        moveTo(14f, 3f); verticalLineToRelative(2f); horizontalLineToRelative(3.59f); lineToRelative(-9.83f, 9.83f); lineToRelative(1.41f, 1.41f); lineTo(19f, 6.41f); verticalLineTo(10f); horizontalLineToRelative(2f); verticalLineTo(3f); horizontalLineToRelative(-7f); close()
    }
}.build()

package com.omnidot.core

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.view.WindowManager
import com.omnidot.data.OmniDotPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class AdaptiveMatrixEngine {
    data class MatrixState(
        val total: Int,
        val completed: Int,
        val columns: Int,
        val rows: Int,
        val label: String,
        val currentIndex: Int?,
    )

    fun stateFor(settings: OmniDotPreferences, today: LocalDate = LocalDate.now()): MatrixState {
        return when (settings.preset) {
            MatrixPreset.YEAR_PROGRESS -> {
                val total = today.lengthOfYear()
                val daysLeft = total - today.dayOfYear
                MatrixState(
                    total,
                    today.dayOfYear,
                    settings.preset.columns,
                    ceil(total / settings.preset.columns.toFloat()).toInt(),
                    if (daysLeft == 1) "1 DAY LEFT" else "$daysLeft DAYS LEFT",
                    today.dayOfYear - 1,
                )
            }
            MatrixPreset.LIFE_IN_WEEKS -> {
                val elapsed = max(0, ChronoUnit.WEEKS.between(settings.birthDate, today).toInt())
                MatrixState(
                    4_160,
                    min(4_160, elapsed),
                    52,
                    80,
                    "${min(4_160, elapsed)} / 4160 WEEKS",
                    elapsed.takeIf { it in 0 until 4_160 && !today.isBefore(settings.birthDate) },
                )
            }
            MatrixPreset.SPRINT_100 -> {
                val elapsed = ChronoUnit.DAYS.between(settings.sprintStart, today).toInt() + 1
                MatrixState(
                    100,
                    elapsed.coerceIn(0, 100),
                    10,
                    10,
                    "${elapsed.coerceIn(0, 100)} / 100 DAYS",
                    (elapsed - 1).takeIf { it in 0 until 100 },
                )
            }
            MatrixPreset.CONTRIBUTION_HEATMAP -> {
                val yearStart = today.minusDays(363)
                val completed = ChronoUnit.DAYS.between(yearStart, today).toInt() + 1
                MatrixState(
                    364,
                    completed.coerceIn(0, 364),
                    52,
                    7,
                    "52 WEEKS · LOCAL ACTIVITY",
                    completed.coerceIn(1, 364) - 1,
                )
            }
        }
    }

    fun render(
        size: Size,
        settings: OmniDotPreferences,
        today: LocalDate = LocalDate.now(),
    ): Bitmap {
        require(size.width > 0 && size.height > 0) { "Bitmap dimensions must be positive" }
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BACKGROUND)

        val state = stateFor(settings, today)
        val margin = size.width * 0.075f
        val isLockscreenSafeYear = settings.preset == MatrixPreset.YEAR_PROGRESS
        // Android does not expose OEM lockscreen overlay bounds. Use an aspect-ratio
        // responsive safe region: tall devices reserve more space for large clocks and
        // bottom shortcuts, while shorter devices reclaim enough room for the matrix.
        val aspectRatio = size.height.toFloat() / size.width
        val topFraction = when {
            aspectRatio >= 2.15f -> .42f
            aspectRatio >= 1.95f -> .40f
            else -> .38f
        }
        val bottomFraction = when {
            aspectRatio >= 2.15f -> .18f
            else -> .16f
        }
        val adjustedTop = (topFraction + settings.verticalOffset).coerceIn(.18f, .62f)
        val top = size.height * adjustedTop
        val bottom = size.height * (1f - bottomFraction + settings.verticalOffset)
        val availableWidth = size.width - margin * 2
        val availableHeight = bottom - top
        val fittedCell = min(
            availableWidth / (state.columns + (state.columns - 1) * GAP_RATIO),
            availableHeight / (state.rows + (state.rows - 1) * GAP_RATIO),
        )
        val cell = fittedCell * settings.matrixScale
        val gap = cell * GAP_RATIO
        val gridWidth = state.columns * cell + (state.columns - 1) * gap
        val gridHeight = state.rows * cell + (state.rows - 1) * gap
        val left = (size.width - gridWidth) / 2f
        val gridTop = top + (availableHeight - gridHeight) / 2f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            textSize = max(20f, size.width * 0.038f)
            letterSpacing = 0.08f
        }
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MUTED_TEXT
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            textSize = max(14f, size.width * 0.026f)
            letterSpacing = 0.12f
        }
        if (!isLockscreenSafeYear) {
            canvas.drawText(settings.preset.title.uppercase(), left, max(titlePaint.textSize, gridTop - size.height * .045f), titlePaint)
            canvas.drawText(state.label, left, max(titlePaint.textSize + detailPaint.textSize, gridTop - size.height * .018f), detailPaint)
        }

        val inactive = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INACTIVE }
        val active = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = settings.accentColor }
        val current = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CURRENT_POSITION }
        val radius = max(1f, cell * .24f)
        repeat(state.total) { index ->
            val column: Int
            val row: Int
            if (settings.preset == MatrixPreset.CONTRIBUTION_HEATMAP) {
                column = index / 7
                row = index % 7
            } else {
                column = index % state.columns
                row = index / state.columns
            }
            val x = left + column * (cell + gap)
            val y = gridTop + row * (cell + gap)
            val isCurrent = index == state.currentIndex
            val paint = when {
                isCurrent -> current
                index < state.completed -> active
                else -> inactive
            }
            if (!isCurrent && settings.preset == MatrixPreset.CONTRIBUTION_HEATMAP && index < state.completed) {
                paint.alpha = 100 + deterministicIntensity(index, today) * 38
            } else {
                paint.alpha = 255
            }
            canvas.drawRoundRect(RectF(x, y, x + cell, y + cell), radius, radius, paint)
        }
        if (isLockscreenSafeYear) {
            val remainingPaint = Paint(titlePaint).apply {
                textSize = max(28f, size.width * .065f)
                letterSpacing = .04f
            }
            canvas.drawText(state.label, left, size.height * (adjustedTop - .055f), remainingPaint)
            canvas.drawText("YEAR IN DOTS", left, size.height * (adjustedTop - .03f), detailPaint)
        }
        return bitmap
    }

    suspend fun applyToLockscreen(context: Context, settings: OmniDotPreferences) = withContext(Dispatchers.IO) {
        val wallpaperManager = WallpaperManager.getInstance(context)
        check(wallpaperManager.isWallpaperSupported) { "Wallpapers are disabled on this device" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            check(wallpaperManager.isSetWallpaperAllowed) { "Wallpaper changes are blocked by device policy" }
        }
        // desiredMinimumWidth commonly describes a horizontally scrolling *home-screen*
        // wallpaper (often 2x the panel width). FLAG_LOCK needs the physical panel bounds;
        // using the former causes OEM lock screens to crop the matrix in half.
        val displaySize = physicalDisplaySize(context)
        val bitmap = render(displaySize, settings)
        try {
            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
        } finally {
            bitmap.recycle()
        }
    }

    private fun deterministicIntensity(index: Int, date: LocalDate): Int =
        ((index * 31L + date.year * 17L) xor (index * 7L)).mod(5).toInt()

    @Suppress("DEPRECATION")
    private fun physicalDisplaySize(context: Context): Size {
        val windowManager = context.getSystemService(WindowManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            Size(bounds.width(), bounds.height())
        } else {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(metrics)
            Size(metrics.widthPixels, metrics.heightPixels)
        }
    }

    companion object {
        private const val GAP_RATIO = .42f
        private const val BACKGROUND = -16_119_286 // #FF0A0A0A
        private const val INACTIVE = -14_277_082 // #FF262626
        private const val MUTED_TEXT = -6_579_301 // #FF9B9B9B
        private const val CURRENT_POSITION = 0xFF40E0D0.toInt()
    }
}

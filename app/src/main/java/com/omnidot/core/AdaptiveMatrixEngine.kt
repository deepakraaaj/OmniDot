package com.omnidot.core

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Size
import android.view.Display
import com.omnidot.data.OmniDotPreferences
import com.omnidot.goals.DayState
import com.omnidot.goals.Goal
import com.omnidot.goals.GoalDays
import com.omnidot.goals.GoalTier
import com.omnidot.goals.GoalsMatrix
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
        /** Compact "80d left" style label shown below the dots. */
        val shortLabel: String = "",
        val percent: Int = 0,
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
                    "${daysLeft}d left",
                    today.dayOfYear * 100 / total,
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
                    "${4_160 - min(4_160, elapsed)}w left",
                    min(4_160, elapsed) * 100 / 4_160,
                )
            }
            MatrixPreset.GOALS -> throw IllegalArgumentException("Goals use GoalsMatrix, not stateFor")
        }
    }

    data class GridGeometry(val left: Float, val top: Float, val cell: Float, val gap: Float, val adjustedTop: Float)

    fun render(
        size: Size,
        settings: OmniDotPreferences,
        goals: List<Goal> = emptyList(),
        today: LocalDate = LocalDate.now(),
    ): Bitmap {
        require(size.width > 0 && size.height > 0) { "Bitmap dimensions must be positive" }
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BACKGROUND)
        if (settings.preset == MatrixPreset.GOALS) drawGoals(canvas, size, settings, goals, today)
        else drawTimeMatrix(canvas, size, settings, today)
        return bitmap
    }

    // Android does not expose OEM lockscreen overlay bounds. Use an aspect-ratio
    // responsive safe region: tall devices reserve more space for large clocks and
    // bottom shortcuts, while shorter devices reclaim enough room for the matrix.
    internal fun gridGeometry(
        width: Int,
        height: Int,
        settings: OmniDotPreferences,
        columns: Int,
        rows: Int,
        gapRatio: Float = settings.dotSpacing,
    ): GridGeometry {
        val margin = width * 0.075f
        val aspectRatio = height.toFloat() / width
        val topFraction = when {
            aspectRatio >= 2.15f -> .42f
            aspectRatio >= 1.95f -> .40f
            else -> .38f
        }
        val bottomFraction = if (aspectRatio >= 2.15f) .18f else .16f
        val adjustedTop = (topFraction + settings.verticalOffset).coerceIn(.18f, .62f)
        val top = height * adjustedTop
        val bottom = height * (1f - bottomFraction + settings.verticalOffset)
        val availableWidth = width - margin * 2
        val availableHeight = bottom - top
        val fitted = min(
            availableWidth / (columns + (columns - 1) * gapRatio),
            availableHeight / (rows + (rows - 1) * gapRatio),
        )
        val cell = fitted * settings.matrixScale
        val gap = cell * gapRatio
        val gridWidth = columns * cell + (columns - 1) * gap
        val gridHeight = rows * cell + (rows - 1) * gap
        return GridGeometry((width - gridWidth) / 2f, top + (availableHeight - gridHeight) / 2f, cell, gap, adjustedTop)
    }

    private fun titlePaint(size: Size) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        textSize = max(20f, size.width * 0.038f)
        letterSpacing = 0.08f
    }

    private fun detailPaint(size: Size) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MUTED_TEXT
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        textSize = max(14f, size.width * 0.026f)
        letterSpacing = 0.12f
    }

    private fun drawTimeMatrix(canvas: Canvas, size: Size, settings: OmniDotPreferences, today: LocalDate) {
        val state = stateFor(settings, today)
        val labelBelow = settings.labelPosition == LabelPosition.BELOW
        val isLockscreenSafeYear = settings.preset == MatrixPreset.YEAR_PROGRESS
        val g = gridGeometry(size.width, size.height, settings, state.columns, state.rows)
        val titlePaint = titlePaint(size)
        val detailPaint = detailPaint(size)
        if (!labelBelow && !isLockscreenSafeYear) {
            canvas.drawText(settings.preset.title.uppercase(), g.left, max(titlePaint.textSize, g.top - size.height * .045f), titlePaint)
            canvas.drawText(state.label, g.left, max(titlePaint.textSize + detailPaint.textSize, g.top - size.height * .018f), detailPaint)
        }

        val past = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = settings.accentColor }
        val current = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = settings.todayColor }
        val future = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = settings.futureColor }
        repeat(state.total) { index ->
            val x = g.left + (index % state.columns) * (g.cell + g.gap)
            val y = g.top + (index / state.columns) * (g.cell + g.gap)
            val paint = when {
                index == state.currentIndex -> current
                index < state.completed -> past
                else -> future
            }
            drawDot(canvas, RectF(x, y, x + g.cell, y + g.cell), settings.dotShape, paint)
        }
        if (labelBelow) {
            drawLabelBelow(canvas, size, g, state, settings)
        } else if (isLockscreenSafeYear) {
            val remainingPaint = Paint(titlePaint).apply {
                textSize = max(28f, size.width * .065f)
                letterSpacing = .04f
            }
            canvas.drawText(state.label, g.left, size.height * (g.adjustedTop - .055f), remainingPaint)
            canvas.drawText("YEAR IN DOTS", g.left, size.height * (g.adjustedTop - .03f), detailPaint)
        }
    }

    private fun drawDot(canvas: Canvas, rect: RectF, shape: DotShape, paint: Paint) {
        when (shape) {
            DotShape.CIRCLE -> canvas.drawOval(rect, paint)
            DotShape.ROUNDED -> {
                val radius = max(1f, rect.width() * .24f)
                canvas.drawRoundRect(rect, radius, radius, paint)
            }
            DotShape.SQUARE -> canvas.drawRect(rect, paint)
        }
    }

    /** "80d left · 78%" centred under the grid, the days in today's colour and the percent muted. */
    private fun drawLabelBelow(canvas: Canvas, size: Size, g: GridGeometry, state: MatrixState, settings: OmniDotPreferences) {
        val textSize = max(14f, size.width * .028f)
        val lead = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = settings.todayColor
            this.textSize = textSize
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        }
        val rest = Paint(lead).apply { color = MUTED_TEXT }
        val tail = " · ${state.percent}%"
        val width = lead.measureText(state.shortLabel) + rest.measureText(tail)
        val x = (size.width - width) / 2f
        val gridBottom = g.top + state.rows * g.cell + (state.rows - 1) * g.gap
        val y = gridBottom + max(g.cell * 2.2f, textSize * 2.2f)
        canvas.drawText(state.shortLabel, x, y, lead)
        canvas.drawText(tail, x + lead.measureText(state.shortLabel), y, rest)
    }

    private fun drawGoals(canvas: Canvas, size: Size, settings: OmniDotPreferences, goals: List<Goal>, today: LocalDate) {
        val matrix = GoalsMatrix.build(goals, settings.lockPeriod, today)
        val g = gridGeometry(size.width, size.height, settings, matrix.columns, matrix.rows, GAP_RATIO)
        val title = titlePaint(size).apply { textSize = max(28f, size.width * .06f); letterSpacing = .02f }
        val detail = detailPaint(size)
        canvas.drawText(matrix.title, g.left, size.height * (g.adjustedTop - .055f), title)
        canvas.drawText(matrix.detail, g.left, size.height * (g.adjustedTop - .03f), detail)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = max(1.5f, g.cell * .12f) }
        matrix.cells.forEachIndexed { index, date ->
            if (date == null) return@forEachIndexed
            val x = g.left + (index % matrix.columns) * (g.cell + g.gap)
            val y = g.top + (index / matrix.columns) * (g.cell + g.gap)
            val rect = RectF(x, y, x + g.cell, y + g.cell)
            when (val state = GoalDays.dayState(date, goals, today)) {
                is DayState.Achieved -> drawTierShape(canvas, rect, state.tier, fill)
                DayState.Missed -> {
                    ring.color = MISSED_RING
                    canvas.drawOval(rect.inset(ring.strokeWidth), ring)
                }
                DayState.TodayPending -> {
                    ring.color = ACCENT
                    canvas.drawOval(rect.inset(ring.strokeWidth), ring)
                }
                DayState.NoGoal, DayState.Future -> {
                    fill.color = INACTIVE
                    canvas.drawOval(rect.inset(g.cell * .18f), fill)
                }
            }
        }
    }

    private fun drawTierShape(canvas: Canvas, rect: RectF, tier: GoalTier, paint: Paint) {
        when (tier) {
            GoalTier.SMALL -> {
                paint.color = TIER_SMALL
                canvas.drawOval(rect, paint)
            }
            GoalTier.MEDIUM -> {
                paint.color = TIER_MEDIUM
                val r = rect.width() * .24f
                canvas.drawRoundRect(rect, r, r, paint)
            }
            GoalTier.BIG -> {
                paint.color = TIER_BIG
                val path = Path().apply {
                    moveTo(rect.centerX(), rect.top)
                    lineTo(rect.right, rect.centerY())
                    lineTo(rect.centerX(), rect.bottom)
                    lineTo(rect.left, rect.centerY())
                    close()
                }
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun RectF.inset(by: Float) = RectF(left + by, top + by, right - by, bottom - by)

    suspend fun applyToLockscreen(context: Context, settings: OmniDotPreferences, goals: List<Goal>) = withContext(Dispatchers.IO) {
        val wallpaperManager = WallpaperManager.getInstance(context)
        check(wallpaperManager.isWallpaperSupported) { "Wallpapers are disabled on this device" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            check(wallpaperManager.isSetWallpaperAllowed) { "Wallpaper changes are blocked by device policy" }
        }
        // desiredMinimumWidth commonly describes a horizontally scrolling *home-screen*
        // wallpaper (often 2x the panel width). FLAG_LOCK needs the physical panel bounds;
        // using the former causes OEM lock screens to crop the matrix in half.
        val displaySize = physicalDisplaySize(context)
        val bitmap = render(displaySize, settings, goals)
        try {
            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
        } finally {
            bitmap.recycle()
        }
    }

    /** Lock screen wallpapers are portrait; normalise whatever orientation the display reports. */
    internal fun portraitSize(width: Int, height: Int): Pair<Int, Int> = minOf(width, height) to maxOf(width, height)

    // Read the panel from DisplayManager rather than WindowManager: it works from workers and
    // receivers (non-visual contexts), and is independent of split-screen or the current rotation.
    private fun physicalDisplaySize(context: Context): Size {
        val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        val mode = display.mode
        val (w, h) = portraitSize(mode.physicalWidth, mode.physicalHeight)
        return Size(w, h)
    }

    companion object {
        private const val GAP_RATIO = .42f
        private const val BACKGROUND = 0xFF121417.toInt()
        private const val INACTIVE = -14_277_082 // #FF262626
        private const val MUTED_TEXT = -6_579_301 // #FF9B9B9B
        private const val MISSED_RING = 0xFF3A3F45.toInt()
        const val ACCENT = 0xFF5FB3A6.toInt()
        const val TIER_SMALL = 0xFF7BC47F.toInt()
        const val TIER_MEDIUM = 0xFF6FA8DC.toInt()
        const val TIER_BIG = 0xFFE0B65A.toInt()
    }
}

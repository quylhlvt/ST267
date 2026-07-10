package com.oc.catemoji.catoc.ui.main.customize.vertical

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class VerticalSizeSlider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var progress: Float = 0.5f // 0.0 -> 1.0
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
            onProgressChanged?.invoke(field)
        }

    var onProgressChanged: ((Float) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EA4DA9")
    }

    private val trackWidthDp = 16f
    private val thumbRadiusDp = 14f

    private val trackWidth get() = trackWidthDp.dp
    private val thumbRadius get() = thumbRadiusDp.dp

    private val Float.dp get() = this * resources.displayMetrics.density
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (trackWidthDp + 42f * 2).dp.toInt()
        setMeasuredDimension(desiredWidth, MeasureSpec.getSize(heightMeasureSpec))
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val marginPx = 30f.dp
        val cx = width / 2f

        // Track chỉ vẽ trong vùng sau khi trừ margin
        val trackWidthPx = width - marginPx * 2
        val scaleX = trackWidthPx / 16f
        val scaleY = height / 191f

        // Offset để căn giữa
        val offsetX = marginPx

        val path = Path().apply {
            moveTo(offsetX + 16f * scaleX, 8f * scaleY)
            cubicTo(
                offsetX + 16f * scaleX, 4.88f * scaleY,
                offsetX + 14.73f * scaleX, 2.34f * scaleY,
                offsetX + 13.77f * scaleX, 2.34f * scaleY
            )
            cubicTo(
                offsetX + 12.27f * scaleX, 0.84f * scaleY,
                offsetX + 10.18f * scaleX, 0f,
                offsetX + 8f * scaleX, 0f
            )
            cubicTo(
                offsetX + 5.82f * scaleX, 0f,
                offsetX + 3.73f * scaleX, 0.84f * scaleY,
                offsetX + 2.24f * scaleX, 2.34f * scaleY
            )
            cubicTo(
                offsetX + 0.74f * scaleX, 3.84f * scaleY,
                offsetX - 0.06f * scaleX, 5.88f * scaleY,
                offsetX, 8f * scaleY
            )
            lineTo(offsetX + 0.25f * scaleX, 17f * scaleY)
            lineTo(offsetX + 4.75f * scaleX, 179f * scaleY)
            lineTo(offsetX + 5f * scaleX, 188f * scaleY)
            cubicTo(
                offsetX + 5.02f * scaleX, 188.77f * scaleY,
                offsetX + 5.36f * scaleX, 189.52f * scaleY,
                offsetX + 5.92f * scaleX, 190.06f * scaleY
            )
            cubicTo(
                offsetX + 6.49f * scaleX, 190.61f * scaleY,
                offsetX + 7.22f * scaleX, 190.92f * scaleY,
                offsetX + 8f * scaleX, 190.92f * scaleY
            )
            cubicTo(
                offsetX + 8.78f * scaleX, 190.92f * scaleY,
                offsetX + 9.51f * scaleX, 190.61f * scaleY,
                offsetX + 10.08f * scaleX, 190.06f * scaleY
            )
            cubicTo(
                offsetX + 10.64f * scaleX, 189.52f * scaleY,
                offsetX + 10.98f * scaleX, 188.77f * scaleY,
                offsetX + 11f * scaleX, 188f * scaleY
            )
            lineTo(offsetX + 11.25f * scaleX, 179f * scaleY)
            lineTo(offsetX + 15.75f * scaleX, 17f * scaleY)
            close()
        }

        canvas.drawPath(path, trackPaint)

        val thumbRadius = thumbRadius
        val usableTop = thumbRadius
        val usableBottom = height - thumbRadius
        val thumbY = usableTop + (1f - progress) * (usableBottom - usableTop)
        canvas.drawCircle(cx, thumbY, thumbRadius, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val thumbRadius = thumbRadius
        val topY = thumbRadius
        val bottomY = height - thumbRadius
        val trackLength = bottomY - topY

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                progress = 1f - ((event.y - topY) / trackLength)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
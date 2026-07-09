package com.oc.catemoji.catoc.core.extention

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Join
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.oc.catemoji.catoc.R
import ir.kotlin.kavehcolorpicker.dp

class OuterStrokeHaveShadownTextView : AppCompatTextView {

    private var outerStrokeWidth = 0f
    private var outerStrokeColor: Int = Color.WHITE
    private var outerStrokeJoin: Join = Join.ROUND
    private var strokeMiter = 2f
    private var extraPadding = 0
    private var isDrawingStroke = false

    // Lop chu thu 2 (de) - chi fill 1 mau, khong stroke, dich xuong duoi-phai
    private var dropShadowColor: Int = Color.TRANSPARENT
    private var dropShadowDx = 0f
    private var dropShadowDy = 0f
    private var dropShadowWidth = 0f
    constructor(context: Context) : super(context) { init(null) }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { init(attrs) }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { init(attrs) }

    private fun init(attrs: AttributeSet?) {
        if (attrs == null) return
        val a = context.obtainStyledAttributes(attrs, R.styleable.OuterStrokeHaveShowTextView)
        dropShadowWidth = a.getDimension(R.styleable.OuterStrokeHaveShowTextView_dropShadowWidth, 0f)
        try {
            outerStrokeWidth = a.getDimension(R.styleable.OuterStrokeHaveShowTextView_outerStrokeWidth, 0f)
            outerStrokeColor = a.getColor(R.styleable.OuterStrokeHaveShowTextView_outerStrokeColor, Color.WHITE)
            outerStrokeJoin = when (a.getInt(R.styleable.OuterStrokeHaveShowTextView_outerStrokeJoinStyle, 5)) {
                0 -> Join.MITER
                1 -> Join.BEVEL
                else -> Join.ROUND
            }
            dropShadowColor = a.getColor(R.styleable.OuterStrokeHaveShowTextView_dropShadowColor, Color.TRANSPARENT)
            dropShadowDx = a.getDimension(R.styleable.OuterStrokeHaveShowTextView_dropShadowDx, 0f)
            dropShadowDy = a.getDimension(R.styleable.OuterStrokeHaveShowTextView_dropShadowDy, 0f)
        } finally {
            a.recycle()
        }
        if (outerStrokeWidth > 0f) {
            extraPadding = (outerStrokeWidth * dp(1)).toInt()
        }
    }
    fun setOuterStrokeWidth(widthPx: Float) {
        outerStrokeWidth = widthPx
        if (extraPadding == 0) {
            // Cập nhật lại padding nếu cần khi đổi width runtime
            val newPadding = (widthPx * dp(1)).toInt()
            setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        }
        invalidate()
    }
    fun setOuterStrokeColor(color: Int) {
        outerStrokeColor = color
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (extraPadding > 0) {
            setPadding(paddingLeft + extraPadding, paddingTop, paddingRight + extraPadding, paddingBottom)
            extraPadding = 0
        }
        if (outerStrokeWidth > 0f) {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (outerStrokeWidth <= 0f) {
            super.onDraw(canvas)
            return
        }

        val p = paint
        val originalColors = textColors
        val originalStyle = p.style
        val originalStrokeWidth = p.strokeWidth
        val originalJoin = p.strokeJoin

        isDrawingStroke = true
        p.clearShadowLayer()

        if (dropShadowColor != Color.TRANSPARENT && (dropShadowDx != 0f || dropShadowDy != 0f)) {
            canvas.save()
            canvas.translate(dropShadowDx, dropShadowDy)

            super.setTextColor(dropShadowColor)

            if (dropShadowWidth > 0f) {
                p.style = Paint.Style.STROKE
                p.strokeWidth = dropShadowWidth
                p.strokeJoin = outerStrokeJoin
                p.strokeMiter = strokeMiter
                p.isAntiAlias = true
                super.onDraw(canvas)
            }

            p.style = Paint.Style.FILL
            p.isAntiAlias = true
            super.onDraw(canvas)

            canvas.restore()
        }

        // LOP TREN: stroke chu chinh
        super.setTextColor(outerStrokeColor)
        p.style = Paint.Style.STROKE
        p.strokeWidth = outerStrokeWidth
        p.strokeJoin = outerStrokeJoin
        p.strokeMiter = strokeMiter
        p.isAntiAlias = true
        super.onDraw(canvas)

        // LOP TREN: fill chu chinh
        isDrawingStroke = false
        super.setTextColor(originalColors)
        p.style = originalStyle
        p.strokeWidth = originalStrokeWidth
        p.strokeJoin = originalJoin
        super.onDraw(canvas)
    }

    override fun invalidate() {
        if (isDrawingStroke) return
        super.invalidate()
    }

    override fun postInvalidate() {
        if (isDrawingStroke) return
        super.postInvalidate()
    }
}
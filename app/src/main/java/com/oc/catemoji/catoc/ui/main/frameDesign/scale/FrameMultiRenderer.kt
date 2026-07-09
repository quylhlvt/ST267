package com.oc.catemoji.catoc.ui.main.frameDesign.scale

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import com.oc.catemoji.catoc.data.model.frameDesign.CropImage

object FrameMultiRenderer {

    fun render(
        frame: Bitmap,
        rects: List<Rect>,
        selectedIndex: Int,
        selectedImages: Map<Int, CropImage>,
        density: Float = 1f
    ): Bitmap {

        val result = createBitmap(
            frame.width,
            frame.height
        )

        val canvas = Canvas(result)

        val paint = Paint(
            Paint.ANTI_ALIAS_FLAG or
                    Paint.FILTER_BITMAP_FLAG
        )
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val plusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 4f * density
        }
        val selectedColor = Color.parseColor("#D3338E")
        val unselectedColor = Color.rgb(170, 170, 170)

        rects.forEachIndexed { index, rect ->

            val cropImage = selectedImages[index]
            val isSelected = selectedIndex == index

            canvas.drawRect(rect, whitePaint)

            if (cropImage != null) {

                canvas.save()

                // Giới hạn ảnh trong ô
                canvas.clipRect(rect)

                val bitmap = cropImage.bitmap

                // FitCenter mặc định
                val baseScale = minOf(
                    rect.width().toFloat() / bitmap.width,
                    rect.height().toFloat() / bitmap.height
                )

                val finalScale =
                    baseScale * cropImage.scale

                val drawW =
                    bitmap.width * finalScale

                val drawH =
                    bitmap.height * finalScale

                val centerX =
                    rect.centerX() + cropImage.offsetX

                val centerY =
                    rect.centerY() + cropImage.offsetY

                val left =
                    centerX - drawW / 2f

                val top =
                    centerY - drawH / 2f

                // XOAY
                canvas.rotate(
                    cropImage.rotation,
                    centerX,
                    centerY
                )

                canvas.drawBitmap(
                    bitmap,
                    null,
                    RectF(
                        left,
                        top,
                        left + drawW,
                        top + drawH
                    ),
                    paint
                )

                canvas.restore()
            }else {
                drawPlus(
                    canvas = canvas,
                    rect = rect,
                    paint = plusPaint,
                    color = if (isSelected) selectedColor else unselectedColor,
                    density = density
                )
            }
        }

        // Frame luôn nằm trên ảnh
        canvas.drawBitmap(
            frame,
            0f,
            0f,
            paint
        )

        // Hiển thị ô đang chọn
        if (selectedIndex in rects.indices) {

            val focusPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {

                    style = Paint.Style.STROKE

                    strokeWidth = 1f * density

                    color = selectedColor
                }

            val inset = 5f * density + focusPaint.strokeWidth / 2f
            val rect = rects[selectedIndex]
            canvas.drawRect(
                RectF(
                    rect.left + inset,
                    rect.top + inset,
                    rect.right - inset,
                    rect.bottom - inset
                ),
                focusPaint
            )
        }

        return result
    }

    private fun drawPlus(
        canvas: Canvas,
        rect: Rect,
        paint: Paint,
        color: Int,
        density: Float
    ) {
        paint.color = color

        val size = minOf(rect.width(), rect.height()) * 0.18f
        val minSize = 18f * density
        val maxSize = 44f * density
        val half = size.coerceIn(minSize, maxSize) / 2f
        val cx = rect.exactCenterX()
        val cy = rect.exactCenterY()

        canvas.drawLine(cx - half, cy, cx + half, cy, paint)
        canvas.drawLine(cx, cy - half, cx, cy + half, paint)
    }
}

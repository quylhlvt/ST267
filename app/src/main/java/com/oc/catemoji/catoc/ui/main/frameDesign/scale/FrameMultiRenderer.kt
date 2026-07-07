package com.example.imagetestscale.scale

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import com.example.imagetestscale.model.CropImage

object FrameMultiRenderer {

    fun render(
        frame: Bitmap,
        rects: List<Rect>,
        selectedIndex: Int,
        selectedImages: Map<Int, CropImage>
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

        rects.forEachIndexed { index, rect ->

            val cropImage = selectedImages[index]

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

                val bgPaint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {

                        color = Color.argb(
                            60,
                            120,
                            120,
                            120
                        )
                    }

                canvas.drawRect(
                    rect,
                    bgPaint
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

                    strokeWidth = 6f

                    color = Color.YELLOW
                }

            canvas.drawRect(
                rects[selectedIndex],
                focusPaint
            )
        }

        return result
    }
}
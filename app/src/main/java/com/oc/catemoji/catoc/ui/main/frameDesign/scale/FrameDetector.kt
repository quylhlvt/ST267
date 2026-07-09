package com.oc.catemoji.catoc.ui.main.frameDesign.scale

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import kotlin.math.roundToInt

object FrameDetector {

    fun detectAllTransparentRects(frame: Bitmap): List<Rect> {
        val w = frame.width
        val h = frame.height

        val pixels = IntArray(w * h)
        frame.getPixels(pixels, 0, w, 0, 0, w, h)

        val visited = BooleanArray(w * h)
        val result = mutableListOf<Rect>()

        fun index(x: Int, y: Int) = y * w + x

        fun isTransparent(pixelIndex: Int): Boolean {
            return Color.alpha(pixels[pixelIndex]) < 30
        }

        val queueX = IntArray(w * h)
        val queueY = IntArray(w * h)
        val minArea = (w * h * 0.005f).roundToInt()

        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = index(x, y)

                if (visited[i] || !isTransparent(i)) continue

                visited[i] = true
                var head = 0
                var tail = 0
                queueX[tail] = x
                queueY[tail] = y
                tail++

                var minX = x
                var minY = y
                var maxX = x
                var maxY = y
                var count = 0
                var touchesBorder = false

                while (head < tail) {
                    val cx = queueX[head]
                    val cy = queueY[head]
                    head++
                    count++

                    if (cx == 0 || cy == 0 || cx == w - 1 || cy == h - 1) {
                        touchesBorder = true
                    }

                    minX = minOf(minX, cx)
                    minY = minOf(minY, cy)
                    maxX = maxOf(maxX, cx)
                    maxY = maxOf(maxY, cy)

                    if (cx + 1 < w) {
                        val nx = cx + 1
                        val ni = index(nx, cy)
                        if (!visited[ni] && isTransparent(ni)) {
                            visited[ni] = true
                            queueX[tail] = nx
                            queueY[tail] = cy
                            tail++
                        }
                    }
                    if (cx - 1 >= 0) {
                        val nx = cx - 1
                        val ni = index(nx, cy)
                        if (!visited[ni] && isTransparent(ni)) {
                            visited[ni] = true
                            queueX[tail] = nx
                            queueY[tail] = cy
                            tail++
                        }
                    }
                    if (cy + 1 < h) {
                        val ny = cy + 1
                        val ni = index(cx, ny)
                        if (!visited[ni] && isTransparent(ni)) {
                            visited[ni] = true
                            queueX[tail] = cx
                            queueY[tail] = ny
                            tail++
                        }
                    }
                    if (cy - 1 >= 0) {
                        val ny = cy - 1
                        val ni = index(cx, ny)
                        if (!visited[ni] && isTransparent(ni)) {
                            visited[ni] = true
                            queueX[tail] = cx
                            queueY[tail] = ny
                            tail++
                        }
                    }
                }

                val rect = Rect(minX, minY, maxX + 1, maxY + 1)

                if (!touchesBorder && count >= minArea) {
                    result.add(rect)
                }
            }
        }

        return result.sortedWith(
            compareBy<Rect> { it.top }.thenBy { it.left }
        )
    }
}

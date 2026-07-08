package com.oc.catemoji.catoc.ui.main.frameDesign.scale

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect

object FrameDetector {

    fun detectAllTransparentRects(frame: Bitmap): List<Rect> {
        val w = frame.width
        val h = frame.height

        val visited = BooleanArray(w * h)
        val result = mutableListOf<Rect>()

        fun index(x: Int, y: Int) = y * w + x

        fun isTransparent(x: Int, y: Int): Boolean {
            return Color.alpha(frame.getPixel(x, y)) < 30
        }

        val queue = ArrayDeque<Pair<Int, Int>>()

        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = index(x, y)

                if (visited[i] || !isTransparent(x, y)) continue

                visited[i] = true
                queue.add(x to y)

                var minX = x
                var minY = y
                var maxX = x
                var maxY = y
                var count = 0
                var touchesBorder = false

                while (queue.isNotEmpty()) {
                    val (cx, cy) = queue.removeFirst()
                    count++

                    if (cx == 0 || cy == 0 || cx == w - 1 || cy == h - 1) {
                        touchesBorder = true
                    }

                    minX = minOf(minX, cx)
                    minY = minOf(minY, cy)
                    maxX = maxOf(maxX, cx)
                    maxY = maxOf(maxY, cy)

                    val dirs = arrayOf(
                        cx + 1 to cy,
                        cx - 1 to cy,
                        cx to cy + 1,
                        cx to cy - 1
                    )

                    for ((nx, ny) in dirs) {
                        if (nx !in 0 until w || ny !in 0 until h) continue

                        val ni = index(nx, ny)
                        if (visited[ni] || !isTransparent(nx, ny)) continue

                        visited[ni] = true
                        queue.add(nx to ny)
                    }
                }

                val rect = Rect(minX, minY, maxX + 1, maxY + 1)
                val minArea = w * h * 0.005f

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
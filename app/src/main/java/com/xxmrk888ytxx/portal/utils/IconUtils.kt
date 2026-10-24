package com.xxmrk888ytxx.portal.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.max

object IconUtils {
    fun renderSquircleBitmap(original: Bitmap): Bitmap {
        val size = 512
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. Draw pure white squircle background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
        }
        val outerRect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        val cornerRadius = size * 0.22f
        canvas.drawRoundRect(outerRect, cornerRadius, cornerRadius, bgPaint)

        // 2. Inner squircle clip with margin
        val innerMargin = 16f
        val innerRect = RectF(innerMargin, innerMargin, size - innerMargin, size - innerMargin)
        val innerRadius = cornerRadius - 4f
        val clipPath = Path().apply {
            addRoundRect(innerRect, innerRadius, innerRadius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)

        // 3. Center crop & scale original image
        val srcWidth = original.width.toFloat()
        val srcHeight = original.height.toFloat()
        val scale = max(innerRect.width() / srcWidth, innerRect.height() / srcHeight)
        val scaledWidth = srcWidth * scale
        val scaledHeight = srcHeight * scale
        val left = innerRect.left + (innerRect.width() - scaledWidth) / 2f
        val top = innerRect.top + (innerRect.height() - scaledHeight) / 2f
        canvas.drawBitmap(original, null, RectF(left, top, left + scaledWidth, top + scaledHeight), Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()

        return output
    }
}

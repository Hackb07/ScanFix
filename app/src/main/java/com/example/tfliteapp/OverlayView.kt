package com.example.tfliteapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.LinkedList
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<Detection> = LinkedList<Detection>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var scaleFactor: Float = 1f
    private var bounds = Rect()
    
    // Store image dimensions to map scan area back to image space
    private var lastImageWidth = 1
    private var lastImageHeight = 1

    init {
        initPaints()
    }

    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.accent_mint)
        boxPaint.strokeWidth = 4F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        for (result in results) {
            val boundingBox = result.boundingBox

            // Skip results with no categories to avoid crash
            if (result.categories.isNullOrEmpty()) continue

            var left = boundingBox.left * scaleFactor
            var top = boundingBox.top * scaleFactor
            var right = boundingBox.right * scaleFactor
            var bottom = boundingBox.bottom * scaleFactor

            // Clamp bounding box to screen
            left = left.coerceIn(0f, width.toFloat())
            top = top.coerceIn(0f, height.toFloat())
            right = right.coerceIn(0f, width.toFloat())
            bottom = bottom.coerceIn(0f, height.toFloat())

            // Draw bounding box
            val drawableRect = RectF(left, top, right, bottom)
            
            // Re-configure paint for cleaner look
            boxPaint.strokeWidth = 4F
            boxPaint.color = ContextCompat.getColor(context!!, R.color.accent_mint) // Use mint accent
            
            canvas.drawRect(drawableRect, boxPaint)

            // Create text to display alongside bounding box
            val drawableText =
                result.categories[0].label + " " +
                        String.format("%.2f", result.categories[0].score)

            // Draw rect behind display text
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            
            // Adjust text position ensuring it stays on screen
            var textLeft = left
            var textTop = top - textHeight - 8
            
            if (textTop < 0) {
                textTop = top + 8
            }
            if (textLeft + textWidth + 16 > width) {
                textLeft = width - textWidth.toFloat() - 16f
            }
            
            canvas.drawRect(
                textLeft,
                textTop,
                textLeft + textWidth + 16,
                textTop + textHeight + 8,
                textBackgroundPaint
            )

            // Draw text for detected object
            canvas.drawText(drawableText, textLeft + 8, textTop + textHeight, textPaint)
        }
    }

    fun setResults(
        detectionResults: MutableList<Detection>,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        results = detectionResults
        lastImageHeight = imageHeight
        lastImageWidth = imageWidth

        // PreviewView is in FILL_START mode, so we need to scale the detection result
        // to match the view size.
        // For simplicity assuming width matches and height is scaled accordingly or vice versa
        // A better implementation would handle aspect ratio properly.
        // Here we just use the max scale factor logic or simple width ratio
        
        // This logic is simplified. In a real app complexity arises from handling rotation and fit types.
        // We will assume the image fills width.
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)

        invalidate()
    }

    // Define the scanning area as a percentage of the screen
    fun getScanAreaRect(): RectF {
        val cx = width / 2f
        val cy = height / 2f
        
        val boxWidth = width * 0.7f
        val boxHeight = width * 0.7f // Make it a square for now
        
        return RectF(
            cx - boxWidth / 2f,
            cy - boxHeight / 2f,
            cx + boxWidth / 2f,
            cy + boxHeight / 2f
        )
    }

    // Return the scan area mapped to image coordinates so the Detector can filter
    fun getScanAreaInImageCoordinates(): Rect {
        val scanRect = getScanAreaRect()
        if (scaleFactor == 0f) return Rect()

        return Rect(
            (scanRect.left / scaleFactor).toInt(),
            (scanRect.top / scaleFactor).toInt(),
            (scanRect.right / scaleFactor).toInt(),
            (scanRect.bottom / scaleFactor).toInt()
        )
    }
}

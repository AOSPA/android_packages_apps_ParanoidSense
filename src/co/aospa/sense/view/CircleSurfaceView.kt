package co.aospa.sense.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.SurfaceView
import android.view.animation.AccelerateDecelerateInterpolator
import co.aospa.sense.R
import kotlin.math.abs

class CircleSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr) {

    private var progressAnimator: ValueAnimator? = null
    private var currentProgress = 0.0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circlePath = Path()
    private val rectF = RectF()

    fun setProgress(progress: Float) {
        if (progress !in 0.0f..100.0f) return

        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(currentProgress, progress).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = abs(1000 * ((progress - currentProgress) / 100)).toLong()

            addUpdateListener { animation ->
                currentProgress = animation.animatedValue as Float
                invalidate()
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    progressAnimator = null
                }
            })

            start()
        }
    }

    override fun draw(canvas: Canvas) {
        val centerX = (measuredWidth / 2).toFloat()
        val centerY = (measuredHeight / 2).toFloat()
        val radius = minOf(centerX, centerY)

        // Set up drawing area
        rectF.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // Draw background arc
        paint.color = context.getColor(R.color.theme_accent_200)
        canvas.drawArc(rectF, START_ANGLE, DEGREES_IN_CIRCLE, true, paint)

        // Draw progress arc
        paint.color = context.getColor(R.color.theme_accent_primary)
        canvas.drawArc(rectF, START_ANGLE, currentProgress * PROGRESS_TO_ANGLE_RATIO, true, paint)

        // Create and apply circular clip
        circlePath.reset()
        circlePath.addCircle(centerX, centerY, radius * 0.95f, Path.Direction.CCW)
        canvas.clipPath(circlePath)

        super.draw(canvas)
        invalidate()
    }

    companion object {
        private const val DEGREES_IN_CIRCLE = 360f
        private const val PROGRESS_TO_ANGLE_RATIO = DEGREES_IN_CIRCLE / 100f  // 3.6f
        private const val START_ANGLE = 270f
    }
}
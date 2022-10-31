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

class CircleSurfaceView : SurfaceView {

    private var mProgressAnimator: ValueAnimator? = null
    private var mProgress = 0.0f

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet) {}
    constructor(context: Context?, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
    }

    fun setProgress(progress: Float) {
        if (progress in 0.0f..100.0f) {
            if (mProgressAnimator != null) {
                mProgressAnimator!!.cancel()
                mProgressAnimator = null
            }
            mProgressAnimator = ValueAnimator.ofFloat(mProgress, progress)
            mProgressAnimator?.interpolator = AccelerateDecelerateInterpolator()
            val duration = abs(1000 * ((progress - mProgress) / 100)).toLong()
            mProgressAnimator?.duration = duration
            mProgressAnimator?.addUpdateListener { animation: ValueAnimator ->
                mProgress = animation.animatedValue as Float
                invalidate()
            }
            mProgressAnimator?.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mProgressAnimator = null
                }
            })
            mProgressAnimator?.start()
        }
    }

    override fun draw(canvas: Canvas) {
        val measuredWidth = (measuredWidth / 2).toFloat()
        val measuredHeight = (measuredHeight / 2).toFloat()
        val min = measuredWidth.coerceAtMost(measuredHeight)
        val rectF = RectF(
            measuredWidth - min,
            measuredHeight - min,
            measuredWidth + min,
            measuredHeight + min
        )
        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = context.getColor(R.color.theme_accent_200)
        canvas.drawArc(rectF, 270.0f, 360.0f, true, paint)
        paint.color = context.getColor(R.color.theme_accent_primary)
        canvas.drawArc(rectF, 270.0f, mProgress * 3.6f, true, paint)
        val path = Path()
        path.addCircle(measuredWidth, measuredHeight, min * 0.95f, Path.Direction.CCW)
        canvas.clipPath(path)
        super.draw(canvas)
        invalidate()
    }
}
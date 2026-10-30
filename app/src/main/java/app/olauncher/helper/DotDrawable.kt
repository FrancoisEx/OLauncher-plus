package app.olauncher.helper

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * Olauncher+ : petit point plein dessine dans le coin superieur droit de la vue
 * qui le porte (utilise en avant-plan des icones rapides).
 */
class DotDrawable(color: Int, private val radiusPx: Float) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    override fun draw(canvas: Canvas) {
        val b = bounds
        canvas.drawCircle(b.right - radiusPx * 1.6f, b.top + radiusPx * 1.6f, radiusPx, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

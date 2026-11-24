package com.musikku.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator

object Ui {
    fun dp(ctx: Context, v: Float): Float = v * ctx.resources.displayMetrics.density
    fun hue(seed: String): Float = (((seed.hashCode() % 360) + 360) % 360).toFloat()
    fun top(seed: String): Int = Color.HSVToColor(floatArrayOf(hue(seed), 0.28f, 0.62f))
    fun bottom(seed: String): Int = Color.HSVToColor(floatArrayOf((hue(seed) + 25f) % 360f, 0.60f, 0.14f))
}

/** Ikon digambar manual (grid 24x24), jadi tidak tergantung font / file gambar. */
object Icons {
    const val HOME = 0
    const val MUSIC = 1
    const val LIST = 2
    const val PLAY = 3
    const val PAUSE = 4
    const val PREV = 5
    const val NEXT = 6
    const val REPEAT = 7
    const val REPEAT_ONE = 8
    const val SHUFFLE = 9
    const val BACK = 10
    const val CHEVRON = 11
    const val PLUS = 12
    const val DOTS = 13

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun poly(c: Canvas, pts: FloatArray, close: Boolean) {
        val path = Path()
        path.moveTo(pts[0], pts[1])
        var i = 2
        while (i < pts.size) {
            path.lineTo(pts[i], pts[i + 1])
            i += 2
        }
        if (close) path.close()
        c.drawPath(path, p)
    }

    fun draw(c: Canvas, type: Int, tint: Int, w: Float, h: Float, iconScale: Float) {
        val u = minOf(w, h) / 24f * iconScale
        c.save()
        c.translate((w - 24f * u) / 2f, (h - 24f * u) / 2f)
        c.scale(u, u)
        p.reset()
        p.isAntiAlias = true
        p.color = tint
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = 2f
        p.style = Paint.Style.FILL
        when (type) {
            HOME -> {
                p.style = Paint.Style.FILL_AND_STROKE
                p.strokeWidth = 1.4f
                poly(c, floatArrayOf(12f, 3.5f, 3f, 11.5f, 5.2f, 11.5f, 5.2f, 20f, 10f, 20f, 10f, 14.5f,
                    14f, 14.5f, 14f, 20f, 18.8f, 20f, 18.8f, 11.5f, 21f, 11.5f), true)
            }
            MUSIC -> {
                c.drawCircle(8f, 17.5f, 3.2f, p)
                c.drawRect(10.2f, 4.5f, 12f, 17.5f, p)
                poly(c, floatArrayOf(12f, 4.5f, 18.5f, 6.5f, 18.5f, 10f, 12f, 8f), true)
            }
            LIST -> {
                p.style = Paint.Style.STROKE
                poly(c, floatArrayOf(4f, 7f, 20f, 7f), false)
                poly(c, floatArrayOf(4f, 12f, 20f, 12f), false)
                poly(c, floatArrayOf(4f, 17f, 14f, 17f), false)
            }
            PLAY -> {
                p.style = Paint.Style.FILL_AND_STROKE
                p.strokeWidth = 1.6f
                poly(c, floatArrayOf(8f, 5.5f, 19f, 12f, 8f, 18.5f), true)
            }
            PAUSE -> {
                c.drawRoundRect(6f, 4.5f, 10.2f, 19.5f, 1.2f, 1.2f, p)
                c.drawRoundRect(13.8f, 4.5f, 18f, 19.5f, 1.2f, 1.2f, p)
            }
            PREV -> {
                c.drawRoundRect(5f, 5f, 7.4f, 19f, 1.2f, 1.2f, p)
                p.style = Paint.Style.FILL_AND_STROKE
                p.strokeWidth = 1.4f
                poly(c, floatArrayOf(19f, 5.5f, 19f, 18.5f, 9f, 12f), true)
            }
            NEXT -> {
                c.drawRoundRect(16.6f, 5f, 19f, 19f, 1.2f, 1.2f, p)
                p.style = Paint.Style.FILL_AND_STROKE
                p.strokeWidth = 1.4f
                poly(c, floatArrayOf(5f, 5.5f, 5f, 18.5f, 15f, 12f), true)
            }
            REPEAT, REPEAT_ONE -> {
                p.style = Paint.Style.STROKE
                c.drawRoundRect(4f, 7f, 20f, 17f, 4f, 4f, p)
                p.style = Paint.Style.FILL
                poly(c, floatArrayOf(15.5f, 3.8f, 20f, 7f, 15.5f, 10.2f), true)
                poly(c, floatArrayOf(8.5f, 13.8f, 4f, 17f, 8.5f, 20.2f), true)
                if (type == REPEAT_ONE) {
                    p.textSize = 7.5f
                    p.textAlign = Paint.Align.CENTER
                    p.typeface = Typeface.DEFAULT_BOLD
                    c.drawText("1", 12f, 14.6f, p)
                }
            }
            SHUFFLE -> {
                p.style = Paint.Style.STROKE
                poly(c, floatArrayOf(4f, 17f, 8.5f, 17f, 14f, 7f, 19f, 7f), false)
                poly(c, floatArrayOf(4f, 7f, 8.5f, 7f, 10.5f, 10f), false)
                poly(c, floatArrayOf(13f, 14.5f, 14.5f, 17f, 19f, 17f), false)
                p.style = Paint.Style.FILL
                poly(c, floatArrayOf(17f, 4.2f, 21.5f, 7f, 17f, 9.8f), true)
                poly(c, floatArrayOf(17f, 14.2f, 21.5f, 17f, 17f, 19.8f), true)
            }
            BACK -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2.4f
                poly(c, floatArrayOf(15f, 5f, 8f, 12f, 15f, 19f), false)
            }
            CHEVRON -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2.4f
                poly(c, floatArrayOf(9f, 5f, 16f, 12f, 9f, 19f), false)
            }
            PLUS -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2.4f
                poly(c, floatArrayOf(12f, 5f, 12f, 19f), false)
                poly(c, floatArrayOf(5f, 12f, 19f, 12f), false)
            }
            DOTS -> {
                c.drawCircle(12f, 5.5f, 2f, p)
                c.drawCircle(12f, 12f, 2f, p)
                c.drawCircle(12f, 18.5f, 2f, p)
            }
            else -> {
            }
        }
        c.restore()
    }
}

class IconView(
    ctx: Context,
    private var type: Int,
    private var tint: Int,
    private val iconScale: Float = 0.6f
) : View(ctx) {
    fun update(t: Int, c: Int) {
        type = t
        tint = c
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        Icons.draw(canvas, type, tint, width.toFloat(), height.toFloat(), iconScale)
    }
}

/** Kotak gradien gelap (warna dari nama lagu / playlist) dengan huruf awal atau ikon. */
class ArtView(ctx: Context) : View(ctx) {
    companion object {
        const val NONE = 0
        const val LETTER = 1
        const val NOTE = 2
        const val PLUS = 3
        const val PLAY = 4
    }

    var seed: String = ""
        set(v) { field = v; invalidate() }
    var label: String = ""
        set(v) { field = v; invalidate() }
    var content: Int = NONE
        set(v) { field = v; invalidate() }
    var radiusDp: Float = 14f

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = Ui.dp(context, radiusDp)
        val clip = Path()
        clip.addRoundRect(RectF(0f, 0f, w, h), r, r, Path.Direction.CW)
        c.save()
        c.clipPath(clip)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, 0f, h, Ui.top(seed), Ui.bottom(seed), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        p.color = Color.argb(34, 255, 255, 255)
        c.drawCircle(w * 0.25f, h * 0.18f, minOf(w, h) * 0.55f, p)
        c.restore()
        when (content) {
            LETTER -> {
                p.color = Color.argb(235, 255, 255, 255)
                p.textAlign = Paint.Align.CENTER
                p.typeface = Typeface.DEFAULT_BOLD
                p.textSize = minOf(w, h) * 0.42f
                c.drawText(label, w / 2f, h / 2f - (p.descent() + p.ascent()) / 2f, p)
            }
            NOTE -> Icons.draw(c, Icons.MUSIC, Color.WHITE, w, h, 0.5f)
            PLUS -> Icons.draw(c, Icons.PLUS, Color.WHITE, w, h, 0.5f)
            PLAY -> Icons.draw(c, Icons.PLAY, Color.WHITE, w, h, 0.5f)
        }
    }
}

/** Piringan hitam berputar + jarum (tonearm) yang turun saat lagu diputar. */
class TurntableView(ctx: Context) : View(ctx) {
    var seed: String = "Musikku"
        set(v) { field = v; invalidate() }
    var label: String = "M"
        set(v) { field = v; invalidate() }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lift = -22f
    private var rot = 0f
    private var arm = lift
    private var progress = 0f
    private var playing = false
    private var armAnim: ValueAnimator? = null

    private val spin: ValueAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 9000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            rot = it.animatedValue as Float
            invalidate()
        }
    }

    private fun drift(): Float = progress * 7f

    fun setPlaying(on: Boolean) {
        playing = on
        if (on) {
            if (!spin.isStarted) spin.start() else if (spin.isPaused) spin.resume()
        } else {
            if (spin.isStarted && !spin.isPaused) spin.pause()
        }
        moveArm(if (on) drift() else lift)
    }

    fun setProgress(f: Float) {
        progress = f
        if (playing && armAnim?.isRunning != true) {
            arm = drift()
            invalidate()
        }
    }

    private fun moveArm(target: Float) {
        armAnim?.cancel()
        val a = ValueAnimator.ofFloat(arm, target)
        a.duration = 450
        a.addUpdateListener {
            arm = it.animatedValue as Float
            invalidate()
        }
        armAnim = a
        a.start()
    }

    override fun onDetachedFromWindow() {
        spin.cancel()
        armAnim?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        c.save()
        c.translate((width - s) / 2f, 0f)
        val cx = s * 0.45f
        val cy = s * 0.55f
        val rad = s * 0.40f

        // bayangan lembut
        p.style = Paint.Style.FILL
        for (i in 6 downTo 1) {
            p.color = Color.argb(10, 0, 0, 0)
            c.drawCircle(cx, cy + rad * 0.05f * i / 2f, rad + rad * 0.03f * i, p)
        }

        // piringan (berputar)
        c.save()
        c.rotate(rot, cx, cy)
        p.color = Color.parseColor("#111111")
        c.drawCircle(cx, cy, rad, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        p.color = Color.argb(30, 255, 255, 255)
        var gr = rad * 0.97f
        while (gr > rad * 0.42f) {
            c.drawCircle(cx, cy, gr, p)
            gr -= rad * 0.045f
        }
        p.style = Paint.Style.FILL
        p.color = Color.argb(24, 255, 255, 255)
        val oval = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        c.drawArc(oval, -60f, 28f, true, p)
        c.drawArc(oval, 120f, 28f, true, p)

        val lr = rad * 0.36f
        p.shader = LinearGradient(cx - lr, cy - lr, cx + lr, cy + lr, Ui.top(seed), Ui.bottom(seed), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, lr, p)
        p.shader = null
        p.color = Color.argb(235, 255, 255, 255)
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = lr * 1.1f
        c.drawText(label, cx, cy - (p.descent() + p.ascent()) / 2f, p)
        p.color = Color.parseColor("#FBF8F0")
        c.drawCircle(cx, cy, rad * 0.035f, p)
        c.restore()

        // jarum
        val px = s * 0.86f
        val py = s * 0.10f
        p.style = Paint.Style.FILL
        p.color = Color.parseColor("#E6E2D8")
        c.drawCircle(px, py, s * 0.055f, p)
        p.color = Color.WHITE
        c.drawCircle(px, py, s * 0.04f, p)

        c.save()
        c.translate(px, py)
        c.rotate(arm)
        val tube = Path()
        tube.moveTo(0f, 0f)
        tube.lineTo(-s * 0.02f, s * 0.50f)
        tube.lineTo(-s * 0.26f, s * 0.64f)
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = s * 0.02f
        p.color = Color.parseColor("#B8BEC6")
        c.drawPath(tube, p)
        p.strokeWidth = s * 0.011f
        p.color = Color.parseColor("#F4F6F8")
        c.drawPath(tube, p)

        c.save()
        c.translate(-s * 0.26f, s * 0.64f)
        c.rotate(150f)
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        c.drawRoundRect(RectF(-s * 0.01f, -s * 0.026f, s * 0.10f, s * 0.026f), s * 0.012f, s * 0.012f, p)
        p.color = Color.parseColor("#2B2B2B")
        c.drawRoundRect(RectF(s * 0.07f, -s * 0.026f, s * 0.10f, s * 0.026f), s * 0.012f, s * 0.012f, p)
        c.restore()
        c.restore()
        c.restore()
    }
}

/** Progress bar tipis, bisa digeser untuk seek. */
class ThinSeek(ctx: Context) : View(ctx) {
    var progress = 0f
    var dragging = false
    var onSeek: ((Float) -> Unit)? = null
    var onDrag: ((Float) -> Unit)? = null
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setProg(f: Float) {
        if (!dragging) {
            progress = f.coerceIn(0f, 1f)
            invalidate()
        }
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val y = height / 2f
        val t = Ui.dp(context, 3f)
        p.style = Paint.Style.FILL
        p.color = Color.parseColor("#D9D6CE")
        c.drawRoundRect(0f, y - t / 2f, w, y + t / 2f, t, t, p)
        p.color = Color.parseColor("#1F1F1F")
        c.drawRoundRect(0f, y - t / 2f, w * progress, y + t / 2f, t, t, p)
        if (dragging) c.drawCircle(w * progress, y, Ui.dp(context, 7f), p)
    }

    private fun upd(e: MotionEvent) {
        progress = (e.x / width).coerceIn(0f, 1f)
        onDrag?.invoke(progress)
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
                upd(e)
            }
            MotionEvent.ACTION_MOVE -> upd(e)
            MotionEvent.ACTION_UP -> {
                upd(e)
                dragging = false
                onSeek?.invoke(progress)
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                invalidate()
            }
        }
        return true
    }
}

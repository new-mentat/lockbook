package app.lockbook.loggedin.editor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import app.lockbook.R
import app.lockbook.utils.Drawing
import app.lockbook.utils.Event
import app.lockbook.utils.PressurePoint
import app.lockbook.utils.Stroke
import timber.log.Timber

class HandwritingEditorView(context: Context, attributeSet: AttributeSet?) :
    SurfaceView(context, attributeSet), Runnable {
    private val activePaint = Paint()
    private val lastPoint = PointF()
    private val activePath = Path()
    private val viewPort = Rect()
    private val bitmapPaint = Paint()
    var isThreadRunning = false
    private lateinit var canvasBitmap: Bitmap
    private lateinit var tempCanvas: Canvas
    var lockBookDrawable: Drawing = Drawing()
    private val scaleGestureDetector =
        ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    lockBookDrawable.page.transformation.scale *= detector.scaleFactor
                    lockBookDrawable.page.transformation.scale = 0.7f.coerceAtLeast(
                        lockBookDrawable.page.transformation.scale.coerceAtMost(2.0f)
                    )

                    lockBookDrawable.page.transformation.scaleFocus.x = detector.focusX
                    lockBookDrawable.page.transformation.scaleFocus.y = detector.focusY

                    return true
                }
            }
        )

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent?,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                lockBookDrawable.page.transformation.translation.x += -distanceX * lockBookDrawable.page.transformation.scale
                lockBookDrawable.page.transformation.translation.y += -distanceY * lockBookDrawable.page.transformation.scale

                return true
            }
        }
    )

    init {
        activePaint.isAntiAlias = true
        activePaint.style = Paint.Style.STROKE
        activePaint.strokeJoin = Paint.Join.ROUND
        activePaint.color = Color.WHITE
        activePaint.strokeCap = Paint.Cap.ROUND

        bitmapPaint.strokeCap = Paint.Cap.ROUND
        bitmapPaint.strokeJoin = Paint.Join.ROUND
    }

    private fun drawBitmap(canvas: Canvas) {
        canvas.save()
        canvas.translate(
            lockBookDrawable.page.transformation.translation.x,
            lockBookDrawable.page.transformation.translation.y
        )
        canvas.scale(
            lockBookDrawable.page.transformation.scale,
            lockBookDrawable.page.transformation.scale,
            lockBookDrawable.page.transformation.scaleFocus.x,
            lockBookDrawable.page.transformation.scaleFocus.y
        )
        viewPort.set(canvas.clipBounds)
        canvas.drawColor(
            Color.TRANSPARENT,
            PorterDuff.Mode.CLEAR
        )
        canvas.drawBitmap(canvasBitmap, 0f, 0f, bitmapPaint)
        canvas.restore()
    }

    fun setColor(color: String) {
        when (color) {
            resources.getString(R.string.handwriting_editor_pallete_white) -> activePaint.color =
                Color.WHITE
            resources.getString(R.string.handwriting_editor_pallete_blue) -> activePaint.color =
                Color.BLUE
            resources.getString(R.string.handwriting_editor_pallete_red) -> activePaint.color =
                Color.RED
            resources.getString(R.string.handwriting_editor_pallete_yellow) -> activePaint.color =
                Color.YELLOW
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val beginTime = System.nanoTime()
        if (event != null) {
            for (point in 0 until event.pointerCount) {
                if (event.getToolType(point) == MotionEvent.TOOL_TYPE_STYLUS ||
                    event.getToolType(point) == MotionEvent.TOOL_TYPE_ERASER
                ) {
                    handleStylusEvent(event)
                }
                if (event.getToolType(point) == MotionEvent.TOOL_TYPE_FINGER) {
                    handleFingerEvent(event)
                }
            }
        }

        return true
    }

    private fun handleFingerEvent(event: MotionEvent) {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
    }

    private fun handleStylusEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> moveTo(event.x, event.y, event.pressure)
            MotionEvent.ACTION_MOVE -> lineTo(event.x, event.y, event.pressure)
        }
    }

    private fun moveTo(x: Float, y: Float, pressure: Float) {
        lastPoint.set(x, y)
        val penPath = Stroke(activePaint.color)
        penPath.points.add(
            PressurePoint(
                x,
                y,
                pressure * 7
            )
        ) // TODO: This should become a setting, maybe called sensitivity
        lockBookDrawable.events.add(Event(penPath))
    }

    private fun lineTo(x: Float, y: Float, pressure: Float) {
        activePaint.strokeWidth = pressure * 7
        activePath.moveTo(
            (viewPort.width() * 2 * (lastPoint.x / tempCanvas.clipBounds.width())) + viewPort.left,
            (viewPort.height() * 2 * (lastPoint.y / tempCanvas.clipBounds.height())) + viewPort.top
        )

        activePath.lineTo(
            (viewPort.width() * 2 * (x / tempCanvas.clipBounds.width())) + viewPort.left,
            (viewPort.height() * 2 * (y / tempCanvas.clipBounds.height())) + viewPort.top
        )

        tempCanvas.drawPath(activePath, activePaint)

        activePath.reset()
        lastPoint.set(x, y)
        for (eventIndex in lockBookDrawable.events.size - 1 downTo 1) {
            val currentEvent = lockBookDrawable.events[eventIndex].stroke
            if (currentEvent is Stroke) {
                currentEvent.points.add(PressurePoint(x, y, pressure * 7))
                break
            }
        }
    }

    fun setUpBitmapDrawable() {
        val canvas = if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            holder.lockHardwareCanvas()
        } else {
            holder.lockCanvas()
        }
        canvasBitmap =
            Bitmap.createBitmap(canvas.width * 2, canvas.height * 2, Bitmap.Config.ARGB_8888)
        tempCanvas = Canvas(canvasBitmap)
        val currentPaint = Paint()
        currentPaint.color = Color.WHITE
        currentPaint.strokeWidth = 10f
        currentPaint.style = Paint.Style.STROKE
        tempCanvas.drawRect(Rect(0, 0, tempCanvas.width, tempCanvas.height), currentPaint)
        viewPort.set(canvas.clipBounds)
        holder.unlockCanvasAndPost(canvas) // The rectangle is not drawn at this moment right?
    }

    fun drawLockbookDrawable() {
        val currentPaint = Paint()
        currentPaint.isAntiAlias = true
        currentPaint.style = Paint.Style.STROKE
        currentPaint.strokeJoin = Paint.Join.ROUND
        currentPaint.strokeCap = Paint.Cap.ROUND

        for (event in lockBookDrawable.events) {
            if (event.stroke is Stroke) {
                currentPaint.color = event.stroke.color

                for (pointIndex in 0 until event.stroke.points.size) {
                    currentPaint.strokeWidth = event.stroke.points[pointIndex].pressure
                    if (pointIndex != 0) {
                        activePath.moveTo(
                            event.stroke.points[pointIndex - 1].x,
                            event.stroke.points[pointIndex - 1].y
                        )
                        activePath.lineTo(
                            event.stroke.points[pointIndex].x,
                            event.stroke.points[pointIndex].y
                        )
                        tempCanvas.drawPath(activePath, currentPaint)
                        activePath.reset()
                    }
                }

                activePath.reset()
            }
        }
    }

    fun setUpDrawing(lockbookDrawable: Drawing?) {
        setUpBitmapDrawable()
        if(lockbookDrawable != null) {
            drawLockbookDrawable()
        }
        isThreadRunning = true
    }

    override fun run() {
        while (isThreadRunning) {
            var canvas: Canvas? = null
            try {
                canvas = if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }
                drawBitmap(canvas)
            } finally { // TODO what happens to this unhandled catch?
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }
}

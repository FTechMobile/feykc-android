package ai.ftech.fekyc.common.widget.overlay

import ai.ftech.fekyc.base.extension.runOnMainThread
import ai.ftech.fekyc.R
import ai.ftech.fekyc.common.drawAt
import ai.ftech.fekyc.common.getAppColor
import ai.ftech.fekyc.common.getAppDrawable
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View

class OverlayView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : View(ctx, attrs) {

    companion object {
        private val TAG = OverlayView::class.java.simpleName
        private const val THREAD_NAME_BY_CROP_BITMAP = "THREAD_NAME_BY_CROP_BITMAP"
        private const val THREAD_NAME_BY_RESIZE_BITMAP = "THREAD_NAME_BY_RESIZE_BITMAP"
        private const val IMAGE_CROP_MAX_SIZE = 960f
        private const val SSN_CORNER = 60f
        private const val CROP_RECTANGLE_TYPE = 0
        private const val CROP_CIRCLE_TYPE = 1
        private const val LAYOUT_HEIGHT_PERCENT_RATIO = 0.6
    }

    var listener: ICallback? = null
    private var rectBackground = RectF()
    private var paintBackground = Paint(Paint.ANTI_ALIAS_FLAG)

    private var rectFrame = RectF()
    private var paintFrame = Paint(Paint.ANTI_ALIAS_FLAG)
    private var drawableFrame: Drawable? = null
//
//    private var rectFrameLiveness = RectF()
//    private var paintFrameLiveness = Paint(Paint.ANTI_ALIAS_FLAG)
//    private var drawableFrameLiveness: Drawable? = null

    private var cropType = CROP_TYPE.REACTANGLE

    private var bitmapFull: Bitmap? = null
    var imageCropMaxSize = IMAGE_CROP_MAX_SIZE
    private var executor: HandleViewPool = HandleViewPool()
    private var runnable: Runnable? = null

    init {
        init(attrs)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        initBackgroundView()
        initFrameView()
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(rectBackground, paintBackground)
        if (cropType == CROP_TYPE.CIRCLE) {
            canvas.drawRoundRect(rectFrame, getDrawableWidth() / 2f, getDrawableHeight() / 2f, paintFrame)
//            canvas.drawRoundRect(rectFrame, 0f, 0f, paintFrame)
        } else if (cropType == CROP_TYPE.REACTANGLE) {
            canvas.drawRoundRect(rectFrame, SSN_CORNER, SSN_CORNER, paintFrame)
        }
        drawableFrame?.drawAt(rectFrame, canvas)

//        val cx = rectFrame.left
//        val cy = rectFrame.top
//        canvas.drawCircle(cx, cy, 5f, paintPointTest)
    }

    fun setFrameCrop(drawable: Drawable?) {
        drawableFrame = drawable
        invalidate()
    }

    fun setCropType(type: CROP_TYPE) {
        this.cropType = type
        getDrawableFrame()
        invalidate()
    }

    fun attachFile(path: String) {
        bitmapFull = BitmapFactory.decodeFile(path)
        cropBitmap()
    }

    fun attachBitmap(bitmap: Bitmap): Bitmap {
        bitmapFull = bitmap
        return cropBitmapNew()
    }

    fun attachBitmapLarge(bitmap: Bitmap): Bitmap {
        bitmapFull = bitmap
        return cropBitmapNewLarge()
    }

    fun attachBitmapFullScreen(bitmap: Bitmap): Bitmap {
        bitmapFull = bitmap
        return cropBitmapFullScreen()
    }

    private fun cropBitmapNew(): Bitmap {
        val rotationMatrix = Matrix()
        bitmapFull!!.let {
            //xử lý ảnh bị xoay bởi camera Samsung
            if (it.width > it.height) {
                rotationMatrix.postRotate(270f)
                val ratio = it.height.toDouble() / width
                val x = ((rectFrame.top) * ratio * LAYOUT_HEIGHT_PERCENT_RATIO).toInt()
                val y = if (((rectFrame.left) * ratio).toInt() < 0) 0 else ((rectFrame.left) * ratio).toInt()
                val w = if ((rectFrame.height() * ratio).toInt() > it.width) it.width else (rectFrame.height() * ratio).toInt()
                val h = if ((rectFrame.width() * ratio).toInt() > it.height) it.height else (rectFrame.width() * ratio).toInt()
                return Bitmap.createBitmap(it, x, y, w, h, rotationMatrix, false)
            } else {
                val ratio = it.width.toDouble() / width
                val x = if (((rectFrame.left) * ratio).toInt() < 0) 0 else ((rectFrame.left) * ratio).toInt()
                val y = ((rectFrame.top) * ratio * LAYOUT_HEIGHT_PERCENT_RATIO).toInt()
                val w = if ((rectFrame.width() * ratio).toInt() > it.width) it.width else (rectFrame.width() * ratio).toInt()
                val h = if ((rectFrame.height() * ratio).toInt() > it.height) it.height else (rectFrame.height() * ratio).toInt()
                return Bitmap.createBitmap(it, x, y, w, h, rotationMatrix, false)
            }
        }
    }

    private fun cropBitmapNewLarge(): Bitmap {
        val rotationMatrix = Matrix()
        val scale = 100
        bitmapFull!!.let {
            //xử lý ảnh bị xoay bởi camera Samsung
            if (it.width > it.height) {
                rotationMatrix.postRotate(270f)
                val ratio = it.height.toDouble() / width
                val x = ((rectFrame.top) * ratio * LAYOUT_HEIGHT_PERCENT_RATIO).toInt()
                val y = if (((rectFrame.left) * ratio).toInt() < 0) 0 else ((rectFrame.left) * ratio).toInt()
                val w = if ((rectFrame.height() * ratio).toInt() > it.width) it.width else (rectFrame.height() * ratio).toInt()
                val h = if ((rectFrame.width() * ratio).toInt() > it.height) it.height else (rectFrame.width() * ratio).toInt()
                return Bitmap.createBitmap(it, if (x - scale < 0) 0 else x - scale, if (y - scale < 0) 0 else y - scale, w + scale, h + scale, rotationMatrix, false)
            } else {
                val ratio = it.width.toDouble() / width
                val x = if (((rectFrame.left) * ratio).toInt() < 0) 0 else ((rectFrame.left) * ratio).toInt()
                val y = ((rectFrame.top) * ratio * LAYOUT_HEIGHT_PERCENT_RATIO).toInt()
                val w = if ((rectFrame.width() * ratio).toInt() > it.width) it.width else (rectFrame.width() * ratio).toInt()
                val h = if ((rectFrame.height() * ratio).toInt() > it.height) it.height else (rectFrame.height() * ratio).toInt()
                return Bitmap.createBitmap(it, if (x - scale < 0) 0 else x - scale, if (y - scale < 0) 0 else y - scale, w + scale, h + scale, rotationMatrix, false)
            }
        }
    }

    private fun cropBitmapFullScreen(): Bitmap {
        val rotationMatrix = Matrix()
        val scale = 100
        bitmapFull!!.let {
            //xử lý ảnh bị xoay bởi camera Samsung
            if (it.width > it.height) {
                rotationMatrix.postRotate(270f)
                return Bitmap.createBitmap(it, 0, 0, it.width, it.height, rotationMatrix, false)
            } else {
                return Bitmap.createBitmap(it, 0, 0, it.width, it.height, rotationMatrix, false)
            }
        }
    }

    private fun cropBitmap() {
        val rotationMatrix = Matrix()
        bitmapFull?.let {
            //xử lý ảnh bị xoay bởi camera Samsung
            if (it.width > it.height) {
                rotationMatrix.postRotate(90f)

                val ratio = it.height.toDouble() / width

                val x = ((rectFrame.top) * ratio * LAYOUT_HEIGHT_PERCENT_RATIO).toInt()
                val y = ((rectFrame.left) * ratio).toInt()

                val w = (rectFrame.height() * ratio).toInt()
                val h = (rectFrame.width() * ratio).toInt()

//                Log.d(TAG, "cropBitmap:  ratio: $ratio x: $x  y: $y   w: $w   h: $h     bitmap[${it.width}   ${it.height}]    rectBackground[${rectBackground.width()}   ${rectBackground.height()}]")

                runnable = HandleViewTask(
                    name = THREAD_NAME_BY_CROP_BITMAP,
                    task = {
                        Bitmap.createBitmap(it, x, y, w, h, rotationMatrix, false)
                    },
                    callback = object : HandleViewTask.ICallback {
                        override fun onFinish(bitmap: Bitmap) {
                            executor.remove(runnable)
                            runnable = null
                            resizeBitmap(bitmap)
                        }

                        override fun onError(exception: Exception) {
                            runOnMainThread({
                                listener?.onError(exception)
                            })
                        }
                    }
                )

                executor.execute(runnable)
            } else {
//                rotationMatrix.postRotate(0f)

                val ratio = it.width.toDouble() / width

                val x = ((rectFrame.left) * ratio).toInt()
                val y = ((rectFrame.top) * ratio * LAYOUT_HEIGHT_PERCENT_RATIO).toInt()

                val w = (rectFrame.width() * ratio).toInt()
                val h = (rectFrame.height() * ratio).toInt()

//                Log.d(TAG, "cropBitmap:  ratio: $ratio x: $x  y: $y   w: $w   h: $h     bitmap[${it.width}   ${it.height}]    rectBackground[${rectBackground.width()}   ${rectBackground.height()}]")

                runnable = HandleViewTask(
                    name = THREAD_NAME_BY_CROP_BITMAP,
                    task = {
                        Bitmap.createBitmap(it, x, y, w, h, rotationMatrix, false)
                    },
                    callback = object : HandleViewTask.ICallback {
                        override fun onFinish(bitmap: Bitmap) {
                            executor.remove(runnable)
                            runnable = null
                            resizeBitmap(bitmap)
                        }

                        override fun onError(exception: Exception) {
                            runOnMainThread({
                                listener?.onError(exception)
                            })
                        }
                    }
                )

                executor.execute(runnable)
            }
        }
    }

    private fun resizeBitmap(bitmap: Bitmap) {
        runnable = HandleViewTask(
            name = THREAD_NAME_BY_RESIZE_BITMAP,
            task = {
                val matrix = Matrix()
                val oldRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
                val newRect = RectF(0f, 0f, imageCropMaxSize, imageCropMaxSize)
                matrix.setRectToRect(oldRect, newRect, Matrix.ScaleToFit.CENTER)
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            },
            callback = object : HandleViewTask.ICallback {
                override fun onFinish(bitmap: Bitmap) {
                    executor.remove(runnable)
                    runnable = null
                    listener?.onTakePicture(bitmap)
                }

                override fun onError(exception: Exception) {
                    runOnMainThread({
                        listener?.onError(exception)
                    })
                }
            })
        executor.execute(runnable)
    }

    private fun getDrawableWidth(): Float {
        return drawableFrame?.intrinsicWidth?.toFloat() ?: 0f
    }

    private fun getDrawableHeight(): Float {
        return drawableFrame?.intrinsicHeight?.toFloat() ?: 0f
    }

    private fun initBackgroundView() {
        paintBackground.apply {
            color = getAppColor(R.color.fekyc_color_black_80)
        }
        rectBackground.set(0f, 0f, width.toFloat(), height.toFloat())
    }

    private fun initFrameView() {
        paintFrame.apply {
            color = Color.TRANSPARENT
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
        }

        val centerX = rectBackground.centerX()
        val centerY = rectBackground.centerY()

        val halfOffsetWidth = getDrawableWidth() / 2f
        val halfOffsetHeight = getDrawableHeight() / 2f


        val left = centerX - halfOffsetWidth
        val top = centerY - halfOffsetHeight
        val right = centerX + halfOffsetWidth
        val bottom = centerY + halfOffsetHeight

        rectFrame.set(left, top, right, bottom)
    }

    private fun getDrawableFrame() {
        if (cropType == CROP_TYPE.REACTANGLE) {
            drawableFrame = getAppDrawable(R.drawable.fekyc_ic_photo_rect_crop)
        } else if (cropType == CROP_TYPE.CIRCLE) {
            drawableFrame = getAppDrawable(R.drawable.fekyc_ic_photo_circle_white_crop)
        }
    }

    private fun init(attrs: AttributeSet?) {
        val ta = context.theme.obtainStyledAttributes(attrs, R.styleable.OverlayView, 0, 0)


        val type = ta.getInt(R.styleable.OverlayView_ov_frame_type, CROP_RECTANGLE_TYPE)
        cropType = CROP_TYPE.valueOfName(type) ?: CROP_TYPE.REACTANGLE

        getDrawableFrame()

        ta.recycle()
    }


    interface ICallback {
        fun onTakePicture(bitmap: Bitmap)
        fun onError(exception: Exception)
    }

    enum class CROP_TYPE(val value: Int) {
        REACTANGLE(0),
        CIRCLE(1);

        companion object {
            fun valueOfName(value: Int): CROP_TYPE? {
                val item = values().find {
                    it.value == value
                }

                return if (item != null) {
                    item
                } else {
                    Log.e("CROP_TYPE", "can not find any CROP_TYPE for name: $value")
                    null
                }
            }
        }
    }
}

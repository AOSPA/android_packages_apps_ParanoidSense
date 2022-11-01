package co.aospa.sense.controller

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.Process
import android.util.Log
import android.view.SurfaceHolder
import android.view.WindowManager
import co.aospa.sense.SenseApp.Companion.app
import co.aospa.sense.camera.CameraService
import co.aospa.sense.camera.CameraUtil
import co.aospa.sense.camera.listeners.*
import co.aospa.sense.util.Util
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.ArrayList

open class FaceEnrollController private constructor(private val mContext: Context?) {

    private val mCameraCallbacks = ArrayList<CameraCallback>()
    private val mHandler = Handler(Looper.getMainLooper(), HandlerCallback())
    private val mCameraListener: CameraListener = object :
        CameraListener {
        override fun onComplete(unused: Any?) {
            mHandler.sendEmptyMessage(MSG_CAMERA_UPDATE_STATE)
        }

        override fun onError(exc: Exception?) {
            mHandler.sendEmptyMessage(MSG_CAMERA_ERROR)
        }
    }

    private val mCameraId: Int = CameraUtil.getCameraId(mContext)
    private var mErrorCallbackListener =
        object : CameraEventListener {
            override fun onEventCallback(data: Int, value: Any?) {
                mHandler.sendEmptyMessage(
                    MSG_CAMERA_ERROR
                )
            }
        }

    private var mCameraParams: Camera.Parameters? = null
    private val mReadParamListener =
        object : CameraEventListener {
            override fun onEventCallback(data: Int, value: Any?) {
                mCameraParams = value as Camera.Parameters
            }
        }
    private var mCameraState = CameraState.CAMERA_IDLE
    private var mFaceUnlockHandler: Handler? = null
    private var mFrame: ByteBuffer? = null
    private var mHandling = false
    private val mByteBufferListener =
        object : CameraEventListener {
            override fun onEventCallback(data: Int, value: Any?) {
                if (Util.IS_DEBUG_LOGGING) Log.d(TAG, "Camera frame arrival $mHandling")
                if (!mHandling) {
                    mHandling = true
                    val obtain = Message.obtain(mFaceUnlockHandler, MSG_HANDLE_FACE_DATA)
                    obtain.obj = value
                    mFaceUnlockHandler!!.sendMessage(obtain)
                }
            }
        }
    private var mHolder: SurfaceHolder? = null
    private var mPreviewSize: Camera.Size? = null
    private var mPreviewStarted = false
    private var mStop = false
    private var mSurfaceCreated = false
    fun setSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        if (surfaceHolder == null) {
            synchronized(mCameraState) {
                if (mCameraState == CameraState.CAMERA_PREVIEW_STARTED) {
                    CameraService.clearQueue()
                    mCameraState = CameraState.CAMERA_PREVIEW_STOPPING
                    CameraService.setFaceDetectionCallback(null, null)
                    CameraService.stopPreview(null)
                    CameraService.setPreviewCallback(null, false, null)
                    CameraService.closeCamera(null)
                }
            }
        }
        mHolder = surfaceHolder
        if (surfaceHolder != null) {
            surfaceHolder.setKeepScreenOn(true)
            mHolder!!.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceChanged(
                    surfaceHolder: SurfaceHolder,
                    i: Int,
                    i2: Int,
                    i3: Int
                ) {
                }

                override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {}
                override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                    mSurfaceCreated = true
                    mHandler.sendEmptyMessage(MSG_CAMERA_SURFACE_CREATED)
                }
            })
            if (mHolder!!.surface != null) {
                mSurfaceCreated = true
            }
        }
    }

    fun start(cameraCallback: CameraCallback, i: Int) {
        Log.i(TAG, "new start : $cameraCallback")
        synchronized(mCameraCallbacks) {
            if (mCameraCallbacks.contains(cameraCallback)) {
                return
            }
            mCameraCallbacks.add(cameraCallback)
        }
        synchronized(mCameraState) {
            if (mCameraState == CameraState.CAMERA_IDLE) {
                if (!mHandler.hasMessages(MSG_CAMERA_UPDATE_STATE)) {
                    mHandler.sendEmptyMessage(MSG_CAMERA_UPDATE_STATE)
                }
                mHandling = false
            }
        }
        if (i > 0) {
            mHandler.postDelayed({
                synchronized(mCameraState) {
                    if (mCameraCallbacks.contains(cameraCallback)) {
                        cameraCallback.onTimeout()
                    }
                }
            }, i.toLong())
        }
        mStop = false
    }

    fun stop(cameraCallback: CameraCallback) {
        Log.i(TAG, "stop : $cameraCallback")
        synchronized(mCameraCallbacks) {
            if (!mCameraCallbacks.contains(cameraCallback)) {
                Log.e(TAG, "callback has been released!")
                return
            }
            mCameraCallbacks.remove(cameraCallback)
            if (mCameraCallbacks.size > 0) {
                return
            }
        }
        mStop = true
        val handler = mFaceUnlockHandler
        if (handler != null) {
            handler.removeMessages(MSG_HANDLE_FACE_DATA)
            mFaceUnlockHandler!!.removeMessages(MSG_FACE_UNLOCK_DETECT_AREA)
        }
        synchronized(mCameraState) {
            CameraService.clearQueue()
            if (mCameraState == CameraState.CAMERA_PREVIEW_STARTED) {
                mCameraState = CameraState.CAMERA_PREVIEW_STOPPING
                CameraService.setFaceDetectionCallback(null, null)
                CameraService.stopPreview(null)
                CameraService.setPreviewCallback(null, false, null)
                CameraService.closeCamera(null)
            } else if (mCameraState != CameraState.CAMERA_IDLE) {
                CameraService.setPreviewCallback(null, false, null)
                CameraService.closeCamera(null)
            }
        }
        mHolder = null
        mCameraState = CameraState.CAMERA_IDLE
        mCameraParams = null
        mPreviewSize = null
        mHandling = false
    }

    private fun handleCameraStateUpdate() {
        if (!mStop) {
            synchronized(mCameraState) {
                when (CameraStateOrdinal.STATE[mCameraState.ordinal]) {
                    0 -> if (mCameraId != -1) {
                        CameraService.openCamera(mCameraId, mErrorCallbackListener, mCameraListener)
                        mCameraState = CameraState.CAMERA_OPENED
                    } else {
                        Log.d(TAG, "No front camera, stop face unlock")
                        mHandler.sendEmptyMessage(MSG_CAMERA_ERROR)
                        return
                    }
                    1 -> {
                        mCameraState = CameraState.CAMERA_PARAM_READ
                        CameraService.readParameters(mReadParamListener, mCameraListener)
                    }
                    2 -> {
                        mCameraState = CameraState.CAMERA_PARAM_SET
                        mPreviewSize = CameraUtil.getBestPreviewSize(mCameraParams, 480, 640)
                        val width = mPreviewSize!!.width
                        val height = mPreviewSize!!.height
                        mCameraParams!!.setPreviewSize(width, height)
                        mCameraParams!!.previewFormat = ImageFormat.NV21
                        mFrame = ByteBuffer.allocateDirect(getPreviewBufferSize(width, height))
                        CameraService.writeParameters(mCameraListener)
                        Log.d(TAG, "preview size " + mPreviewSize!!.height + " " + mPreviewSize!!.width)
                    }
                    3 -> {
                        mCameraState = CameraState.CAMERA_PREVIEW_STARTED
                        CameraService.addCallbackBuffer(mFrame!!.array(), null)
                        CameraService.setDisplayOrientationCallback(cameraAngle, null)
                        CameraService.setPreviewCallback(mByteBufferListener, true, null)
                        if (mHolder != null) {
                            if (mSurfaceCreated) {
                                CameraService.startPreview(mHolder, mCameraListener)
                                mPreviewStarted = true
                            } else {
                                return
                            }
                        } else {
                            val surfaceTexture = SurfaceTexture(10)
                            CameraService.startPreview(surfaceTexture, mCameraListener)
                        }
                    }
                    4 -> CameraService.setFaceDetectionCallback({ faceArr: Array<Camera.Face?>, _: Camera? ->
                        if (faceArr.isNotEmpty()) {
                            for (mCameraCallback in mCameraCallbacks) {
                                mCameraCallback.onFaceDetected()
                            }
                        }
                    }, null)
                    5 -> {
                        CameraService.setPreviewCallback(null, false, null)
                        CameraService.closeCamera(null)
                    }
                    else -> { return }
                }
            }
        }
    }

    private fun initWorkHandler() {
        if (mFaceUnlockThread == null) {
            mFaceUnlockThread = HandlerThread("Camera Face unlock")
            mFaceUnlockThread!!.priority = Process.THREAD_PRIORITY_BACKGROUND
            mFaceUnlockThread!!.start()
        }
        mFaceUnlockHandler = FaceHandler(
            mFaceUnlockThread!!.looper
        )
    }

    private fun getPreviewBufferSize(i: Int, i2: Int): Int {
        return i2 * i * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8 + 32
    }

    private val cameraAngle: Int
        get() {
            val cameraInfo = Camera.CameraInfo()
            Camera.getCameraInfo(mCameraId, cameraInfo)
            val rotation =
                mContext!!.getSystemService(WindowManager::class.java).defaultDisplay.rotation
            var orientation = 0
            if (rotation != 0) {
                when (rotation) {
                    1 -> {
                        orientation = 90
                    }
                    2 -> {
                        orientation = 180
                    }
                    3 -> {
                        orientation = 270
                    }
                }
            }
            return if (cameraInfo.facing == 1) {
                (360 - (cameraInfo.orientation + orientation) % 360) % 360
            } else (cameraInfo.orientation - orientation + 360) % 360
        }

    private enum class CameraState {
        CAMERA_IDLE, CAMERA_OPENED, CAMERA_PARAM_READ, CAMERA_PARAM_SET, CAMERA_PREVIEW_STARTED, CAMERA_PREVIEW_STOPPING
    }

    interface CameraCallback {
        fun handleSaveFeature(data: ByteArray?, width: Int, height: Int, angle: Int): Int
        fun handleSaveFeatureResult(result: Int)
        fun onCameraError()
        fun onFaceDetected()
        fun onTimeout()
        fun setDetectArea(size: Camera.Size?)
    }

    internal object CameraStateOrdinal {
        val STATE = IntArray(CameraState.values().size)

        init {
            STATE[CameraState.CAMERA_OPENED.ordinal] = 1
            STATE[CameraState.CAMERA_PARAM_READ.ordinal] = 2
            STATE[CameraState.CAMERA_PARAM_SET.ordinal] = 3
            try {
                STATE[CameraState.CAMERA_PREVIEW_STOPPING.ordinal] = 4
            } catch (ignored: NoSuchFieldError) {
            }
        }
    }

    internal inner class HandlerCallback : Handler.Callback {
        override fun handleMessage(message: Message): Boolean {
            if (Util.IS_DEBUG_LOGGING) {
                Log.i(TAG, "handleMessage : $message")
            }
            when (message.what) {
                MSG_CAMERA_ERROR -> for (mCameraCallback in mCameraCallbacks) {
                    mCameraCallback.onCameraError()
                }
                MSG_CAMERA_UPDATE_STATE -> handleCameraStateUpdate()
                MSG_CAMERA_SURFACE_CREATED -> if (CameraState.CAMERA_PREVIEW_STARTED == mCameraState && !mPreviewStarted) {
                    CameraService.startPreview(mHolder, mCameraListener)
                }
            }
            return true
        }
    }

    private inner class FaceHandler(looper: Looper?) : Handler(
        looper!!
    ) {
        override fun handleMessage(message: Message) {
            if (!mStop) {
                if (Util.IS_DEBUG_LOGGING) {
                    Log.i(TAG, "FaceHandler handle msg : $message")
                }
                val i = message.what
                if (i == MSG_HANDLE_FACE_DATA) {
                    synchronized(mCameraCallbacks) {
                        val byteBuffer = message.obj as ByteBuffer
                        var result = -1
                        for (mCameraCallback in mCameraCallbacks) {
                            val featureResult = mCameraCallback.handleSaveFeature(
                                byteBuffer.array(),
                                mPreviewSize!!.width,
                                mPreviewSize!!.height,
                                0
                            )
                            if (featureResult != -1) {
                                result = featureResult
                            }
                        }
                        for (mCameraCallback in mCameraCallbacks) {
                            mCameraCallback.handleSaveFeatureResult(result)
                        }
                        if (mFrame != null) {
                            CameraService.addCallbackBuffer(mFrame!!.array(), null)
                            mHandling = false
                        }
                    }
                } else if (i == MSG_FACE_UNLOCK_DETECT_AREA) {
                    for (mCameraCallback in mCameraCallbacks) {
                        mCameraCallback.setDetectArea(mPreviewSize)
                    }
                }
            }
        }
    }

    companion object {
        private const val MSG_CAMERA_ERROR = 101
        private const val MSG_CAMERA_UPDATE_STATE = 102
        private const val MSG_CAMERA_SURFACE_CREATED = 103
        private const val MSG_HANDLE_FACE_DATA = 1003
        private const val MSG_FACE_UNLOCK_DETECT_AREA = 1004
        private val TAG = FaceEnrollController::class.java.simpleName
        private var mFaceUnlockThread: HandlerThread? = null

        @SuppressLint("StaticFieldLeak")
        private var sInstance: FaceEnrollController? = null
        val instance: FaceEnrollController?
            get() {
                if (sInstance == null) {
                    sInstance = FaceEnrollController(app)
                }
                return sInstance
            }
    }

    init {
        initWorkHandler()
    }
}
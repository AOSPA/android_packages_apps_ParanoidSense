package co.aospa.sense.camera

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
import co.aospa.sense.camera.listeners.CameraListener
import co.aospa.sense.camera.listeners.ErrorCallbackListener
import co.aospa.sense.camera.listeners.ReadParametersListener
import co.aospa.sense.camera.listeners.ByteBufferCallbackListener
import co.aospa.sense.util.Constants.MSG_UNLOCK_FACE_NOT_FOUND
import co.aospa.sense.util.Util
import java.lang.Exception
import java.nio.ByteBuffer

open class CameraFaceAuthController(
    private val mContext: Context,
    private var mCallback: ServiceCallback?
) {
    private val mHandler: Handler? = Handler(Looper.getMainLooper(), HandlerCallback())
    private val mCameraListener: CameraListener = object : CameraListener {
        override fun onComplete(unused: Any) {
            mHandler!!.sendEmptyMessage(CAM_MSG_STATE_UPDATE)
        }

        override fun onError(exc: Exception) {
            mHandler!!.sendEmptyMessage(CAM_MSG_ERROR)
        }
    }
    private val mCameraId: Int = CameraUtil.getCameraId(mContext)
    private var mErrorCallbackListener = ErrorCallbackListener { _: Int, _: Any? ->
        mHandler!!.sendEmptyMessage(
            CAM_MSG_ERROR
        )
    }
    private var mCameraParam: Camera.Parameters? = null
    private val mReadParamListener =
        ReadParametersListener { _, parameters -> mCameraParam = parameters as Camera.Parameters }
    private var mCameraState = CameraState.CAMERA_IDLE
    private var mCompareSuccess = false
    private var mComparing = false
    private var mFaceUnlockHandler: Handler? = null
    private val mByteBufferListener = ByteBufferCallbackListener { i: Int, byteBuffer: Any? ->
        if (!mComparing) {
            mComparing = true
            if (Util.IS_DEBUG_LOGGING) {
                Log.d(TAG, "Camera frame arrival")
            }
            val obtain = Message.obtain(mFaceUnlockHandler, MSG_FACE_UNLOCK_COMPARE)
            obtain.obj = byteBuffer
            mFaceUnlockHandler!!.sendMessage(obtain)
        }
    }
    private var mFrame: ByteBuffer? = null
    private var mPreviewSize: Camera.Size? = null
    private var mStop = false
    private var mTexture: SurfaceTexture? = null
    fun start() {
        Log.i(TAG, "start enter")
        mHandler!!.sendEmptyMessageDelayed(CAM_MSG_OPEN, 0)
    }

    private fun handleCameraOpen() {
        Log.i(TAG, "start enter")
        if (mCameraId == -1) {
            Log.d(TAG, "No front camera, stop face unlock")
            return
        }
        initWorkHandler()
        CameraService.openCamera(mCameraId, mErrorCallbackListener, mCameraListener)
        mCameraState = CameraState.CAMERA_OPENED
        resetTimeout(0)
        mStop = false
        Log.i(TAG, "start exit")
    }

    private fun stopSelf() {
        mHandler!!.post { stop() }
    }

    fun stop() {
        Log.i(TAG, "stop enter")
        if (mFaceUnlockHandler != null) {
            mFaceUnlockHandler!!.removeMessages(MSG_FACE_UNLOCK_COMPARE)
            mFaceUnlockHandler!!.removeMessages(MSG_FACE_UNLOCK_DETECT_AREA)
        }
        if (mHandler != null) {
            mHandler.removeMessages(CAM_MSG_OPEN)
            mHandler.removeMessages(MSG_TIME_OUT_NO_FACE)
            mHandler.removeMessages(MSG_TIME_OUT_WITH_FACE)
        }
        CameraService.clearQueue()
        if (mCameraState == CameraState.CAMERA_PREVIEW_STARTED) {
            CameraService.addCallbackBuffer(null, null)
            mFrame = null
            mCameraState = CameraState.CAMERA_PREVIEW_STOPPING
            CameraService.stopPreview(null)
            CameraService.closeCamera(null)
        } else if (mCameraState != CameraState.CAMERA_IDLE) {
            CameraService.closeCamera(null)
        }
        mCallback = null
        mStop = true
        if (!mCompareSuccess) {
            mCompareSuccess = false
        }
        Log.i(TAG, "stop exit")
    }

    private fun resetTimeout(i: Int) {
        mHandler!!.removeMessages(MSG_TIME_OUT_NO_FACE)
        mHandler.removeMessages(MSG_TIME_OUT_WITH_FACE)
        mHandler.sendEmptyMessageDelayed(
            MSG_TIME_OUT_NO_FACE,
            (if (i > 0) i.toLong() else MATCH_TIME_OUT_NO_FACE_MS) as Long
        )
        mHandler.sendEmptyMessageDelayed(
            MSG_TIME_OUT_WITH_FACE,
            (if (i > 0) i.toLong() else MATCH_TIME_OUT_WITH_FACE_MS) as Long
        )
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

    private fun handleCameraStateUpdate() {
        if (!mStop) {
            when (CameraStateOrdinal.STATE[mCameraState.ordinal]) {
                1 -> {
                    mCameraState = CameraState.CAMERA_PARAM_READ
                    CameraService.readParameters(mReadParamListener, mCameraListener)
                }
                2 -> {
                    mCameraState = CameraState.CAMERA_PARAM_SET
                    mPreviewSize = CameraUtil.getBestPreviewSize(mCameraParam, 480, 640)
                    val width = mPreviewSize!!.width
                    val height = mPreviewSize!!.height
                    mCameraParam!!.setPreviewSize(width, height)
                    mCameraParam!!.previewFormat = ImageFormat.NV21
                    mFrame = ByteBuffer.allocateDirect(getPreviewBufferSize(width, height))
                    CameraService.writeParameters(mCameraListener)
                    Log.d(TAG, "preview size " + mPreviewSize!!.height + " " + mPreviewSize!!.width)
                    mFaceUnlockHandler!!.sendEmptyMessage(MSG_FACE_UNLOCK_DETECT_AREA)
                }
                3 -> {
                    mCameraState = CameraState.CAMERA_PREVIEW_STARTED
                    if (mTexture == null) {
                        mTexture = SurfaceTexture(10)
                    }
                    CameraService.addCallbackBuffer(mFrame!!.array(), null)
                    CameraService.setPreviewCallback(mByteBufferListener, true, null)
                    CameraService.startPreview(mTexture, mCameraListener)
                }
                4 -> {
                    CameraService.closeCamera(null)
                }
            }
        }
    }

    private fun getPreviewBufferSize(width: Int, height: Int): Int {
        return height * width * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8 + 32
    }

    private enum class CameraState {
        CAMERA_IDLE, CAMERA_OPENED, CAMERA_PARAM_READ, CAMERA_PARAM_SET, CAMERA_PREVIEW_STARTED, CAMERA_PREVIEW_STOPPING
    }

    interface ServiceCallback {
        fun handlePreviewData(data: ByteArray?, width: Int, height: Int): Int
        fun onCameraError()
        fun onTimeout(shouldTimeout: Boolean)
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
            if (message.what == MSG_TIME_OUT_NO_FACE || message.what == MSG_TIME_OUT_WITH_FACE) {
                var shouldTimeout = false
                if (Util.IS_DEBUG_LOGGING) {
                    Log.d(TAG, "timeout, sendBroadcast faceId stop")
                }
                stopSelf()
                if (mCallback != null) {
                    if (message.what == MSG_TIME_OUT_WITH_FACE) {
                        shouldTimeout = true
                    }
                    mCallback!!.onTimeout(shouldTimeout)
                }
            } else if (message.what == CAM_MSG_ERROR) {
                stopSelf()
                if (mCallback != null) {
                    mCallback!!.onCameraError()
                }
            } else if (message.what == CAM_MSG_STATE_UPDATE) {
                handleCameraStateUpdate()
            } else if (message.what == CAM_MSG_OPEN) {
                handleCameraOpen()
            }
            return true
        }
    }

    private inner class FaceHandler(looper: Looper?) : Handler(
        looper!!
    ) {
        override fun handleMessage(message: Message) {
            if (Util.IS_DEBUG_LOGGING) {
                Log.i(TAG, "FaceHandler handle msg : $message")
            }
            if (message.what == MSG_FACE_UNLOCK_COMPARE) {
                val byteBuffer = message.obj as ByteBuffer
                var result = 0
                if (mCallback != null) {
                    result = mCallback!!.handlePreviewData(
                        byteBuffer.array(),
                        mPreviewSize!!.width,
                        mPreviewSize!!.height
                    )
                }
                if (result == 0) {
                    mCompareSuccess = true
                    return
                }
                if (result != MSG_UNLOCK_FACE_NOT_FOUND) {
                    mHandler!!.removeMessages(MSG_TIME_OUT_NO_FACE)
                }
                if (mFrame != null) {
                    CameraService.addCallbackBuffer(mFrame!!.array(), null)
                    mComparing = false
                }
            } else if (message.what == MSG_FACE_UNLOCK_DETECT_AREA && mCallback != null) {
                mCallback!!.setDetectArea(mPreviewSize)
            }
        }
    }

    companion object {
        private const val CAM_MSG_ERROR = 101
        private const val CAM_MSG_STATE_UPDATE = 102
        private const val CAM_MSG_OPEN = 103
        private const val MATCH_TIME_OUT_NO_FACE_MS = 3000
        private const val MATCH_TIME_OUT_WITH_FACE_MS = 4800
        private const val MSG_FACE_UNLOCK_COMPARE = 1003
        private const val MSG_FACE_UNLOCK_DETECT_AREA = 1004
        private const val MSG_TIME_OUT_NO_FACE = 1
        private const val MSG_TIME_OUT_WITH_FACE = 2
        private val TAG = CameraFaceAuthController::class.java.simpleName
        private var mFaceUnlockThread: HandlerThread? = null
    }

}
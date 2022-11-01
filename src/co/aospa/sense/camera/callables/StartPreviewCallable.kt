package co.aospa.sense.camera.callables

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import co.aospa.sense.camera.listeners.CameraListener
import co.aospa.sense.util.Util
import java.io.IOException
import java.lang.Exception
import java.lang.RuntimeException

class StartPreviewCallable : CameraCallable {
    private val mSurfaceHolder: SurfaceHolder?
    private val mSurfaceTexture: SurfaceTexture?

    constructor(surfaceTexture: SurfaceTexture?, listener: CameraListener?) : super(listener) {
        mSurfaceTexture = surfaceTexture
        mSurfaceHolder = null
    }

    constructor(surfaceHolder: SurfaceHolder?, listener: CameraListener?) : super(listener) {
        mSurfaceTexture = null
        mSurfaceHolder = surfaceHolder
    }

    override fun call(): CallableReturn {
        val camera = cameraData.mCamera
            ?: return CallableReturn(Exception("Camera isn't opened"))
        return try {
            if (mSurfaceTexture != null) {
                camera.setPreviewTexture(mSurfaceTexture)
            } else if (mSurfaceHolder != null) {
                camera.setPreviewDisplay(mSurfaceHolder)
            }
            try {
                startPreview(camera)
                CallableReturn(null)
            } catch (e: RuntimeException) {
                CallableReturn(e)
            }
        } catch (e: IOException) {
            if (Util.IS_DEBUG_LOGGING) Log.e(tag, "setPreviewDisplay failed.")
            CallableReturn(e)
        }
    }

    override fun callback(callableReturn: CallableReturn) {
        val listener: CameraListener? = mCameraListener.get()
        if (callableReturn.exception != null
        ) {
            listener!!.onError(callableReturn.exception)
        }
    }

    override val tag: String?
        get() = StartPreviewCallable::class.java.simpleName

    companion object {
        private fun startPreview(camera: Camera) {
            camera.startPreview()
        }
    }
}
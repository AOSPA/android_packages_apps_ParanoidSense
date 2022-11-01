package co.aospa.sense.camera.callables

import android.hardware.Camera
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import co.aospa.sense.camera.CameraHandlerThread
import co.aospa.sense.camera.CameraHandlerThread.CameraData
import co.aospa.sense.camera.listeners.CameraListener
import co.aospa.sense.util.Util
import java.lang.ref.WeakReference


abstract class CameraCallable(cameraListener: CameraListener?) {

    @JvmField
    protected val mCameraListener: WeakReference<CameraListener>
    private var mStartTime: Long = 0
    abstract fun call(): CallableReturn
    abstract val tag: String?
    val cameraData: CameraData
        get() = (Thread.currentThread() as CameraHandlerThread).cameraData

    open fun getCameraParameters(): Camera.Parameters? {
        return (Thread.currentThread() as CameraHandlerThread).cameraData.mParameters
    }

    fun run() {
        if (Util.IS_DEBUG_LOGGING) Log.d(tag, "Begin")
        mStartTime = SystemClock.elapsedRealtime()
        val call = call()
        if (Util.IS_DEBUG_LOGGING) {
            val tag = tag
            Log.d(tag, "End (dur:" + (SystemClock.elapsedRealtime() - mStartTime) + ")")
        }
        runOnUiThread { callback(call) }
    }

    open fun callback(callableReturn: CallableReturn) {
        val elapsedRealtime = SystemClock.elapsedRealtime() - mStartTime
        val tag = tag
        val listener = mCameraListener.get()
        if (callableReturn.exception != null) {
            if (Util.IS_DEBUG_LOGGING) Log.w(tag, "Exception in result (dur:$elapsedRealtime)", callableReturn.exception)
            if (listener != null) {
                listener.onError(callableReturn.exception)
                return
            }
            return
        }
        if (Util.IS_DEBUG_LOGGING) Log.d(tag, "Result success (dur:$elapsedRealtime)")
        listener?.onComplete(callableReturn.value)
    }

    companion object {
        protected fun runOnUiThread(runnable: Runnable?) {
            val handler = Handler(Looper.getMainLooper())
            handler.post(runnable!!)
        }
    }

    init {
        mCameraListener = WeakReference(cameraListener)
    }
}
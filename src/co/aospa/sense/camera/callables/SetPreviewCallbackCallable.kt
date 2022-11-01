package co.aospa.sense.camera.callables

import android.hardware.Camera
import co.aospa.sense.camera.listeners.CameraEventListener
import co.aospa.sense.camera.listeners.CameraListener
import java.lang.Exception
import java.lang.ref.WeakReference
import java.nio.ByteBuffer

class SetPreviewCallbackCallable(
    eventListener: CameraEventListener?,
    private val mWithBuffer: Boolean,
    listener: CameraListener?
) : CameraCallable(listener) {

    private val mPreviewCallbackListener: WeakReference<CameraEventListener> = WeakReference(eventListener)
    private val mPreviewCallback = Camera.PreviewCallback { data: ByteArray?, _: Camera? ->
        val listener = mPreviewCallbackListener.get()
        listener?.onEventCallback(0, data?.let { ByteBuffer.wrap(it) })
    }

    override fun call(): CallableReturn {
        val camera = cameraData.mCamera
            ?: return CallableReturn(Exception("Camera isn't opened"))
        if (mWithBuffer) {
            camera.setPreviewCallbackWithBuffer(mPreviewCallback)
        } else {
            camera.setPreviewCallback(mPreviewCallback)
        }
        return CallableReturn(null)
    }

    override val tag: String?
        get() = SetPreviewCallbackCallable::class.java.simpleName
}
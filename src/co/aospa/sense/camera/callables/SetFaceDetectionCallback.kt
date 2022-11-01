package co.aospa.sense.camera.callables

import android.hardware.Camera
import co.aospa.sense.camera.listeners.CameraListener
import java.lang.Exception

class SetFaceDetectionCallback(
    var mListener: Camera.FaceDetectionListener?,
    listener: CameraListener?
) : CameraCallable(listener) {

    override fun call(): CallableReturn {
        val camera = cameraData.mCamera
            ?: return CallableReturn(Exception("Camera isn't opened"))
        camera.setFaceDetectionListener(mListener)
        if (mListener != null) {
            camera.startFaceDetection()
        } else {
            camera.stopFaceDetection()
        }
        return CallableReturn(null)
    }

    override val tag: String?
        get() = SetFaceDetectionCallback::class.java.simpleName
}
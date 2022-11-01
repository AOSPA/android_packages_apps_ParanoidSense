package co.aospa.sense.camera.callables

import co.aospa.sense.camera.listeners.CameraEventListener
import co.aospa.sense.camera.listeners.CameraListener
import java.lang.Exception

class ReadParamsCallable(var mReadListener: CameraEventListener?, listener: CameraListener?) :
    CameraCallable(
        listener!!
    ) {

    override fun call(): CallableReturn {
        val camera = cameraData.mCamera
            ?: return CallableReturn(Exception("Camera isn't opened"))
        return try {
            cameraData.mParameters = camera.parameters
            mReadListener!!.onEventCallback(0, cameraData.mParameters)
            CallableReturn(null)
        } catch (e: Exception) {
            CallableReturn(e)
        }
    }

    override val tag: String?
        get() = ReadParamsCallable::class.java.simpleName
}
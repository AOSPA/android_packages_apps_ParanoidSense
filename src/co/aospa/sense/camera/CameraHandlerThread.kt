package co.aospa.sense.camera

import android.hardware.Camera
import android.os.HandlerThread

class CameraHandlerThread : HandlerThread(TAG, -2) {

    val cameraData = CameraData()

    class CameraData constructor() {
        @JvmField
        var mCamera: Camera? = null
        @JvmField
        var mCameraId: Int = -1

        @JvmField
        var mParameters: Camera.Parameters? = null
    }

    companion object {
        val TAG: String = CameraHandlerThread::class.java.simpleName
    }
}
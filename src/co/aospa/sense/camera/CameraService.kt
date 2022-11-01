package co.aospa.sense.camera

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Handler
import android.os.Message
import android.view.SurfaceHolder
import co.aospa.sense.camera.callables.CameraCallable
import co.aospa.sense.camera.listeners.CameraEventListener
import co.aospa.sense.camera.listeners.CameraListener
import co.aospa.sense.camera.callables.OpenCameraCallable
import co.aospa.sense.camera.callables.CloseCameraCallable
import co.aospa.sense.camera.callables.ReadParamsCallable
import co.aospa.sense.camera.callables.WriteParamsCallable
import co.aospa.sense.camera.callables.StartPreviewCallable
import co.aospa.sense.camera.callables.StopPreviewCallable
import co.aospa.sense.camera.callables.AddCallbackBufferCallable
import co.aospa.sense.camera.callables.SetPreviewCallbackCallable
import co.aospa.sense.camera.callables.SetFaceDetectionCallback
import co.aospa.sense.camera.callables.SetDisplayOrientationCallback

class CameraService private constructor() {

    private val mServiceHandler: Handler
    private fun addCallable(cameraCallable: CameraCallable) {
        mServiceHandler.sendMessage(mServiceHandler.obtainMessage(DEFAULT_MSG_TYPE, cameraCallable))
    }

    private object LazyLoader {
        var field: CameraService = CameraService()
            get() = LazyLoader.field
    }

    companion object {
        private const val DEFAULT_MSG_TYPE = 1
        fun openCamera(id: Int, errorListener: CameraEventListener?, listener: CameraListener?) {
            LazyLoader.field.addCallable(OpenCameraCallable(id, errorListener, listener))
        }

        fun closeCamera(listener: CameraListener?) {
            val instance: CameraService = LazyLoader.field
            clearQueue()
            instance.addCallable(CloseCameraCallable(listener))
        }

        fun readParameters(eventListener: CameraEventListener?, listener: CameraListener?) {
            LazyLoader.field.addCallable(ReadParamsCallable(eventListener, listener))
        }

        fun writeParameters(listener: CameraListener?) {
            LazyLoader.field.addCallable(WriteParamsCallable(listener))
        }

        fun startPreview(surfaceTexture: SurfaceTexture?, listener: CameraListener?) {
            LazyLoader.field.addCallable(StartPreviewCallable(surfaceTexture, listener))
        }

        fun startPreview(surfaceHolder: SurfaceHolder?, listener: CameraListener?) {
            LazyLoader.field.addCallable(StartPreviewCallable(surfaceHolder, listener))
        }

        fun stopPreview(listener: CameraListener?) {
            LazyLoader.field.addCallable(StopPreviewCallable(listener))
        }

        fun addCallbackBuffer(data: ByteArray?, listener: CameraListener?) {
            LazyLoader.field.addCallable(AddCallbackBufferCallable(data, listener))
        }

        fun setPreviewCallback(
            eventListener: CameraEventListener?,
            withBuffer: Boolean,
            listener: CameraListener?
        ) {
            LazyLoader.field.addCallable(
                SetPreviewCallbackCallable(
                    eventListener,
                    withBuffer,
                    listener
                )
            )
        }

        fun setFaceDetectionCallback(
            faceDetectionListener: Camera.FaceDetectionListener?,
            listener: CameraListener?
        ) {
            LazyLoader.field.addCallable(
                SetFaceDetectionCallback(
                    faceDetectionListener,
                    listener
                )
            )
        }

        fun setDisplayOrientationCallback(angle: Int, listener: CameraListener?) {
            LazyLoader.field.addCallable(SetDisplayOrientationCallback(angle, listener))
        }

        fun clearQueue() {
            LazyLoader.field.mServiceHandler.removeMessages(DEFAULT_MSG_TYPE)
        }
    }

    init {
        val cameraHandlerThread = CameraHandlerThread()
        cameraHandlerThread.start()
        mServiceHandler = Handler(cameraHandlerThread.looper) { message: Message ->
            (message.obj as CameraCallable).run()
            true
        }
    }
}
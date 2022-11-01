package co.aospa.sense.camera.listeners

import java.lang.Exception

interface CameraListener {
    fun onComplete(obj: Any?)
    fun onError(e: Exception?)
}
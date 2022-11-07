package co.aospa.sense.camera

import android.content.Context
import android.hardware.Camera
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.SystemProperties
import java.util.ArrayList
import java.util.Comparator
import kotlin.math.abs

object CameraUtil {

    fun getBestPreviewSize(parameters: Camera.Parameters?, width: Int, height: Int): Camera.Size {
        val supportedPreviewSizes = parameters!!.supportedPreviewSizes
        val previewSizes = ArrayList<Camera.Size>()
        for (size in supportedPreviewSizes) {
            if (size.width > size.height) {
                previewSizes.add(size)
            }
        }
        previewSizes.sortWith(Comparator.comparingInt { size: Camera.Size ->
            abs(
                size.width * size.height - width * height
            )
        })
        return previewSizes[0]
    }

    fun getCameraId(context: Context?): Int {
        val cameraIdProp = SystemProperties.get("ro.face.sense_service.camera_id")
        if (cameraIdProp != null && cameraIdProp != "") {
            return cameraIdProp.toInt()
        }
        try {
            val cameraManager = context!!.getSystemService(
                CameraManager::class.java
            )
            var cameraId: String
            var orientation: Int
            var characteristics: CameraCharacteristics
            for (i in cameraManager.cameraIdList.indices) {
                cameraId = cameraManager.cameraIdList[i]
                characteristics = cameraManager.getCameraCharacteristics(cameraId)
                orientation = characteristics.get(CameraCharacteristics.LENS_FACING)!!
                if (orientation == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId.toInt()
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return -1
    }
}
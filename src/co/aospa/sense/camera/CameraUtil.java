package co.aospa.sense.camera;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.SystemProperties;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CameraUtil {

    @SuppressWarnings("deprecation")
    public static Camera.Size getBestPreviewSize(Camera.Parameters parameters, int width, int height) {
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        ArrayList<Camera.Size> arrayList = new ArrayList<>();
        for (Camera.Size size : supportedPreviewSizes) {
            if (size.width > size.height) {
                arrayList.add(size);
            }
        }
        arrayList.sort(Comparator.comparingInt(size -> Math.abs((size.width * size.height) - (width * height))));
        return arrayList.get(0);
    }

    public static int getCameraId(Context context) {
        String cameraIdProp = SystemProperties.get("ro.face.sense_service.camera_id");
        if (cameraIdProp != null && !cameraIdProp.equals("")) {
            return Integer.parseInt(cameraIdProp);
        }
        try {
            CameraManager cameraManager = context.getSystemService(CameraManager.class);
            String cameraId;
            int cameraOrientation;
            CameraCharacteristics characteristics;
            for (int i = 0; i < cameraManager.getCameraIdList().length; i++) {
                cameraId = cameraManager.getCameraIdList()[i];
                characteristics = cameraManager.getCameraCharacteristics(cameraId);
                cameraOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
                    return Integer.parseInt(cameraId);
                }

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return -1;
    }
}

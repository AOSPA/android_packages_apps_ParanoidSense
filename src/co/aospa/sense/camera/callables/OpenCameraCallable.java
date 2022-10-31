package co.aospa.sense.camera.callables;

import android.hardware.Camera;
import android.util.Log;

import co.aospa.sense.camera.CameraHandlerThread;
import co.aospa.sense.camera.listeners.CameraListener;
import co.aospa.sense.camera.listeners.ErrorCallbackListener;
import co.aospa.sense.util.Util;

@SuppressWarnings("rawtypes")
public class OpenCameraCallable extends CameraCallable {
    private static final String TAG = OpenCameraCallable.class.getSimpleName();
    private final int mCameraId;
    private final ErrorCallbackListener mErrorListener;

    public OpenCameraCallable(int i, ErrorCallbackListener errorCallbackListener, CameraListener cameraListener) {
        super(cameraListener);
        mCameraId = i;
        mErrorListener = errorCallbackListener;
    }

    @SuppressWarnings("deprecation")
    private static Camera openCamera(int id) {
        return Camera.open(id);
    }

    @Override
    public CallableReturn call() {
        if (Util.IS_DEBUG_LOGGING) {
            Log.d(TAG, "device: connect device async task: start");
        }
        if (getCameraData().mCamera != null && getCameraData().mCameraId == mCameraId) {
            if (Util.IS_DEBUG_LOGGING) {
                Log.d(TAG, "Camera is already opened");
            }
            setErrorCallback(getCameraData().mCamera);
            return new CallableReturn(null);
        } else if (getCameraData().mCamera != null) {
            return new CallableReturn(new Exception("Other camera is all ready opened"));
        } else {
            try {
                openCamera();
                if (Util.IS_DEBUG_LOGGING) {
                    Log.d(TAG, "device: connect device async task:open camera complete");
                }
                return new CallableReturn(null);
            } catch (Exception e) {
                return new CallableReturn(e);
            }
        }
    }

    @Override
    public String getTag() {
        return TAG;
    }

    private void openCamera() {
        CameraHandlerThread.CameraData cameraData = getCameraData();
        try {
            if (Util.IS_DEBUG_LOGGING) {
                Log.d(TAG, "open camera " + mCameraId);
            }
            if (cameraData.mCameraId != mCameraId) {
                cameraData.mCamera = openCamera(mCameraId);
                cameraData.mCameraId = mCameraId;
            }
            if (Util.IS_DEBUG_LOGGING) {
                Log.d(TAG, "open camera success, id: " + getCameraData().mCameraId);
            }
            setErrorCallback(cameraData.mCamera);
        } catch (RuntimeException e) {
            if (Util.IS_DEBUG_LOGGING) {
                Log.e(TAG, "fail to connect Camera", e);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void setErrorCallback(Camera camera) {
        if (Util.IS_DEBUG_LOGGING) {
            Log.d(TAG, "set error callback");
        }
        camera.setErrorCallback((i, camera1) -> {
            if (mErrorListener != null) {
                mErrorListener.onEventCallback(i, null);
            }
        });
    }
}

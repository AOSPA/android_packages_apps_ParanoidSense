package co.aospa.sense.camera.callables;

import android.hardware.Camera;

import co.aospa.sense.camera.listeners.CameraListener;

public class SetDisplayOrientationCallback extends CameraCallable {
    private static final String TAG = AddCallbackBufferCallable.class.getSimpleName();
    private final int mAngle;

    public SetDisplayOrientationCallback(int angle, CameraListener cameraListener) {
        super(cameraListener);
        mAngle = angle;
    }

    @SuppressWarnings("deprecation")
    @Override
    public CallableReturn call() {
        Camera camera = getCameraData().mCamera;
        if (camera == null) {
            return new CallableReturn(new Exception("Camera isn't opened"));
        }
        camera.setDisplayOrientation(mAngle);
        return new CallableReturn(null);
    }

    @Override
    public String getTag() {
        return TAG;
    }
}

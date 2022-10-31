package co.aospa.sense.camera.callables;

import android.hardware.Camera;

import co.aospa.sense.camera.listeners.CameraListener;

public class AddCallbackBufferCallable extends CameraCallable {
    private static final String TAG = AddCallbackBufferCallable.class.getSimpleName();
    private final byte[] mBuffer;

    public AddCallbackBufferCallable(byte[] buffer, CameraListener cameraListener) {
        super(cameraListener);
        mBuffer = buffer;
    }

    @SuppressWarnings("deprecation")
    @Override
    public CallableReturn call() {
        Camera camera = getCameraData().mCamera;
        if (camera == null) {
            return new CallableReturn(new Exception("Camera isn't opened"));
        }
        camera.addCallbackBuffer(mBuffer);
        return new CallableReturn(null);
    }

    @Override
    public String getTag() {
        return TAG;
    }
}

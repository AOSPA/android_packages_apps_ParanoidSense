package co.aospa.sense.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.view.SurfaceHolder;

import co.aospa.sense.camera.callables.AddCallbackBufferCallable;
import co.aospa.sense.camera.callables.AutoFocusCallable;
import co.aospa.sense.camera.callables.CameraCallable;
import co.aospa.sense.camera.callables.CloseCameraCallable;
import co.aospa.sense.camera.callables.OpenCameraCallable;
import co.aospa.sense.camera.callables.ReadParamsCallable;
import co.aospa.sense.camera.callables.SetDisplayOrientationCallback;
import co.aospa.sense.camera.callables.SetFaceDetectionCallback;
import co.aospa.sense.camera.callables.SetPreviewCallbackCallable;
import co.aospa.sense.camera.callables.StartPreviewCallable;
import co.aospa.sense.camera.callables.StopPreviewCallable;
import co.aospa.sense.camera.callables.WriteParamsCallable;
import co.aospa.sense.camera.listeners.ByteBufferCallbackListener;
import co.aospa.sense.camera.listeners.CameraListener;
import co.aospa.sense.camera.listeners.ErrorCallbackListener;
import co.aospa.sense.camera.listeners.FocusResultListener;
import co.aospa.sense.camera.listeners.ReadParametersListener;

public class CameraService {

    private static final int DEFAULT_MSG_TYPE = 1;
    private final Handler mServiceHandler;

    private CameraService() {
        CameraHandlerThread cameraHandlerThread = new CameraHandlerThread();
        cameraHandlerThread.start();
        this.mServiceHandler = new Handler(cameraHandlerThread.getLooper(), message -> {
            ((CameraCallable) message.obj).run();
            return true;
        });
    }

    private static CameraService getInstance() {
        return LazyLoader.INSTANCE;
    }

    public static void openCamera(int id, ErrorCallbackListener errorCallbackListener, CameraListener cameraListener) {
        getInstance().addCallable(new OpenCameraCallable(id, errorCallbackListener, cameraListener));
    }

    public static void closeCamera(CameraListener cameraListener) {
        CameraService instance = getInstance();
        clearQueue();
        instance.addCallable(new CloseCameraCallable(cameraListener));
    }

    public static void autoFocus(boolean enable, FocusResultListener focusResultListener, CameraListener cameraListener) {
        getInstance().addCallable(new AutoFocusCallable(enable, focusResultListener, cameraListener));
    }

    public static void readParameters(ReadParametersListener readParametersListener, CameraListener cameraListener) {
        getInstance().addCallable(new ReadParamsCallable(readParametersListener, cameraListener));
    }

    public static void writeParameters(CameraListener cameraListener) {
        getInstance().addCallable(new WriteParamsCallable(cameraListener));
    }

    public static void startPreview(CameraListener cameraListener) {
        getInstance().addCallable(new StartPreviewCallable(cameraListener));
    }

    public static void startPreview(SurfaceTexture surfaceTexture, CameraListener cameraListener) {
        getInstance().addCallable(new StartPreviewCallable(surfaceTexture, cameraListener));
    }

    public static void startPreview(SurfaceHolder surfaceHolder, CameraListener cameraListener) {
        getInstance().addCallable(new StartPreviewCallable(surfaceHolder, cameraListener));
    }

    public static void stopPreview(CameraListener cameraListener) {
        getInstance().addCallable(new StopPreviewCallable(cameraListener));
    }

    public static void addCallbackBuffer(byte[] data, CameraListener cameraListener) {
        getInstance().addCallable(new AddCallbackBufferCallable(data, cameraListener));
    }

    public static void setPreviewCallback(ByteBufferCallbackListener byteBufferCallbackListener, boolean withBuffer, CameraListener cameraListener) {
        getInstance().addCallable(new SetPreviewCallbackCallable(byteBufferCallbackListener, withBuffer, cameraListener));
    }

    public static void setFaceDetectionCallback(Camera.FaceDetectionListener faceDetectionListener, CameraListener cameraListener) {
        getInstance().addCallable(new SetFaceDetectionCallback(faceDetectionListener, cameraListener));
    }

    public static void setDisplayOrientationCallback(int angle, CameraListener cameraListener) {
        getInstance().addCallable(new SetDisplayOrientationCallback(angle, cameraListener));
    }

    public static void clearQueue() {
        getInstance().mServiceHandler.removeMessages(DEFAULT_MSG_TYPE);
    }

    private void addCallable(CameraCallable cameraCallable) {
        this.mServiceHandler.sendMessage(this.mServiceHandler.obtainMessage(DEFAULT_MSG_TYPE, cameraCallable));
    }

    private static final class LazyLoader {
        private static final CameraService INSTANCE = new CameraService();

        private LazyLoader() {
        }
    }
}

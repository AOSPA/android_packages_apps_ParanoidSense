package co.aospa.sense.camera.callables;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import co.aospa.sense.SenseApp;
import co.aospa.sense.camera.CameraHandlerThread;
import co.aospa.sense.camera.listeners.CallableListener;
import co.aospa.sense.util.Util;

import java.lang.ref.WeakReference;

public abstract class CameraCallable {
    protected final WeakReference<CallableListener> mCameraListener;
    private long mBegin;

    public CameraCallable(CallableListener callableListener) {
        mCameraListener = new WeakReference<>(callableListener);
    }

    protected static void runOnUiThread(Runnable runnable) {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post((runnable));
    }

    public abstract CallableReturn call();

    public abstract String getTag();

    public CameraHandlerThread.CameraData getCameraData() {
        return ((CameraHandlerThread) Thread.currentThread()).getCameraData();
    }

    @SuppressWarnings("deprecation")
    public Camera.Parameters getCameraParameters() {
        return ((CameraHandlerThread) Thread.currentThread()).getCameraData().mParameters;
    }

    public void run() {
        if (Util.IS_DEBUG_LOGGING) {
            Log.d(getTag(), "Begin");
        }
        mBegin = SystemClock.elapsedRealtime();
        final CallableReturn call = call();
        if (Util.IS_DEBUG_LOGGING) {
            String tag = getTag();
            Log.d(tag, "End (dur:" + (SystemClock.elapsedRealtime() - mBegin) + ")");
        }
        runOnUiThread(() -> callback(call));
    }

    public void callback(CallableReturn callableReturn) {
        long elapsedRealtime = SystemClock.elapsedRealtime() - mBegin;
        CallableListener callableListener = mCameraListener.get();
        if (callableReturn.exception != null) {
            String tag = getTag();
            Log.w(tag, "Exception in result (dur:" + elapsedRealtime + ")", callableReturn.exception);
            if (callableListener != null) {
                callableListener.onError(callableReturn.exception);
                return;
            }
            return;
        }
        String tag2 = getTag();
        Log.d(tag2, "Result success (dur:" + elapsedRealtime + ")");
        if (callableListener != null) {
            callableListener.onComplete(callableReturn.value);
        }
    }
}

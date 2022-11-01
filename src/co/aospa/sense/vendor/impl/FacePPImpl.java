package co.aospa.sense.vendor.impl;

import android.content.Context;
import android.util.Log;
import java.io.File;

import co.aospa.sense.util.Constants;
import co.aospa.sense.util.PreferenceHelper;
import co.aospa.sense.vendor.Vendor;
import co.aospa.sense.vendor.util.ConUtil;
import co.aospa.sense.vendor.util.VendorUnlockEncryptor;
import co.aospa.sense.R;

public class FacePPImpl extends Vendor {

    private static final String TAG = FacePPImpl.class.getSimpleName();

    private static final boolean DEBUG = true;
    private static final String SDK_VERSION = "1";
    private final Context mContext;
    private SERVICE_STATE mCurrentState = SERVICE_STATE.INITING;
    private final PreferenceHelper mPreferenceHelper;

    public enum SERVICE_STATE {
        INITING,
        IDLE,
        ENROLLING,
        UNLOCKING,
        ERROR
    }

    public FacePPImpl(Context context) {
        mContext = context;
        mPreferenceHelper = new PreferenceHelper(context);
    }

    @Override
    public void init() {
        synchronized (this) {
            if (mCurrentState != SERVICE_STATE.INITING) {
                Log.d(TAG, " Has been init, ignore");
                return;
            }
            String str = TAG;
            Log.i(str, "init start");
            boolean z = !SDK_VERSION.equals(mPreferenceHelper.getStringValueByKey(Constants.SHARED_KEY_SDK_VERSION));
            File dir = mContext.getDir("faceunlock_data", 0);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String raw = ConUtil.getRaw(mContext, R.raw.model_file, "model", "model_file", z);
            if (raw == null) {
                Log.e(str, "Unavalibale memory, init failed, stop self");
                return;
            }
            String raw2 = ConUtil.getRaw(mContext, R.raw.panorama_mgb, "model", "panorama_mgb", z);
            MegviiFaceUnlockImpl.getInstance().initHandle(dir.getAbsolutePath(), new VendorUnlockEncryptor());
            Log.i(str, "init stop");
            if (MegviiFaceUnlockImpl.getInstance().initAllWithPath(raw2, "", raw) != 0) {
                Log.e(str, "init failed, stop self");
                return;
            }
            if (z) {
                restoreFeature();
                this.mPreferenceHelper.saveStringValue(Constants.SHARED_KEY_SDK_VERSION, SDK_VERSION);
            }
            this.mCurrentState = SERVICE_STATE.IDLE;
        }
    }

    private void restoreFeature() {
        Log.i(TAG, "RestoreFeature");
        synchronized (this) {
            MegviiFaceUnlockImpl.getInstance().prepare();
            MegviiFaceUnlockImpl.getInstance().restoreFeature();
            MegviiFaceUnlockImpl.getInstance().reset();
        }
    }

    @Override
    public void compareStart() {
        synchronized (this) {
            if (this.mCurrentState == SERVICE_STATE.INITING) {
                init();
            }
            if (this.mCurrentState == SERVICE_STATE.UNLOCKING) {
                return;
            }
            if (this.mCurrentState != SERVICE_STATE.IDLE) {
                String str = TAG;
                Log.e(str, "unlock start failed: current state: " + this.mCurrentState);
                return;
            }
            Log.i(TAG, "compareStart");
            MegviiFaceUnlockImpl.getInstance().prepare();
            this.mCurrentState = SERVICE_STATE.UNLOCKING;
        }
    }

    @Override
    public int compare(byte[] bArr, int i, int i2, int i3, boolean z, boolean z2, int[] iArr) {
        synchronized (this) {
            if (this.mCurrentState != SERVICE_STATE.UNLOCKING) {
                String str = TAG;
                Log.e(str, "compare failed: current state: " + this.mCurrentState);
                return -1;
            }
            int compare = MegviiFaceUnlockImpl.getInstance().compare(bArr, i, i2, i3, z, z2, iArr);
            String str2 = TAG;
            Log.i(str2, "compare finish: " + compare);
            if (compare == 0) {
                compareStop();
            }
            return compare;
        }
    }

    @Override // co.aospa.sense.vendor.Vendor
    public void compareStop() {
        synchronized (this) {
            if (this.mCurrentState != SERVICE_STATE.UNLOCKING) {
                String str = TAG;
                Log.e(str, "compareStop failed: current state: " + this.mCurrentState);
                return;
            }
            Log.i(TAG, "compareStop");
            MegviiFaceUnlockImpl.getInstance().reset();
            this.mCurrentState = SERVICE_STATE.IDLE;
        }
    }

    @Override // co.aospa.sense.vendor.Vendor
    public void saveFeatureStart() {
        synchronized (this) {
            if (this.mCurrentState == SERVICE_STATE.INITING) {
                init();
            } else if (this.mCurrentState == SERVICE_STATE.UNLOCKING) {
                Log.e(TAG, "save feature, stop unlock");
                compareStop();
            }
            if (this.mCurrentState != SERVICE_STATE.IDLE) {
                String str = TAG;
                Log.e(str, "saveFeatureStart failed: current state: " + this.mCurrentState);
            }
            Log.i(TAG, "saveFeatureStart");
            MegviiFaceUnlockImpl.getInstance().prepare();
            this.mCurrentState = SERVICE_STATE.ENROLLING;
        }
    }

    @Override // co.aospa.sense.vendor.Vendor
    public int saveFeature(byte[] bArr, int i, int i2, int i3, boolean z, byte[] bArr2, byte[] bArr3, int[] iArr) {
        synchronized (this) {
            if (this.mCurrentState != SERVICE_STATE.ENROLLING) {
                String str = TAG;
                Log.e(str, "save feature failed , current state : " + this.mCurrentState);
                return -1;
            }
            Log.i(TAG, "saveFeature");
            return MegviiFaceUnlockImpl.getInstance().saveFeature(bArr, i, i2, i3, z, bArr2, bArr3, iArr);
        }
    }

    @Override // co.aospa.sense.vendor.Vendor
    public void saveFeatureStop() {
        synchronized (this) {
            if (this.mCurrentState != SERVICE_STATE.ENROLLING) {
                String str = TAG;
                Log.d(str, "saveFeatureStop failed: current state: " + this.mCurrentState);
            }
            Log.i(TAG, "saveFeatureStop");
            MegviiFaceUnlockImpl.getInstance().reset();
            this.mCurrentState = SERVICE_STATE.IDLE;
        }
    }

    @Override // co.aospa.sense.vendor.Vendor
    public void setDetectArea(int i, int i2, int i3, int i4) {
        synchronized (this) {
            Log.i(TAG, "setDetectArea start");
            MegviiFaceUnlockImpl.getInstance().setDetectArea(i, i2, i3, i4);
        }
    }

    @Override // co.aospa.sense.vendor.Vendor
    public void deleteFeature(int i) {
        synchronized (this) {
            String str = TAG;
            Log.i(str, "deleteFeature start");
            MegviiFaceUnlockImpl.getInstance().deleteFeature(i);
            Log.i(str, "deleteFeature stop");
            release();
        }
    }

    @Override // co.aospa.sense.vendor.Vendor
    public int getFeatureCount() {
        return 0;
    }

    @Override // co.aospa.sense.vendor.Vendor
    public void release() {
        synchronized (this) {
            if (this.mCurrentState == SERVICE_STATE.INITING) {
                Log.i(TAG, "has been released, ignore");
                return;
            }
            String str = TAG;
            Log.i(str, "release start");
            MegviiFaceUnlockImpl.getInstance().release();
            this.mCurrentState = SERVICE_STATE.INITING;
            Log.i(str, "release stop");
        }
    }
}

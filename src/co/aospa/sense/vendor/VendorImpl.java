package co.aospa.sense.vendor;

import android.content.Context;


import co.aospa.sense.vendor.impl.FacePPImpl;

public class VendorImpl extends Vendor {
    private final Vendor mFaceManager;

    public VendorImpl(Context context) {
        this.mFaceManager = new FacePPImpl(context);
    }

    @Override
    public int compare(byte[] bArr, int i, int i2, int i3, boolean z, boolean z2, int[] iArr) {
        return this.mFaceManager.compare(bArr, i, i2, i3, z, z2, iArr);
    }

    @Override
    public void compareStart() {
        this.mFaceManager.compareStart();
    }

    @Override
    public void compareStop() {
        this.mFaceManager.compareStop();
    }

    @Override
    public void deleteFeature(int i) {
        this.mFaceManager.deleteFeature(i);
    }

    @Override
    public int getFeatureCount() {
        return this.mFaceManager.getFeatureCount();
    }

    @Override
    public void init() {
        this.mFaceManager.init();
    }

    @Override
    public void release() {
        this.mFaceManager.release();
    }

    @Override
    public int saveFeature(byte[] bArr, int i, int i2, int i3, boolean z, byte[] bArr2, byte[] bArr3, int[] iArr) {
        return this.mFaceManager.saveFeature(bArr, i, i2, i3, z, bArr2, bArr3, iArr);
    }

    @Override
    public void saveFeatureStart() {
        this.mFaceManager.saveFeatureStart();
    }

    @Override
    public void saveFeatureStop() {
        this.mFaceManager.saveFeatureStop();
    }

    @Override
    public void setDetectArea(int i, int i2, int i3, int i4) {
        this.mFaceManager.setDetectArea(i, i2, i3, i4);
    }
}

package co.aospa.sense.vendor;

public abstract class Vendor {
    public abstract int compare(byte[] bArr, int i, int i2, int i3, boolean z, boolean z2, int[] iArr);

    public abstract void compareStart();

    public abstract void compareStop();

    public abstract void deleteFeature(int i);

    public abstract int getFeatureCount();

    public abstract void init();

    public abstract void release();

    public abstract int saveFeature(byte[] bArr, int i, int i2, int i3, boolean z, byte[] bArr2, byte[] bArr3, int[] iArr);

    public abstract void saveFeatureStart();

    public abstract void saveFeatureStop();

    public abstract void setDetectArea(int i, int i2, int i3, int i4);
}

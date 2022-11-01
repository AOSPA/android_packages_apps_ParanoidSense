package co.aospa.sense.vendor.util;

public interface UnlockEncryptor {
    byte[] decrypt(byte[] bArr);

    byte[] encrypt(byte[] bArr);
}

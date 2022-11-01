package co.aospa.sense.vendor.util;

import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class VendorUnlockEncryptor implements UnlockEncryptor {

    private static final String TAG = "VendorUnlockEncryptor";

    public static final String AKS_PROVIDER = "AndroidKeyStore";
    private static final int PROFILE_KEY_IV_SIZE = 12;
    public static final String SEED_ALIAS = "seed_faceunlock";

    public VendorUnlockEncryptor() {
        saveSeed();
    }

    private boolean saveSeed() {
        try {
            KeyStore keyStore = KeyStore.getInstance(AKS_PROVIDER);
            keyStore.load(null);
            if (keyStore.containsAlias(SEED_ALIAS)) {
                Log.i(TAG, "key is already created");
                return true;
            }
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(new SecureRandom());
            keyStore.setEntry(SEED_ALIAS, new KeyStore.SecretKeyEntry(keyGenerator.generateKey()),
                    new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setUserAuthenticationRequired(false).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            Log.i(TAG, "create key successfully");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            String str = TAG;
            Log.e(str, "Exception in store. " + e.toString());
            return false;
        }
    }

    private byte[] encryptData(byte[] bArr) {
        SecretKey secretKey;
        if (bArr == null) {
            return null;
        }
        try {
            KeyStore keyStore = KeyStore.getInstance(AKS_PROVIDER);
            keyStore.load(null);
            if (keyStore.containsAlias(SEED_ALIAS)) {
                secretKey = (SecretKey) keyStore.getKey(SEED_ALIAS, null);
            } else {
                Log.i(TAG, "key not exist, create key!");
                saveSeed();
                secretKey = (SecretKey) keyStore.getKey(SEED_ALIAS, null);
            }
            if (secretKey != null) {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(1, secretKey);
                byte[] doFinal = cipher.doFinal(bArr);
                byte[] iv = cipher.getIV();
                if (iv.length == 12) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byteArrayOutputStream.write(iv);
                    byteArrayOutputStream.write(doFinal);
                    return byteArrayOutputStream.toByteArray();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            String str = TAG;
            Log.e(str, "Exception in encrypt. " + e.toString());
        }
        return new byte[0];
    }

    private byte[] decryptData(byte[] bArr) {
        SecretKey secretKey = null;
        if (bArr == null) {
            return null;
        }
        try {
            KeyStore keyStore = KeyStore.getInstance(AKS_PROVIDER);
            keyStore.load(null);
            if (keyStore.containsAlias(SEED_ALIAS)) {
                secretKey = (SecretKey) keyStore.getKey(SEED_ALIAS, null);
            } else {
                Log.e(TAG, "key not exist, something is wrong!");
            }
            if (secretKey != null) {
                byte[] copyOfRange = Arrays.copyOfRange(bArr, 0, 12);
                byte[] copyOfRange2 = Arrays.copyOfRange(bArr, 12, bArr.length);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(2, secretKey, new GCMParameterSpec(128, copyOfRange));
                return cipher.doFinal(copyOfRange2);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String str = TAG;
            Log.e(str, "Exception in decrypt. " + e.toString());
        }
        return new byte[0];
    }

    @Override
    public byte[] encrypt(byte[] bArr) {
        return encryptData(bArr);
    }

    @Override
    public byte[] decrypt(byte[] bArr) {
        return decryptData(bArr);
    }
}

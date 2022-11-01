package co.aospa.sense.vendor.util;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ConUtil {
    public static String getRaw(Context context, int i, String str, String str2, boolean z) {
        File file = new File(context.getDir("faceunlock_data", 0), str);
        if (file.exists() || file.mkdirs()) {
            File file2 = new File(file, str2);
            if (!z && file2.exists()) {
                return file2.getAbsolutePath();
            }
            byte[] bArr = new byte[1024];
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file2);
                InputStream openRawResource = context.getResources().openRawResource(i);
                while (true) {
                    int read = openRawResource.read(bArr);
                    if (read == -1) {
                        break;
                    }
                    fileOutputStream.write(bArr, 0, read);
                }
                String absolutePath = file2.getAbsolutePath();
                if (openRawResource != null) {
                    openRawResource.close();
                }
                fileOutputStream.close();
                return absolutePath;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}

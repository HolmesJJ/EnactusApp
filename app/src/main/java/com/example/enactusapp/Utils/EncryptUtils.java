package com.example.enactusapp.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptUtils {
    private static final String TAG = EncryptUtils.class.getSimpleName();
    private static EncryptUtils instance;
    private String key;

    private EncryptUtils(Context context) {
        String serialNo = this.getDeviceSerialNumber(context);
        this.key = this.sha(serialNo + "#$ERDTS$D%F^Gojikbh").substring(0, 16);
    }

    @SuppressLint({"HardwareIds"})
    private String getDeviceSerialNumber(Context context) {
        try {
            String deviceSerial = (String) Build.class.getField("SERIAL").get((Object) null);
            return TextUtils.isEmpty(deviceSerial) ? Settings.Secure.getString(context.getContentResolver(), "android_id") : deviceSerial;
        } catch (Exception var3) {
            return Settings.Secure.getString(context.getContentResolver(), "android_id");
        }
    }

    private String sha(String strText) {
        String strResult = null;
        if (strText != null && strText.length() > 0) {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("sha-256");
                messageDigest.update(strText.getBytes());
                byte[] byteBuffer = messageDigest.digest();
                StringBuffer strHexString = new StringBuffer();

                for (int i = 0; i < byteBuffer.length; ++i) {
                    String hex = Integer.toHexString(255 & byteBuffer[i]);
                    if (hex.length() == 1) {
                        strHexString.append('0');
                    }

                    strHexString.append(hex);
                }

                strResult = strHexString.toString();
            } catch (NoSuchAlgorithmException var8) {
                var8.printStackTrace();
            }
        }

        return strResult;
    }

    public static EncryptUtils getInstance(Context context) {
        if (instance == null) {
            Class var1 = EncryptUtils.class;
            synchronized (EncryptUtils.class) {
                if (instance == null) {
                    instance = new EncryptUtils(context.getApplicationContext());
                }
            }
        }

        return instance;
    }

    public String encryptByAes128(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keyspec = new SecretKeySpec(this.key.getBytes(), "AES");
            cipher.init(1, keyspec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            return Base64.encodeToString(encrypted, 2);
        } catch (Exception var5) {
            var5.printStackTrace();
            return null;
        }
    }

    public String decryptByAes128(String cipherText) {
        try {
            byte[] encrypted = Base64.decode(cipherText, 2);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keyspec = new SecretKeySpec(this.key.getBytes(), "AES");
            cipher.init(2, keyspec);
            byte[] original = cipher.doFinal(encrypted);
            String originalString = new String(original);
            return originalString;
        } catch (Exception var7) {
            var7.printStackTrace();
            return null;
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String encryptByAes128Old(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, this.key.getBytes());
            SecretKeySpec keyspec = new SecretKeySpec(this.key.getBytes(), "AES");
            cipher.init(1, keyspec, parameterSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            return Base64.encodeToString(encrypted, 2);
        } catch (Exception var6) {
            var6.printStackTrace();
            return null;
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String decryptByAes128Old(String cipherText) {
        try {
            byte[] encrypted = Base64.decode(cipherText, 2);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, this.key.getBytes());
            SecretKeySpec keyspec = new SecretKeySpec(this.key.getBytes(), "AES");
            cipher.init(2, keyspec, parameterSpec);
            byte[] original = cipher.doFinal(encrypted);
            String originalString = new String(original);
            return originalString;
        } catch (Exception var8) {
            var8.printStackTrace();
            return null;
        }
    }
}

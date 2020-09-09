package com.example.enactusapp.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapUtils {

    public static Bitmap createBitmapThumbnail(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = width;
        int newHeight = height;
        // 设置想要的大小
        if (width > 256) {
            newWidth = width / 2;
            newHeight = height / 2;
        }
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap newBitMap = Bitmap.createBitmap(bitmap, 0, 0, width, height,
                matrix, true);
        return newBitMap;
    }

    public static Bitmap getCropBitmap(Bitmap sourceBitmap, Rect originRect, int scale) {
        return getCropBitmap(sourceBitmap, originRect, scale, scale);
    }

    public static Bitmap getCropBitmap(Bitmap sourceBitmap, Rect orginRect, float scaleX, float scaleY) {
        if (sourceBitmap == null || sourceBitmap.isRecycled()) {
            return null;
        }
        Rect rect = getScaleRect(orginRect, scaleX, scaleY, sourceBitmap.getWidth(), sourceBitmap.getHeight());
        if (rect.width() <= 0 || rect.height() <= 0) return null;
        return sourceBitmap.createBitmap(sourceBitmap, rect.left, rect.top, rect.width(), rect.height());
    }

    public static Rect getScaleRect(Rect rect, float scaleX, float scaleY, int maxW, int maxH) {
        Rect resultRect = new Rect();
        int left = (int) (rect.left - rect.width() * (scaleX - 1) / 2);
        int right = (int) (rect.right + rect.width() * (scaleX - 1) / 2);
        int bottom = (int) (rect.bottom + rect.height() * (scaleY - 1) / 2);
        int top = (int) (rect.top - rect.height() * (scaleY - 1) / 2);
        resultRect.left = left > 0 ? left : 0;
        resultRect.right = right > maxW ? maxW : right;
        resultRect.bottom = bottom > maxH ? maxH : bottom;
        resultRect.top = top > 0 ? top : 0;
        return resultRect;
    }

    public static Bitmap rotateBitmap(Bitmap sourceBitmap, float rotateDegree, boolean isLRMirror, boolean isTMMirror) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotateDegree);
        if (isLRMirror) {
            matrix.postScale(-1, 1);
        }
        if (isTMMirror) {
            matrix.postScale(1, -1);
        }
        Bitmap rotaBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix, false);
        return rotaBitmap;
    }

    public static byte[] btmToBytes(Bitmap btm) {
        if (btm == null || btm.isRecycled()) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        btm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    public static void savePhotoToSDCard(String dirRoot, String photoName, Bitmap photoBitmap, int quality) {
        if (photoBitmap == null || photoBitmap.isRecycled()) return;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File dir = new File(dirRoot);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File photoFile = new File(dir, photoName); // 在指定路径下创建文件
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(photoFile);
                if (photoBitmap != null) {
                    if (photoBitmap.compress(Bitmap.CompressFormat.JPEG, quality,
                            fileOutputStream)) {
                        fileOutputStream.flush();
                    }
                }
            } catch (FileNotFoundException e) {
                photoFile.delete();
                e.printStackTrace();
            } catch (IOException e) {
                photoFile.delete();
                e.printStackTrace();
            } finally {
                try {
                    if (fileOutputStream != null) {

                        fileOutputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Bitmap compressImageFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length <= 10) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        int sampleSize = 1;
        int w = options.outWidth;
        int h = options.outHeight;
        if (w < 0 || h < 0) { // 不是图片文件直接返回
            return null;
        }
        int requestH = 800; // 需求最大高度
        int requestW = 800; // 需求最大宽度
        while ((h / sampleSize > requestH) || (w / sampleSize > requestW)) {
            sampleSize = sampleSize << 1;
        }
        options.inJustDecodeBounds = false; // 不再只读边
        options.inSampleSize = sampleSize;// 设置采样率大小
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    public static Bitmap compressImageFromFile(String filePath) {
        if (TextUtils.isEmpty(filePath)) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        int sampleSize = 1;
        int w = options.outWidth;
        int h = options.outHeight;
        if (w < 0 || h < 0) { // 不是图片文件直接返回
            return null;
        }
        int requestH = 1000; // 需求最大高度
        int requestW = 1000; // 需求最大宽度
        while ((h / sampleSize > requestH) || (w / sampleSize > requestW)) {
            sampleSize = sampleSize << 1;
        }
        options.inJustDecodeBounds = false; // 不再只读边
        options.inSampleSize = sampleSize;// 设置采样率大小
        return BitmapFactory.decodeFile(filePath, options);
    }

    public static byte[] convertNv21ToJpeg(byte[] nv21, int w, int h, Rect rect) {
        if (rect == null) rect = new Rect(0, 0, w, h);
        ByteArrayOutputStream outputSteam = new ByteArrayOutputStream();
        YuvImage image = new YuvImage(nv21, ImageFormat.NV21, w, h, null);
        image.compressToJpeg(rect, 100, outputSteam);
        return outputSteam.toByteArray();
    }
}

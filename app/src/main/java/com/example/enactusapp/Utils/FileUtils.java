package com.example.enactusapp.Utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

public class FileUtils {

    public static void copyFileIfNeed(Context context, String modelName) {
        InputStream is = null;
        OutputStream os = null;
        try {
            File modelFile = new File(context.getFilesDir(), modelName);
            is = context.getAssets().open(modelName);
            if (modelFile.length() == is.available()) {
                return;
            }

            os = new FileOutputStream(modelFile);
            byte[] buffer = new byte[1024 << 9]; // 512KB
            int length = is.read(buffer);
            while (length > 0) {
                os.write(buffer, 0, length);
                length = is.read(buffer);
            }
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(os != null){
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void writeJpegToDisk(byte[] img, String fileName) {
        FileOutputStream fops = null;
        try {
            File file = new File(fileName);
            if(!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            fops = new FileOutputStream(file);
            fops.write(img);
            fops.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(fops != null) {
                try {
                    fops.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void writePngToDisk(byte[] img, String fileName) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(img, 0, img.length);
        FileOutputStream fops = null;
        try {
            File file = new File(fileName);
            if(!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            fops = new FileOutputStream(file);
            fops.write(img);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fops);
            fops.flush();
            fops.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(fops != null) {
                try {
                    fops.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void writeBitmapToDisk(Bitmap bitmap, String fileName) {
        FileOutputStream fops = null;
        try {
            File file = new File(fileName);
            if(!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            fops = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fops);
            fops.flush();
            fops.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(fops != null) {
                try {
                    fops.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void writeYuvToDisk(int width, int height, int quality, byte[] image, String fileName) {
        FileOutputStream fops = null;
        try {
            File file = new File(fileName);
            if(!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            fops = new FileOutputStream(file);
            fops.flush();
            YuvImage yuvImage = new YuvImage(image, ImageFormat.NV21, width, height, null);
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), quality, fops);
            fops.flush();
            fops.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeTxtToDisk(String data, String fileName) {
        FileOutputStream fops = null;
        try {
            File file = new File(fileName);
            if(!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            fops = new FileOutputStream(file);
            fops.write(data.getBytes());
            fops.flush();
            fops.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(fops != null) {
                try {
                    fops.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void delDir(File f) {
        // 判断是否是一个目录, 不是的话跳过, 直接删除; 如果是一个目录, 先将其内容清空.
        if(f.isDirectory()) {
            // 获取子文件/目录
            File[] subFiles = f.listFiles();
            // 遍历该目录
            for (File subFile : subFiles) {
                // 递归调用删除该文件: 如果这是一个空目录或文件, 一次递归就可删除. 如果这是一个非空目录, 多次
                // 递归清空其内容后再删除
                delDir(subFile);
            }
        }
        // 删除空目录或文件
        f.delete();
    }

    // 创建一个临时目录，用于复制临时文件，如assets目录下的离线资源文件
    public static String createTmpDir(Context context, String dirPath) {
        String tmpDir = Environment.getExternalStorageDirectory().toString() + "/" + dirPath;
        if (!FileUtils.makeDir(tmpDir)) {
            tmpDir = context.getExternalFilesDir(dirPath).getAbsolutePath();
            if (tmpDir == null || !FileUtils.makeDir(tmpDir)) {
                throw new RuntimeException("create model resources dir failed :" + tmpDir);
            }
        }
        return tmpDir;
    }

    public static boolean fileCanRead(String filename) {
        File f = new File(filename);
        return f.canRead();
    }

    public static boolean makeDir(String dirPath) {
        File file = new File(dirPath);
        if (!file.exists()) {
            return file.mkdirs();
        } else {
            return true;
        }
    }

    public static void copyFromAssets(AssetManager assets, String source, String dest, boolean isCover)
            throws IOException {
        File file = new File(dest);
        if (isCover || (!isCover && !file.exists())) {
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                is = assets.open(source);
                String path = dest;
                fos = new FileOutputStream(path);
                byte[] buffer = new byte[1024];
                int size = 0;
                while ((size = is.read(buffer, 0, 1024)) >= 0) {
                    fos.write(buffer, 0, size);
                }
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                }
            }
        }
    }

    public static String getResourceText(Context context, int textId) {
        InputStream is = context.getResources().openRawResource(textId);
        try {
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            return new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}

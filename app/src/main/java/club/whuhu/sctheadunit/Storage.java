package club.whuhu.sctheadunit;

import android.app.Activity;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.EventListener;

public class Storage {

    private static Storage instance;

    private Activity activity;

    private File getSctFolder() {
       // File folder = new File(Environment.getExternalStorageDirectory(), "sct");
        File folder = new File(activity.getFilesDir(), "sct");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }

    private File getIconsFolder() {
        File folder = new File(getSctFolder(), "icons");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }

    public synchronized void init(Activity activity) {
        if (this.activity != null) {
            return;
        }
        this.activity = activity;
    }

    public synchronized static Storage getInstance() {
        if (instance == null) {
            instance = new Storage();
        }
        return instance;
    }

    private byte[] readFile(File file) {
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (Exception e) {
            return null;
        }

        return bytes;
    }

    private static String escape(String md5) {
        return md5.replaceAll(":|/|\\|", "-");
    }

    public Bitmap getIcon(String md5) {
        byte[] data = readFile(new File(getIconsFolder(), escape(md5) + ".jpg"));
        if (data == null) {
            return null;
        }

        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void storeIcon(String md5, byte[] data) {
        File file = new File(getIconsFolder(), escape(md5) + ".jpg");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

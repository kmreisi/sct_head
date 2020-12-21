package club.whuhu.sctheadunit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Storage {

    private static Storage instance;

    private final Dashboard dashboard;

    private File getSctFolder() {
        File extStore = Environment.getExternalStorageDirectory();
        File folder = new File(extStore, "sct");
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

    Storage(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public static void init(Dashboard dashboard) {
        instance = new Storage(dashboard);
    }

    public static Storage getInstance() {
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
            e.printStackTrace();
            return  null;
        }

        return  bytes;
    }

    public Bitmap getIcon(String md5) {

        byte[] data = readFile(new File(getIconsFolder(),md5 + ".bmp"));
        if (data == null) {
            return  null;
        }

        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }
    }

    private void writeFile(File file, byte[] data) {
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

    public void storeIcon(String md5, byte[] data){
        writeFile(new File(getIconsFolder(), md5 + ".bmp"), data);
    }
}

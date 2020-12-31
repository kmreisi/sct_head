package club.whuhu.sctheadunit;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Icon;
import android.util.Base64;

import java.util.HashMap;
import java.util.Map;

import club.whuhu.jrpc.JRPC;
import club.whuhu.sctheadunit.controller.Controller;

public class IconCache {

    private final Map<String, Bitmap> icons;
    private final Bitmap dummy;

    private static IconCache instance = null;
    private Activity activity;

    private IconCache() {
        icons = new HashMap<>();
        dummy = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    public synchronized void init(Activity activity) {
        if (this.activity != null) {
            return;
        }
        this.activity = activity;
    }

    public synchronized static IconCache getInstance() {
        if (instance == null) {
            instance = new IconCache();
        }
        return instance;
    }

    public static interface IGotIcon {
        void call(Bitmap icon);
    }

    public Bitmap getIcon(final String md5, final IGotIcon callback) {
        if (md5 == null) {
            return  dummy;
        }

        synchronized (this) {
            // check memory cache if already loaded
            Bitmap icon = icons.get(md5);
            if (icon != null) {
                return icon;
            }

            // check storage if already exists
            icon = Storage.getInstance().getIcon(md5);
            if (icon != null) {
                // loaded add to cache
                icons.put(md5, icon);
                return  icon;
            }

            JRPC jrpc = Controller.getInstance().getIconJrpc();
            if (jrpc != null) {
                // icon does not exist yet query from phone
                // in the mean time provide our dummy icon for all requests
                icons.put(md5, dummy);
                Map<String, String> params = new HashMap<>();
                params.put("icon_md5", md5);
                jrpc.send(new JRPC.Request("get_icon", params, new JRPC.Request.CallbackResponse() {
                    @Override
                    public void call(Object params) {
                        Map<String, Object> data = (Map<String, Object>) params;
                        String base = (String) data.get("icon");
                        if (base == null) {
                            return;
                        }

                        try {
                            byte[] byteArray = Base64.decode(base, Base64.DEFAULT);
                            final Bitmap icon = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                            Storage.getInstance().storeIcon(md5, byteArray);
                            icons.put(md5, icon);

                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    callback.call(icon);
                                }
                            });
                        } catch (Exception e) {

                            e.printStackTrace();
                        }

                    }
                }, new JRPC.Request.CallbackError() {
                    @Override
                    public void call(JRPC.Error error) {

                    }
                }));
            }

            return dummy;
        }
    }

    private Bitmap doInvert(Bitmap src) {
        int height = src.getHeight();
        int width = src.getWidth();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        ColorMatrix matrixGrayscale = new ColorMatrix();
        matrixGrayscale.setSaturation(0);

        ColorMatrix matrixInvert = new ColorMatrix();
        matrixInvert.set(new float[]
                {
                        -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                        0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                        0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                        0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                });
        matrixInvert.preConcat(matrixGrayscale);

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrixInvert);
        paint.setColorFilter(filter);

        canvas.drawBitmap(src, 0, 0, paint);
        return bitmap;
    }

    public Bitmap getInvertedIcon(final String md5) {
        synchronized (this) {
            // check memory cache if already loaded
            Bitmap icon = icons.get(md5);
            if (icon == null) {
                return null;
            }

            return doInvert(icon);
        }
    }
}

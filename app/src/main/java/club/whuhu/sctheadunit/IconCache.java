package club.whuhu.sctheadunit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.util.Base64;

import java.util.HashMap;
import java.util.Map;

import club.whuhu.jrpc.JRPC;

public class IconCache {

    private final Dashboard dashboard;
    private final Map<String, Bitmap> icons = new HashMap<>();

    private final Bitmap dummy;

    private static IconCache instance = null;

     public IconCache(Dashboard dashboard){
        this.dashboard = dashboard;
        this.dummy = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    public  static void  init(Dashboard dashboard) {
        instance = new IconCache(dashboard);
    }

    public static IconCache getInstance() {
        return instance;
    }

    public Bitmap getIcon(final String md5) {
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

            JRPC jrpc = Dashboard.getController().getIconJrpc();
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
                            Bitmap icon = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                            Storage.getInstance().storeIcon(md5, byteArray);
                            icons.put(md5, icon);

                            dashboard.receivedIcons();
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
}

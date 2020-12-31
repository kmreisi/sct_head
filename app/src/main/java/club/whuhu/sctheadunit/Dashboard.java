package club.whuhu.sctheadunit;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.sql.Time;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import club.whuhu.jrpc.JRPC;
import club.whuhu.sctheadunit.controller.Controller;
import club.whuhu.sctheadunit.controller.NotificationHandler;

public class Dashboard extends AppCompatActivity {

    private class NotificationContainer {
        ImageView notificationIcon;
        View dashView;
    }

    Map<String, NotificationContainer> notifications = new HashMap<>();

    LinearLayout notificationList;
    LinearLayout dashList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        getWindow().setAttributes(attributes);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        setContentView(R.layout.activity_dashboard);

        TextView weatherText = (TextView) findViewById(R.id.weather_text);

        Controller.getInstance().setNotificationListener(new NotificationHandler.IUpdateListener() {
            @Override
            public void onNotification(final NotificationHandler.PhoneNotification notification) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateNotification(notification);
                    }
                });
            }
        });

        notificationList = (LinearLayout) findViewById(R.id.list_notifications);
        dashList = (LinearLayout) findViewById(R.id.list_dash);

        for (NotificationHandler.PhoneNotification notification : Controller.getInstance().getNotifications()) {
            updateNotification(notification);
        }

        notificationList.setGravity(Controller.getInstance().getNotifications().size());
        dashList.setGravity(Controller.getInstance().getNotifications().size());
    }

    private NotificationContainer getNotificationContainer(NotificationHandler.PhoneNotification notification) {
        NotificationContainer cnt = notifications.get(notification.getKey());
        if (cnt != null) {
            return cnt;
        }

        if (!notification.isVisible()) {
            return null;
        }

        cnt = new NotificationContainer();
        notifications.put(notification.getKey(), cnt);

        if (notification.isDash()) {
            cnt.dashView = LayoutInflater.from(this).inflate(R.layout.listitem_dash, dashList, false);
            cnt.dashView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            dashList.addView(cnt.dashView);
        } else {
            cnt.notificationIcon = new ImageView(this);
            cnt.notificationIcon.setAdjustViewBounds(true);
            cnt.notificationIcon.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            notificationList.addView(cnt.notificationIcon);
        }

        return cnt;
    }

    public void updateNotification(NotificationHandler.PhoneNotification notification) {

        final NotificationContainer cnt = getNotificationContainer(notification);

        if (cnt == null) {
            return;
        }

        if (notification.isVisible()) {

            final ImageView icon = cnt.dashView == null ? null : (ImageView) cnt.dashView.findViewById(R.id.icon);
            TextView text = cnt.dashView == null ? null : (TextView) cnt.dashView.findViewById(R.id.text);
            LinearLayout actionList = cnt.dashView == null ? null : (LinearLayout) cnt.dashView.findViewById(R.id.actions);

            Bitmap bitmap = IconCache.getInstance().getIcon(notification.getIconMd5(), new IconCache.IGotIcon() {
                @Override
                public void call(Bitmap bitmap) {
                    if (cnt.notificationIcon != null) {
                        cnt.notificationIcon.setImageBitmap(bitmap);
                    }
                    if (icon != null) {
                        icon.setImageBitmap(bitmap);
                    }
                }
            });

            if (cnt.notificationIcon != null) {
                cnt.notificationIcon.setImageBitmap(bitmap);
            }

            if (cnt.dashView != null) {
                text.setText(notification.getSpannable());
                icon.setImageBitmap(bitmap);
                actionList.removeAllViews();
                final List<NotificationHandler.PhoneNotification.Action> actions = notification.getActions();
                for (final NotificationHandler.PhoneNotification.Action action : actions) {
                    final String iconMd5 = action.getIconMd5();
                    if (iconMd5 == null) {
                        continue;
                    }
                    final ImageView image = new ImageView(this);
                    image.setAdjustViewBounds(true);
                    image.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
                    image.setImageBitmap(IconCache.getInstance().getIcon(iconMd5, new IconCache.IGotIcon() {
                        @Override
                        public void call(Bitmap bitmap) {
                            image.setImageBitmap(bitmap);
                        }
                    }));

                    // use an image we don't want to have the keyboard focusing, these should only be clicked from the touchscreen!
                    image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            action.doIt();
                            image.setImageBitmap(IconCache.getInstance().getInvertedIcon(iconMd5));
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            image.setImageBitmap(IconCache.getInstance().getIcon(iconMd5, new IconCache.IGotIcon() {
                                                @Override
                                                public void call(Bitmap bitmap) {
                                                    image.setImageBitmap(bitmap);
                                                }
                                            }));
                                        }
                                    });

                                }
                            }, 100);
                        }
                    });
                    actionList.addView(image);
                }
                actionList.setGravity(actionList.getChildCount());
            }
        } else {
            notificationList.removeView(cnt.notificationIcon);
            dashList.removeView(cnt.dashView);
            notifications.remove(notification.getKey());
        }

        notificationList.setGravity(notificationList.getChildCount());
        dashList.setGravity(dashList.getChildCount());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            startActivity(new Intent(this, NotificationListActivity.class));
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            startActivity(new Intent(this, ActionListActivity.class));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}

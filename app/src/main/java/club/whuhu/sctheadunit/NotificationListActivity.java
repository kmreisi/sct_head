package club.whuhu.sctheadunit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import club.whuhu.jrpc.JRPC;
import club.whuhu.sctheadunit.controller.Controller;
import club.whuhu.sctheadunit.controller.NotificationHandler;
import club.whuhu.sctheadunit.ui.UiList;

public class NotificationListActivity extends AppCompatActivity {

    private NotificationListActivity activity;
    private UiList.Scope notificationScope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        getWindow().setAttributes(attributes);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        setContentView(R.layout.activity_notification_list);
        this.activity = this;

        ListView listView = (ListView) findViewById(R.id.notification_list);
        notificationScope = new UiList.Scope(listView, this, new ArrayList<UiList.Entry>(), new UiList.Scope.IUpdater() {

            @Override
            public Object getKey(Object data) {
                if (data instanceof NotificationHandler.PhoneNotification) {
                    return ((NotificationHandler.PhoneNotification) data).getKey();
                }
                return null;
            }

            @Override
            public boolean filter(UiList.Entry e, Object data) {
                if (data instanceof NotificationHandler.PhoneNotification) {
                    final NotificationHandler.PhoneNotification notification = (NotificationHandler.PhoneNotification) data;
                    return !((NotificationHandler.PhoneNotification) data).isVisible();
                }

                return false;
            }

            @Override
            public void load(UiList.Entry entry, Object data) {
                if (data instanceof NotificationHandler.PhoneNotification) {
                    final NotificationHandler.PhoneNotification notification = (NotificationHandler.PhoneNotification) data;
                    entry.setData(data);
                    entry.setTitle(notification.getTitle());
                    entry.setText(notification.getText());
                    entry.setIconMd5(notification.getIconMd5());
                    entry.setOnClick(new UiList.Entry.IOnClick() {
                        @Override
                        public void clicked(UiList.Scope scope, UiList.Entry entry) {
                            onNotificationClicked(notification, scope, entry);
                        }

                        @Override
                        public boolean longClicked(UiList.Scope scope, UiList.Entry entry) {
                            notification.hide();
                            return true;
                        }
                    });
                }
            }

            @Override
            public void dispose() {
                Controller.getInstance().setNotificationListener(null);
            }
        });

        Controller.getInstance().setNotificationListener(new NotificationHandler.IUpdateListener() {
            @Override
            public void onNotification(final NotificationHandler.PhoneNotification notification) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notificationScope.update(notification);
                    }
                });
            }
        });

        NotificationHandler.PhoneNotification last = null;
        for (NotificationHandler.PhoneNotification notification : Controller.getInstance().getNotifications()) {
            notificationScope.update(notification);
            last = notification;
        }

        if (last != null) {
            notificationScope.selectByKey(last.getKey());
        }
    }

    private void onNotificationClicked(NotificationHandler.PhoneNotification notification, UiList.Scope parent, UiList.Entry entry) {
        if (notification.getActions() == null) {
            // to mark as read and dispose
            return;
        }

        final UiList.Scope actions = parent.child(entry, new ArrayList<UiList.Entry>(), new UiList.Scope.IUpdater() {
            @Override
            public Object getKey(Object data) {
                if (data instanceof NotificationHandler.PhoneNotification.Action) {
                    return  ((NotificationHandler.PhoneNotification.Action) data).getTitle();
                }
                return null;
            }

            @Override
            public void load(UiList.Entry entry, Object data) {
                if (data instanceof NotificationHandler.PhoneNotification.Action) {
                    final NotificationHandler.PhoneNotification.Action action = (NotificationHandler.PhoneNotification.Action) data;
                    entry.setTitle(action.getTitle());
                    entry.setText(action.getText());
                    entry.setIconMd5(action.getIconMd5());
                    entry.setOnClick(new UiList.Entry.IOnClick() {
                        @Override
                        public void clicked(UiList.Scope scope, UiList.Entry entry) {
                            action.doIt();
                            scope.back();
                        }

                        @Override
                        public boolean longClicked(UiList.Scope scope, UiList.Entry entry) {
                            return false;
                        }
                    });
                }
            }
        });

        for (NotificationHandler.PhoneNotification.Action action : notification.getActions()) {
            actions.update(action);
        }
    }

    @Override
    public void onBackPressed() {
        if (notificationScope.handleBack()){
            return;
        }

        startActivity(new Intent(this, Dashboard.class));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}

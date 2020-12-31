package club.whuhu.sctheadunit.controller;


import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import club.whuhu.jrpc.JRPC;
import club.whuhu.sctheadunit.ui.UiList;

public class NotificationHandler {

    public static class PhoneNotification {
        private final String key;
        private String title;
        private String text;
        private String iconMd5;
        private List<Action> actions;
        private boolean visible;
        private boolean dash;

        public static class Action {
            private final PhoneNotification parent;
            private final String title;
            private final String text;
            private final String iconMd5;

            public Action(PhoneNotification parent, Object data) {
                Map<String, Object> params = (Map<String, Object>) data;
                this.parent = parent;
                title = (String) params.get("title");
                text = (String) params.get("text");
                iconMd5 = (String) params.get("icon_md5");
            }


            public String getTitle() {
                return title;
            }

            public String getText() {
                return text;
            }

            public String getIconMd5() {
                return iconMd5;
            }

            public void doIt() {
                Map<String, Object> data = new HashMap<>();
                data.put("key", parent.key);
                data.put("title", getTitle());

                Controller.getInstance().getEventJrpc().send(new JRPC.Request("notification_action", data, null, null));
            }
        }

        public PhoneNotification(Object data) {
            Map<String, Object> params = (Map<String, Object>) data;
            key = (String) params.get("key");
            load(data);
        }

        public void load(Object data) {
            Map<String, Object> params = (Map<String, Object>) data;
            setTitle((String) params.get("title"));
            setText((String) params.get("text"));
            setIconMd5((String) params.get("icon_md5"));
            setActions((List<Object>) params.get("actions"));
            setDash((Boolean) params.get("dash"));
            setVisible(true);
        }

        public Spannable getSpannable() {
            StringBuilder complete = new StringBuilder();

            if (title != null && !title.isEmpty()) {
                complete.append(title + "\n");
            }
            if (text != null) {
                complete.append(text);
            }

            Spannable s = new SpannableString(complete);
            if (title != null && !title.isEmpty()) {
                s.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return  s;
        }


        public void setIconMd5(String iconMd5) {
            this.iconMd5 = iconMd5;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setText(String text) {
            this.text = text;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public void setDash(boolean dash) {
            this.dash = dash;
        }

        public boolean isDash() {
            return dash;
        }

        public void setActions(List<Object> data) {
            if (data != null) {
                actions = new ArrayList<>();
                for (Object action : data) {
                    actions.add(new Action(this, action));
                }
            } else {
                actions = null;
            }
        }

        public void hide() {
            Map<String, Object> data = new HashMap<>();
            data.put("key", key);

            Controller.getInstance().getEventJrpc().send(new JRPC.Request("notification_cancel", data, null, null));
        }

        public String getKey() {
            return key;
        }

        public String getTitle() {
            return title;
        }

        public String getText() {
            return text;
        }

        public String getIconMd5() {
            return iconMd5;
        }

        public List<Action> getActions() {
            return actions;
        }

        public boolean isVisible() {
            return visible;
        }
    }

    public static interface IUpdateListener {
        void onNotification(PhoneNotification notification);
    }

    private final Controller controller;
    private final SortedSet<PhoneNotification> notifications;
    private IUpdateListener listener;

    public NotificationHandler(Controller controller) {
        this.controller = controller;
        notifications = new TreeSet<>(new Comparator<PhoneNotification>() {
            @Override
            public int compare(PhoneNotification o1, PhoneNotification o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        controller.getEventJrpc().register("notification", new JRPC.Method() {
            @Override
            public void call(JRPC.Response r, Object params) throws JRPC.Error {
                PhoneNotification notification = new PhoneNotification(params);
                synchronized (notifications) {

                    notifications.remove(notification);
                    notifications.add(notification);
                }
                if (listener != null) {
                    listener.onNotification(notification);
                }
            }
        });

        controller.getEventJrpc().register("notification_removed", new JRPC.Method() {
            @Override
            public void call(JRPC.Response r, Object params) throws JRPC.Error {
                PhoneNotification found = null;

                String key = (String)((Map<String, Object>)params).get("key");

                synchronized (notifications) {
                    for (PhoneNotification notification : notifications) {
                        if (notification.getKey().equals(key)) {
                            found = notification;
                            break;
                        }
                    }
                }

                if (found != null) {
                    found.setVisible(false);
                    if (listener != null) {
                        listener.onNotification(found);
                    }
                }
            }
        });
    }

    public List<PhoneNotification> getNotifications() {
        List<PhoneNotification> visible = new ArrayList<>();
        synchronized (notifications) {
            for (PhoneNotification notification : notifications) {
                if (notification.isVisible()) {
                    visible.add(notification);
                }
            }
        }
        return visible;
    }

    public void clear() {
        synchronized (notifications) {
            for (PhoneNotification notification : notifications) {
                notification.setVisible(false);
                if (listener != null) {
                    listener.onNotification(notification);
                }
            }
            notifications.clear();
        }
    }

    public void setListener(IUpdateListener listener) {
        this.listener = listener;
    }
}

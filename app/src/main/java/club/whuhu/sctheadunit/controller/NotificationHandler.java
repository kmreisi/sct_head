package club.whuhu.sctheadunit.controller;


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
        private final long id;
        private String title;
        private String text;
        private String iconMd5;
        private List<Action> actions;
        private boolean visible;

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
                data.put("id", parent.id);
                data.put("title", getTitle());

                Controller.getInstance().getEventJrpc().send(new JRPC.Request("notification_action", data, null, null));
            }
        }

        public PhoneNotification(Object data) {
            Map<String, Object> params = (Map<String, Object>) data;
            id = (long) params.get("id");
            load(data);
        }

        public void load(Object data) {
            Map<String, Object> params = (Map<String, Object>) data;
            setTitle((String) params.get("title"));
            setText((String) params.get("text"));
            setIconMd5((String) params.get("icon_md5"));
            setActions((List<Object>) params.get("actions"));
            setVisible(true);
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

        public long getId() {
            return id;
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
                if (o1.getId() == o2.getId()) return 0;
                if (o1.getId() > o2.getId()) return 1;
                return -1;
            }
        });


        controller.clientEvent.getJrpc().register("notification", new JRPC.Method() {
            @Override
            public void call(JRPC.Response r, Object params) throws JRPC.Error {
                PhoneNotification notification = new PhoneNotification(params);
                notifications.remove(notification);
                notifications.add(notification);
                if (listener != null) {
                    listener.onNotification(notification);
                }
            }
        });

        controller.clientEvent.getJrpc().register("notification_removed", new JRPC.Method() {
            @Override
            public void call(JRPC.Response r, Object params) throws JRPC.Error {
                PhoneNotification found = null;

                long id = (long)((Map<String, Object>)params).get("id");

                for (PhoneNotification notification : notifications) {
                    if(notification.getId() == id) {
                        found = notification;
                        break;
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
        return Arrays.asList(notifications.toArray(new PhoneNotification[0]));
    }

    public void setListener(IUpdateListener listener) {
        this.listener = listener;
    }
}

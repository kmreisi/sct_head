package club.whuhu.sctheadunit.controller;

import android.app.Activity;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import club.whuhu.jrpc.JRPC;
import club.whuhu.sctheadunit.Dashboard;
import club.whuhu.sctheadunit.ui.UiList;

public class GenAppHandler {

    public static interface IResult {
        void onResult(Object object);
    }

    private final Controller controller;


    public static class GenScope extends UiList.Scope.IUpdater {

        @Override
        public Object getKey(Object data) {
            if (data instanceof MenuItem) {
                return ((MenuItem) data).getTitle();
            }
            return null;
        }

        @Override
        public void load(UiList.Entry entry, Object data) {
            if (data instanceof MenuItem) {
                MenuItem item = (MenuItem) data;
                entry.setTitle(item.getTitle());
                entry.setText(item.getText());
                entry.setIconMd5(item.getIconMd5());
                entry.setOnClick(item);
            }
        }
    }

    public class MenuItem implements UiList.Entry.IOnClick {
        private final String title;
        private final String text;
        private final String iconMd5;
        private final String appId;
        private final String cmd;
        private final String cmd2;
        private final Object data;
        private final Activity activity;

        MenuItem(Activity activity, Object param) {
            this.activity = activity;
            Map<String, Object> data = (Map<String, Object>) param;
            this.title = (String) data.get("title");
            this.text = (String) data.get("text");
            this.iconMd5 = (String) data.get("icon_md5");
            this.appId = (String) data.get("app_id");
            this.cmd = (String) data.get("cmd");
            this.cmd2 = (String) data.get("cmd2");
            this.data = data.get("data");
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

        private void doCommand(final UiList.Scope scope, final UiList.Entry entry, final String cmd) {
            final Map<String, Object> data = new HashMap<>();
            data.put("cmd", cmd);
            data.put("data", this.data);

            controller.getEventJrpc().send(new JRPC.Request("ui:" + appId, data, new JRPC.Request.CallbackResponse() {
                @Override
                public void call(final Object params) {
                    final List<MenuItem> items = new ArrayList<>();
                    if (params instanceof List) {
                        for (Object app : ((List<Object>) params)) {
                            items.add(new MenuItem(activity, app));
                        }
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                UiList.Scope menu = scope.child(entry, new ArrayList<UiList.Entry>(), new GenScope());
                                for (MenuItem item : items) {
                                    menu.update(item);
                                }
                            }
                        });
                    } else {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (params instanceof Map) {
                                    if ("dash".equals(((Map) params).get("next"))) {
                                        activity.startActivity(new Intent(activity, Dashboard.class));
                                        return;
                                    }
                                }

                                scope.back();
                            }
                        });
                    }
                }
            }, null));
        }

        @Override
        public void clicked(final UiList.Scope scope, final UiList.Entry entry) {
            doCommand(scope, entry, cmd);
        }

        @Override
        public boolean longClicked(final UiList.Scope scope, final UiList.Entry entry) {
            if (cmd2 == null) {
                return false;
            }
            doCommand(scope, entry, cmd2);
            return true;
        }
    }

    public GenAppHandler(Controller controller) {
        this.controller = controller;
    }

    public void getApps(final Activity activity, final IResult result) {
        controller.getEventJrpc().send(new JRPC.Request("ui:get_apps", null, new JRPC.Request.CallbackResponse() {
            @Override
            public void call(Object params) {
                List<MenuItem> items = new ArrayList<>();
                if (params instanceof List) {
                    for (Object app : ((List<Object>) params)) {
                        items.add(new MenuItem(activity, app));
                    }
                }
                result.onResult(items);
            }
        }, null));
    }
}

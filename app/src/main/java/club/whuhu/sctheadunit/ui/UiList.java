package club.whuhu.sctheadunit.ui;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.media.DrmInitData;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import club.whuhu.sctheadunit.IconCache;
import club.whuhu.sctheadunit.R;
import club.whuhu.sctheadunit.controller.Controller;

public class UiList {


    public static class Entry {

        public static interface IOnClick {
            public void clicked(Scope scope, Entry entry);

            public boolean longClicked(Scope scope, Entry entry);
        }

        private final Object key;
        private String title;
        private String text;
        private String iconMd5;
        private View view;
        private IOnClick onClick = null;

        private Object data;

        public Entry(Object key) {
            this.key = key;
        }

        public Entry(Object key, String title, String text, IOnClick onClick) {
            this.key = key;
            this.title = title;
            this.text = text;
            this.onClick = onClick;
        }

        public Object getKey() {
            return key;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setIconMd5(String iconMd5) {
            this.iconMd5 = iconMd5;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public String getText() {
            return text;
        }

        public String getIconMd5() {
            return iconMd5;
        }

        public View getView() {
            return view;
        }

        public void setView(View view) {
            this.view = view;
        }

        public void setOnClick(IOnClick onClick) {
            this.onClick = onClick;
        }

        public void update() {
            try {
                TextView title = (TextView) view.findViewById(R.id.title);
                TextView text = (TextView) view.findViewById(R.id.text);
                final ImageView icon = (ImageView) view.findViewById(R.id.icon);

                // Populate the data into the template view using the data object
                title.setText(getTitle());
                text.setText(getText());
                String iconMd5 = getIconMd5();
                icon.setVisibility(iconMd5 == null ? View.INVISIBLE : View.VISIBLE);
                if (iconMd5 != null) {
                    icon.setImageBitmap(IconCache.getInstance().getIcon(getIconMd5(), new IconCache.IGotIcon() {
                        @Override
                        public void call(Bitmap update) {
                            icon.setImageBitmap(update);
                        }
                    }));
                }
            } catch (Exception e) {
                // not visible anymore, this is a dirty HACK
                view = null;
            }
        }

        public List<Entry> getChildren() {
            return null;
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

        public void onClick(Scope scope) {
            if (onClick != null) {
                onClick.clicked(scope, this);
            }
        }

        public boolean onLongClick(Scope scope) {
            if (onClick != null) {
                return onClick.longClicked(scope, this);
            }

            return false;
        }
    }

    public static class EntryDescriptorAdapter extends ArrayAdapter<Entry> {
        public EntryDescriptorAdapter(Context context, List<Entry> data) {
            super(context, 0, data);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            // Check if an existing view is being reused, otherwise inflate the view
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.listitem_entry, parent, false);
            }

            // Populate view
            Entry entry = getItem(position);
            if (entry == null) {
                return null;
            }

            entry.setView(view);

            TextView title = (TextView) view.findViewById(R.id.text);
            title.setText(entry.getSpannable());

            final ImageView icon = (ImageView) view.findViewById(R.id.icon);
            icon.setImageBitmap(IconCache.getInstance().getIcon(entry.getIconMd5(), new IconCache.IGotIcon() {
                @Override
                public void call(Bitmap update) {
                    icon.setImageBitmap(update);
                }
            }));

            return view;
        }
    }

    public static class Scope {
        private final ListView list;
        private final Context context;
        private final Scope parent;
        private final EntryDescriptorAdapter adapter;
        private final IUpdater updater;
        private final Entry clicked;

        private Scope child;

        public static abstract class IUpdater {

            public abstract Object getKey(Object data);

            public boolean isEqual(Entry entry, Object key) {
                Object ownKey = entry.getKey();
                return ownKey != null && ownKey.equals(key);

            }

            public boolean filter(Entry e, Object data) {
                return false;
            }

            public abstract void load(Entry entry, Object data);

            public void dispose() {

            }

            public int compare(Entry a, Entry b) {
                Object keyA = a.getKey();
                Object keyB = b.getKey();
                if (keyA == keyB) {
                    return 0;
                }

                if (keyA instanceof Long && keyB instanceof Long) {
                    Long o1 = (Long) keyA;
                    Long o2 = (Long) keyB;
                    if (o1 == o2) return 0;
                    if (o1 > o2) return 1;
                    return -1;
                }

                if (keyA instanceof String && keyB instanceof String) {
                    String o1 = (String) keyA;
                    String o2 = (String) keyB;
                    return ((String) o1).compareTo(o2);
                }

                return -1;
            }
        }


        private Scope(Scope parent, Entry clicked, ListView list, Context context, List<Entry> data, IUpdater updater) {
            this.parent = parent;
            this.clicked = clicked;
            this.list = list;
            this.context = context;
            this.updater = updater;
            this.adapter = new EntryDescriptorAdapter(context, data);
            show();
        }

        public Scope(final ListView list, Context context, IUpdater updater) {
            this(null, null, list, context, new ArrayList<Entry>(), updater);
        }

        public void clear() {
            adapter.clear();
        }

        public void update(Object data) {
            if (updater == null) {
                return;
            }

            Object key = updater.getKey(data);
            if (key == null) {
                return;
            }

            // check if entry is already existent
            for (int pos = 0; pos < adapter.getCount(); pos++) {
                Entry entry = adapter.getItem(pos);
                if (updater.isEqual(entry, key)) {
                    if (updater.filter(entry, data)) {
                        adapter.remove(entry);
                        return;
                    }
                    updater.load(entry, data);
                    entry.update();
                    return;
                }
            }

            // create new entry
            Entry entry = new Entry(key);
            if (updater.filter(entry, data)) {
                return;
            }

            updater.load(entry, data);

            adapter.add(entry);
            adapter.sort(new Comparator<Entry>() {
                @Override
                public int compare(Entry o1, Entry o2) {
                    return updater.compare(o1, o2);
                }
            });
        }

        public void show() {
            this.list.setAdapter(adapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Entry entry = (Entry) list.getItemAtPosition(position);
                    entry.onClick(Scope.this);
                }
            });

            list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    Entry entry = (Entry) list.getItemAtPosition(position);
                    return entry.onLongClick(Scope.this);
                }
            });
        }

        public boolean back() {
            if (parent == null) {
                return false;
            }

            if (updater != null) {
                updater.dispose();
            }
            parent.child = null;

            parent.show();

            if (clicked != null) {
                parent.select(clicked);
            }
            return true;
        }

        public void select(Entry entry) {
            int pos = adapter.getPosition(entry);
            list.setSelection(pos);
        }

        public void selectByKey(Object key) {
            if (updater != null) {
                for (int pos = 0; pos < adapter.getCount(); pos++) {
                    Entry entry = adapter.getItem(pos);
                    if (updater.isEqual(entry, key)) {
                        select(entry);
                        return;
                    }
                }
            }
        }

        public Scope child(Entry clicked, List<Entry> data, IUpdater updater) {
            child = new Scope(this, clicked, list, context, data, updater);
            return child;
        }

        public boolean handleBack() {
            if (child != null) {
                return child.handleBack();
            }

            if (parent != null) {
                back();
                return true;
            }

            return false;
        }
    }
}

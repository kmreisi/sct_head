package club.whuhu.sctheadunit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationHandler {

    private final List<NotificationDescriptor> list = new ArrayList<>();
    private final Dashboard dashboard;

    public NotificationHandler(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public void update(Object params) {
        // create new Notification Descriptor
        if (!(params instanceof Map)) {
            // THIS SHOULD NOT HAPPEN!
            return;
        }

        Map<String, Object> data = (Map<String, Object>) params;

        NotificationDescriptor descriptor = new NotificationDescriptor((long) data.get("id"), (String) data.get("title"), (String) data.get("text"), (String) data.get("icon"));

        // search for an entry with the ID
        int pos = 0;
        NotificationDescriptor found = null;
        for (int i = 0; i < list.size(); ++i) {
            if (list.get(i).getId() == descriptor.getId()) {
                pos = i;
                found = list.get(i);
                break;
            }
        }

        if (found != null) {

            // update element
            list.set(pos, descriptor);
        } else {
            list.add(descriptor);
        }

        dashboard.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                show();
            }
        });
    }

    public void show() {
        final ListView listView = (ListView) dashboard.findViewById(R.id.notification_list);
        final NotificationDescriptorAdapter adapter = new NotificationDescriptorAdapter(dashboard, list);
        listView.setAdapter(adapter);
    }

    public static class NotificationDescriptor {

        private final long id;


        private final String title;
        private final String text;

        private final Bitmap icon;

        public NotificationDescriptor(long id, String title, String text, String icon) {
            this.id = id;
            this.title = title;
            this.text = text;

            if (icon != null) {
                Bitmap received = null;
                try {
                    byte[] byteArray = Base64.decode(icon, Base64.DEFAULT);
                    received = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                } catch (Exception e) {

                    e.printStackTrace();
                }
                this.icon = received;
            } else {
                this.icon = null;
            }

        }

        public long getId() {
            return id;
        }

        public String getName() {
            return title;
        }

        public String getDescription() {
            return text;
        }

        public Bitmap getIcon() {
            return icon;
        }
    }

    public static class NotificationDescriptorAdapter extends ArrayAdapter<NotificationDescriptor> {
        public NotificationDescriptorAdapter(Context context, List<NotificationDescriptor> notifications) {
            super(context, 0, notifications);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            NotificationDescriptor notification = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_notification, parent, false);
            }
            // Lookup view for data population
            TextView textName = (TextView) convertView.findViewById(R.id.textName);
            TextView textLocation = (TextView) convertView.findViewById(R.id.textDescription);
            ImageView imageView = (ImageView) convertView.findViewById(R.id.imageNoti);
            // Populate the data into the template view using the data object
            textName.setText(notification.getName());
            textLocation.setText(notification.getDescription());
            if (notification.getIcon() != null) {
                imageView.setImageBitmap(notification.getIcon());
            }
            // Return the completed view to render on screen
            return convertView;
        }
    }


}

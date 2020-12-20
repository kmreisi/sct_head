package club.whuhu.sctheadunit;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import club.whuhu.sctheadunit.jrpc.JRPC;

public class Navigate extends AppCompatActivity {

    public static class LocationDescriptor {
        private String name;
        private String uri;

        public String getName() {
            return  name;
        }

        public String getUri() {
            return uri;
        }


        public LocationDescriptor(String name, String uri) {
            this.name = name;
            this.uri = uri;
        }
    }


    public static class LocationDescriptorAdapter extends ArrayAdapter<LocationDescriptor> {
        public LocationDescriptorAdapter(Context context, List<LocationDescriptor> locations) {
            super(context, 0, locations);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            LocationDescriptor location = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_locations, parent, false);
            }
            // Lookup view for data population
            TextView textName = (TextView) convertView.findViewById(R.id.textName);
            TextView textLocation = (TextView) convertView.findViewById(R.id.textLocation);
            // Populate the data into the template view using the data object
            textName.setText(location.getName());
            textLocation.setText(location.getUri());
            // Return the completed view to render on screen
            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigate);

        final ListView listView = (ListView) findViewById(R.id.navigate_locations_list);
        final List<LocationDescriptor> array = new ArrayList<>();
        final LocationDescriptorAdapter adapter = new LocationDescriptorAdapter(this, array);

        final Map<String,String> dummy = new HashMap<>();
        dummy.put("dummy", "dummy");

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LocationDescriptor descriptor = (LocationDescriptor) listView.getItemAtPosition(position);

                JRPC jrpc = Dashboard.getJrpc();
                if (jrpc != null) {
                    Map<String, String> params = new HashMap<>();
                    params.put("name", descriptor.getName());
                    params.put("uri", descriptor.getUri());
                    jrpc.send(new JRPC.Request("navigate", params, new JRPC.Request.CallbackResponse() {
                        @Override
                        public void call(Object params) {

                            System.out.println("NAVIGATE OK " + params);

                        }
                    }, new JRPC.Request.CallbackError() {
                        @Override
                        public void call(JRPC.Error error) {
                            System.out.println("NAVIGATE ERROR " + error);

                        }
                    }) );
                }

                // TODO spawn navigation

                finish();
            }
        });

        // request locations
        JRPC jrpc = Dashboard.getJrpc();
        jrpc.send(new JRPC.Request("get_places", dummy, new JRPC.Request.CallbackResponse() {
            @Override
            public void call(Object params) {
                System.out.println("PLACES: " + params);
                List<Object> places = (List<Object>) params;
                final List<LocationDescriptor> descriptors = new ArrayList<>(places.size());

                for (Object place : places) {
                    Map<String, String> data = (Map<String, String>) place;
                    descriptors.add(new LocationDescriptor(data.get("name"), data.get("uri")));
                }

                runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      adapter.clear();
                                      for (LocationDescriptor descriptor : descriptors) {
                                          adapter.add(descriptor);
                                      }
                                  }
                              }
                );
            }

        }, new JRPC.Request.CallbackError() {
            @Override
            public void call(JRPC.Error error) {

            }
        }));
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}

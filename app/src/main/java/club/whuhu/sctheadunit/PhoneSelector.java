package club.whuhu.sctheadunit;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import club.whuhu.sctheadunit.jrpc.Link;

public class PhoneSelector extends AppCompatActivity {

    private Button scan;


    public static class BluetoothDeviceAdapter extends ArrayAdapter<BluetoothDevice> {
        public BluetoothDeviceAdapter(Context context, List<BluetoothDevice> locations) {
            super(context, 0, locations);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            BluetoothDevice device = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_bluetooth_device, parent, false);
            }
            // Lookup view for data population
            TextView textName = (TextView) convertView.findViewById(R.id.textName);
            TextView textLocation = (TextView) convertView.findViewById(R.id.textLocation);
            // Populate the data into the template view using the data object
            textName.setText(device.getName());
            textLocation.setText(device.getAddress());
            // Return the completed view to render on screen
            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_selector);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 123);
        }

        listView = (ListView) findViewById(R.id.bluetooth_device_list);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice descriptor = (BluetoothDevice) listView.getItemAtPosition(position);
                // update bluetooth device
                Preferences.getInstance().setBluetoothDevice(descriptor);
                // close dialog;
                finish();
            }
        });

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        List<BluetoothDevice> array = new ArrayList<>();

        deviceadapter = new BluetoothDeviceAdapter(this, array);
        listView.setAdapter(deviceadapter);

        scan = (Button) findViewById(R.id.bluetooth_scan);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDiscovery();
            }
        });

        // initially start discovery
        doDiscovery();
    }

    private ListView listView;

    private BluetoothDeviceAdapter deviceadapter;

    private void doDiscovery() {
        if (scan != null) {
            scan.setEnabled(false);
        }

        // add already paired devices
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        // cancel running discovery
        if (adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }

        deviceadapter.clear();

        for (BluetoothDevice device : adapter.getBondedDevices()) {
            deviceadapter.add(device);
        }

        adapter.startDiscovery();
        setTitle("Searching for devices...");
    }

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // check if the item is already in the list
                boolean found = false;
                for (int pos = 0; pos < deviceadapter.getCount(); pos++) {
                    BluetoothDevice ex = deviceadapter.getItem(pos);
                    if (ex.getAddress().equals(device.getAddress())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            deviceadapter.add(device);
                        }});
                }

                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (scan != null) {
                            scan.setEnabled(true);
                        }
                    }
                });
                String strSelectDevice = getIntent().getStringExtra("select_device");
                if (strSelectDevice == null) {
                    strSelectDevice = "Select a device to connect";
                }
                setTitle(strSelectDevice);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}

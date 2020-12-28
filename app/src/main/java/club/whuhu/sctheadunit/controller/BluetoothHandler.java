package club.whuhu.sctheadunit.controller;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

public class BluetoothHandler {

    public static interface IBlueUpdate {
        void stateChanged(String state, boolean active);
        void detected(BluetoothDevice device);
    }

    private final List<BluetoothDevice> devices = new ArrayList<>();
   private IBlueUpdate listener = null;

   private final AppCompatActivity activity;


    public BluetoothHandler(AppCompatActivity activity) {
        this.activity = activity;

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.registerReceiver(mReceiver, filter);
    }

    public  void dispose() {
        activity.unregisterReceiver(mReceiver);
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

                for (BluetoothDevice existing : devices) {
                    if (device.getAddress().equals(existing)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    if (listener != null) {
                        listener.detected(device);
                    }
                }

                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (listener != null) {
                    listener.stateChanged("Finished", false);
                }
            }
        }
    };

    public void scan() {
        if (listener != null) {
            listener.stateChanged("Scanning...", true);
        }

        // cancel running discovery
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }

        devices.clear();

        // start
        adapter.startDiscovery();
   }

    public List<BluetoothDevice> getDevices() {
        List<BluetoothDevice> full = new ArrayList<>(devices);
        // add already paired devices
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        for (BluetoothDevice device : adapter.getBondedDevices()) {
            full.add(device);
        }
        return full;
    }

    public void setListener(IBlueUpdate listener) {
        this.listener = listener;
    }
}

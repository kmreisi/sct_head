package club.whuhu.sctheadunit.controller;

import android.bluetooth.BluetoothDevice;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;

public class BluetoothHandler {

    public static interface IBlueUpdate {
        void update(BluetoothDevice device);

        void onStatusChanged(String status);

        void onPaired(BluetoothDevice device);
    }

    private final SortedSet<BluetoothDevice> devices;
    private final SortedSet<BluetoothDevice> discovered;
    private final AppCompatActivity activity;
    private final IBlueUpdate listener;
    private BluetoothDevice paring;
    private boolean scanning;

    private boolean disposed = false;

    public boolean isAvailable(BluetoothDevice device) {
        String name = device.getName();
        if (name == null || name.isEmpty()) {
            return false;
        }

        synchronized (devices) {
            for (BluetoothDevice entry : devices) {
                if (entry.getAddress().equals(device.getAddress())) {
                    return true;
                }
            }
        }
        return false;
    }

    public BluetoothHandler(AppCompatActivity activity, IBlueUpdate listener) {
        Comparator<BluetoothDevice> comp = new Comparator<BluetoothDevice>() {
            @Override
            public int compare(BluetoothDevice o1, BluetoothDevice o2) {
                return o1.getAddress().compareTo(o2.getAddress());
            }
        };

        devices = new TreeSet<>(comp);
        discovered = new TreeSet<>(comp);

        this.activity = activity;
        this.listener = listener;

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        activity.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.registerReceiver(mReceiver, filter);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        // Initially read all bond devices
        for (BluetoothDevice device : adapter.getBondedDevices()) {
            devices.add(device);
        }
    }

    public void dispose() {
        activity.unregisterReceiver(mReceiver);
        disposed = true;
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
                discovered.add(device);
                devices.add(device);
                listener.update(device);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                synchronized (BluetoothHandler.this) {
                    if (paring != null && device.getAddress().equals(paring.getAddress())) {
                        switch (device.getBondState()) {
                            case BluetoothDevice.BOND_BONDED:
                                paring = null;
                                listener.onStatusChanged("Paired with " + device.getName() + ".");
                                listener.onPaired(device);
                                break;
                            case BluetoothDevice.BOND_NONE:
                                paring = null;
                                listener.onStatusChanged("Failed to pair with " + device.getName() + ".");
                                break;
                            case BluetoothDevice.BOND_BONDING:
                                listener.onStatusChanged("Requesting to pair, please accept here and on " + device.getName() + ".");
                                break;
                        }
                    }
                    if (paring == null) {
                        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                            // device was unpaired, remove it if not available anymore
                            if (!discovered.contains(device)) {
                                devices.remove(device);
                            }
                        }
                        // we are not paring request new scanning
                        scanAfterDelay();
                    }
                }
                listener.update(device);

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                synchronized (this) {
                    if (paring == null) {
                        listener.onStatusChanged("Scanning finished.");
                    }
                    scanning = false;
                }
                scanAfterDelay();
            }
        }
    };

    private void scanAfterDelay() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                scan();
            }
        }, 3000);

    }

    public void scan() {
        synchronized (this) {
            if (disposed) {
                return;
            }
            if (paring != null) {
                return;
            }
            if (scanning) {
                return;
            }
            scanning = true;
        }

        // cancel running discovery
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }

        // remove all not bond devices which were not detected
        List<BluetoothDevice> toRemove = new ArrayList<>();
        if (!discovered.isEmpty()) {
            for (BluetoothDevice device : devices) {
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    if (!discovered.contains(device)) {
                        toRemove.add(device);
                    }
                }
            }
        }
        for (BluetoothDevice device : toRemove) {
            devices.remove(device);
            listener.update(device);
        }

        // clear last discovered
        discovered.clear();

        listener.onStatusChanged("Scanning for devices... ");

        adapter.startDiscovery();
    }

    public List<BluetoothDevice> getDevices() {
        return Arrays.asList(devices.toArray(new BluetoothDevice[devices.size()]));
    }

    public void pair(BluetoothDevice device) {

        try {
            synchronized (this) {
                if (paring != null) {
                    // only allowed to pair one!
                    return;
                }
                paring = device;
                listener.onStatusChanged("Paring with " + device.getName() + "... ");
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter.isDiscovering()) {
                    adapter.cancelDiscovery();
                }
            }

            // init pair function (using refection since this not available until API LVL19)
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {

        }
    }

    public void unPair(BluetoothDevice device) {
        synchronized (this) {
            if (paring != null) {
                // only allowed while not paring!
                return;
            }
        }
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {

        }
    }
}

package club.whuhu.sctheadunit;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashMap;

import club.whuhu.jrpc.JRPC;
import club.whuhu.sctheadunit.controller.BluetoothHandler;
import club.whuhu.sctheadunit.controller.Controller;
import club.whuhu.sctheadunit.ui.UiList;

public class ConnectActivity extends AppCompatActivity {


    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished

    private BluetoothHandler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        getWindow().setAttributes(attributes);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        setContentView(R.layout.activity_connect);

        Controller.getInstance().init(this);

        final TextView message = (TextView) findViewById(R.id.message);
        final TextView status = (TextView) findViewById(R.id.status);

        // Request coarse location permission, this is required in order to use bluetooth!
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 123);
        }

        // Start bluetooth if not enabled yet
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        final UiList.Scope deviceList = new UiList.Scope((ListView) findViewById(R.id.devicelist), this, new UiList.Scope.IUpdater() {
            @Override
            public Object getKey(Object data) {
                if (data instanceof BluetoothDevice) {
                    return ((BluetoothDevice) data).getAddress();
                }
                return null;
            }

            @Override
            public void load(UiList.Entry entry, Object data) {
                if (data instanceof BluetoothDevice) {
                    final BluetoothDevice device = (BluetoothDevice) data;
                    entry.setData(data);
                    entry.setTitle(getTitle(device));
                    entry.setText(device.getAddress());
                    entry.setIconMd5(null);
                    entry.setOnClick(new UiList.Entry.IOnClick() {

                        @Override
                        public void clicked(UiList.Scope scope, UiList.Entry entry) {
                            // is device bond? => send paring request
                            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                                handler.pair(device);
                            } else {
                                Preferences.getInstance().setBluetoothDevice(device);
                                message.setText(getMessage());
                            }
                        }

                        @Override
                        public boolean longClicked(UiList.Scope scope, UiList.Entry entry) {
                            handler.unPair(device);
                            Preferences.getInstance().clearSelectedDevice();
                            return true;
                        }
                    });
                }
            }

            @Override
            public boolean filter(UiList.Entry e, Object data) {
                return !handler.isAvailable((BluetoothDevice) data);
            }
        });

        handler = new BluetoothHandler(this, new BluetoothHandler.IBlueUpdate() {
            @Override
            public void update(final BluetoothDevice device) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        deviceList.update(device);
                    }
                });
            }

            @Override
            public void onStatusChanged(final String text) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        status.setText(text);
                        message.setText(getMessage());
                    }
                });
            }

            @Override
            public void onPaired(final BluetoothDevice device) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Preferences.getInstance().setBluetoothDevice(device);
                        message.setText(getMessage());
                    }
                });
            }
        });

        for (BluetoothDevice device : handler.getDevices()) {
            deviceList.update(device);
        }

        message.setText(getMessage());
        handler.scan();
    }

    @Override
    protected void onStop() {
        handler.dispose();
        super.onStop();
    }

    private String getTitle(BluetoothDevice device) {
        BluetoothDevice selected = Preferences.getInstance().getBluetoothDevice();
        if (selected != null && selected.getAddress().equals(device.getAddress())) {
            return "Selected " + device.getName();
        }

        return  device.getBondState() + " -  " +  device.getName();
    }

    private String getText(BluetoothDevice device) {
        return device.getName();
    }

    private String getMessage() {
        String status;
        BluetoothDevice selected = Preferences.getInstance().getBluetoothDevice();
        if (selected == null) {
            return "No Selection\nPlease start the phone app and select your device";
        } else {
            if (Controller.State.CONNECTED != Controller.getInstance().getState()) {
                return "Connecting to " + selected.getName() + "\nWaiting for device, ensure the phone app is running or select another device";
            } else {
                return "Connected to " + selected.getName() + "\nTo change the selection, ensure the phone app is running and update the selection";
            }
        }
    }
}

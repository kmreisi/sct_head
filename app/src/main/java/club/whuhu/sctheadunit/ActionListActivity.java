package club.whuhu.sctheadunit;

import android.content.Intent;
import android.media.DrmInitData;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import club.whuhu.sctheadunit.controller.BluetoothHandler;
import club.whuhu.sctheadunit.controller.Controller;
import club.whuhu.sctheadunit.controller.GenAppHandler;
import club.whuhu.sctheadunit.ui.UiList;

public class ActionListActivity extends AppCompatActivity {

    private ActionListActivity activity;
    private ListView listView;
    private UiList.Scope actionsScope;
    private BluetoothHandler bluetoothHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = this;
        bluetoothHandler = new BluetoothHandler(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        getWindow().setAttributes(attributes);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        setContentView(R.layout.activity_notification_list);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 123);
        }

        listView = (ListView) findViewById(R.id.notification_list);

        final List<UiList.Entry> actions = new ArrayList<>();

        actions.add(new UiList.Entry(null, "Configure", "", new UiList.Entry.IOnClick() {
            @Override
            public void clicked(UiList.Scope scope, UiList.Entry entry) {
                showBluetoothConfig(entry, scope);
            }

            @Override
            public boolean longClicked(UiList.Scope scope, UiList.Entry entry) {
                return false;
            }
        }));

        actionsScope = new UiList.Scope(listView, this, actions, new GenAppHandler.GenScope());

        Controller.getInstance().getApps(activity, new GenAppHandler.IResult() {
            @Override
            public void onResult(final Object object) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (object instanceof List<?>) {
                            for (Object entry : ((List<Object>) object)) {
                                actionsScope.update(entry);
                            }
                        }
                    }
                });
            }
        });
    }

    private void showBluetoothConfig(UiList.Entry clicked, UiList.Scope parent){
        final List<UiList.Entry> actions = new ArrayList<>();

        final UiList.Entry scan = new UiList.Entry(null, "Scan", "", new UiList.Entry.IOnClick() {
            @Override
            public void clicked(UiList.Scope scope, UiList.Entry entry) {
                bluetoothHandler.scan();
            }

            @Override
            public boolean longClicked(UiList.Scope scope, UiList.Entry entry) {
                return false;
            }
        });

        actions.add(scan);

        final UiList.Scope bluetooth = parent.child(clicked, actions, new UiList.Scope.IUpdater() {
            @Override
            public Object getKey(Object data) {
                if (data instanceof  BluetoothDevice) {
                    return ((BluetoothDevice) data).getAddress();
                }
                return null;
            }

            @Override
            public void load(UiList.Entry entry, Object data) {
                if (data instanceof  BluetoothDevice) {
                    final BluetoothDevice device = (BluetoothDevice) data;
                    entry.setData(data);
                    entry.setTitle(device.getName());
                    entry.setText(device.getAddress());
                    entry.setIconMd5(null);
                    entry.setOnClick(new UiList.Entry.IOnClick() {
                        @Override
                        public void clicked(UiList.Scope scope, UiList.Entry entry) {
                            Preferences.getInstance().setBluetoothDevice(device);
                            scope.back();
                        }

                        @Override
                        public boolean longClicked(UiList.Scope scope, UiList.Entry entry) {
                            return false;
                        }
                    });
                }
            }

            @Override
            public void dispose() {
                bluetoothHandler.setListener(null);
            }
        });

        bluetoothHandler.setListener(new BluetoothHandler.IBlueUpdate() {
            @Override
            public void stateChanged(String state, boolean active) {
            }

            @Override
            public void detected(final BluetoothDevice device) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bluetooth.update(device);
                    }
                } );
            }
        });

        for (BluetoothDevice device : bluetoothHandler.getDevices()) {
            bluetooth.update(device);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothHandler.dispose();
    }

    @Override
    public void onBackPressed() {
        if (actionsScope.handleBack()){
            return;
        }

        startActivity(new Intent(this, Dashboard.class));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}

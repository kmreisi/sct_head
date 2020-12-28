package club.whuhu.sctheadunit;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Preferences implements SharedPreferences.OnSharedPreferenceChangeListener {

    public interface IChangeListener {
        public void bluetoothDeviceChangedListener(BluetoothDevice device);
    }

    public static final String KEY_PHONE_BLUETOOTH_ADDRESS =
            "PHONE_BLUETOOTH_ADDRESS";

    private static Preferences instance;

    private Activity activity = null;
    private final List<IChangeListener> listenerList;


    private Preferences() {
        this.listenerList = new CopyOnWriteArrayList<>();
    }

    public void init(Activity activity) {
        if (this.activity != null) {
            return;
        }
        this.activity = activity;
        getPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    public synchronized static Preferences getInstance() {
        if (instance == null) {
        instance = new Preferences();
        }
        return instance;
    }

    public BluetoothDevice getBluetoothDevice() {
        String addr = getPreferences().getString(KEY_PHONE_BLUETOOTH_ADDRESS, null);

        if (addr != null) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            for (BluetoothDevice device : adapter.getBondedDevices()) {
                if (addr.equals(device.getAddress())) {
                    return device;
                }
            }
        }

        return  null;
    }

    public void setBluetoothDevice(BluetoothDevice device) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(KEY_PHONE_BLUETOOTH_ADDRESS, device.getAddress());
        editor.commit();
    }

    public SharedPreferences getPreferences(){
        return activity.getPreferences(Context.MODE_PRIVATE);
    }

    public void addChangeListener(IChangeListener listener) {
        this.listenerList.add(listener);
    }

    public void removeChangeListener(IChangeListener listener) {
        this.listenerList.remove(listener);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (KEY_PHONE_BLUETOOTH_ADDRESS.equals(key)) {
            final BluetoothDevice device = getBluetoothDevice();
            for (IChangeListener listener: listenerList) {
                listener.bluetoothDeviceChangedListener(device);
            }
        }
    }
}

package club.whuhu.sctheadunit;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;

import club.whuhu.jrpc.Client;
import club.whuhu.jrpc.JRPC;
import club.whuhu.jrpc.Link;

public class Controller  {

    private final Client clientEvent;
    private final Client clientIcon;

    public Controller(Activity activity, Link.ILinkStateListener linkStateListener) {
        Preferences.init(activity);

        Client.IGetBluetoothDevice handler = new Client.IGetBluetoothDevice() {
            @Override
            public BluetoothDevice getDevice() {
                return Preferences.getInstance().getBluetoothDevice();
            }
        };

        clientEvent = new Client(Link.ANDROID_CAR_SERVICE_EVENT, linkStateListener, handler);
        clientIcon = new Client(Link.ANDROID_CAR_SERVICE_ICON, linkStateListener, handler);

        Preferences.getInstance().addChangeListener(new Preferences.IChangeListener() {
            @Override
            public void bluetoothDeviceChangedListener(BluetoothDevice device) {
                clientEvent.disconnect();
                clientIcon.disconnect();
            }
        });

        // start thread
        clientEvent.start();
        clientIcon.start();
    }

    public void dispose() {
        clientEvent.stop();
        clientIcon.stop();
    }

    public JRPC getEventJrpc() {
        return clientEvent.getJrpc();
    }
    public JRPC getIconJrpc() {
        return clientIcon.getJrpc();
    }
}

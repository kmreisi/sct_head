package club.whuhu.sctheadunit;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;

import club.whuhu.jrpc.JRPC;
import club.whuhu.jrpc.Link;

public class Controller  {

    private final Link link;

    private boolean running = true;

    public Controller(Activity activity, Link.ILinkStateListener linkStateListener) {
        Preferences.init(activity);
        link = new Link(linkStateListener);

        Preferences.getInstance().addChangeListener(new Preferences.IChangeListener() {
            @Override
            public void bluetoothDeviceChangedListener(BluetoothDevice device) {
                 link.disconnect();
            }
        });

        // start thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                loop();
            }
        }).start();
    }

    private void process() {
        // get bluetooth device
        BluetoothDevice device = Preferences.getInstance().getBluetoothDevice();
        if (device== null) {
            return;
        }

        // run link on it
        link.connect(device);
    }


    private void loop() {
        System.out.println("Start controller loop... ");
        while(running) {
            process();
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Finished controller loop... ");
    }

    public void dispose() {
        running = false;
        link.disconnect();
    }

    public JRPC getJrpc() {
        return link.getJrpc();
    }
}

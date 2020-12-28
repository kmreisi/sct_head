package club.whuhu.sctheadunit.controller;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import club.whuhu.jrpc.Client;
import club.whuhu.jrpc.JRPC;
import club.whuhu.jrpc.Link;
import club.whuhu.sctheadunit.Preferences;
import club.whuhu.sctheadunit.ui.UiList;

public class Controller  {

    private static Controller instance;
    public static synchronized Controller getInstance() {
        if (instance == null) {
            instance = new Controller();
        }
        return  instance;
    }

    public static interface IBlueUpdate {
        void stateChanged(String state, boolean active);
        void detected(BluetoothDevice device);
    }

    /* package */ final Client clientEvent;
    /* package */ final Client clientIcon;
    private final NotificationHandler notificationHandler;
    private final GenAppHandler genAppHandler;

    public Controller() {
        Client.IGetBluetoothDevice handler = new Client.IGetBluetoothDevice() {
            @Override
            public BluetoothDevice getDevice() {
                return Preferences.getInstance().getBluetoothDevice();
            }
        };

        Link.ILinkStateListener listener = new Link.ILinkStateListener() {
            @Override
            public void connecting() {
                System.out.println("CONNECTING....");
            }

            @Override
            public void connected() {
                System.out.println("CONNECTED");

            }

            @Override
            public void disconnected() {
                System.out.println("DISCONNECTED!");
            }
        };

        clientEvent = new Client(Link.ANDROID_CAR_SERVICE_EVENT, listener, handler);
        clientIcon = new Client(Link.ANDROID_CAR_SERVICE_ICON, listener, handler);

        Preferences.getInstance().addChangeListener(new Preferences.IChangeListener() {
            @Override
            public void bluetoothDeviceChangedListener(BluetoothDevice device) {
                clientEvent.disconnect();
                clientIcon.disconnect();
            }
        });

        notificationHandler = new NotificationHandler(this);
        genAppHandler = new GenAppHandler(this);

        // start thread
        clientEvent.start();
        clientIcon.start();
    }

    public void dispose() {
        clientEvent.stop();
        clientIcon.stop();
    }


    public List<NotificationHandler.PhoneNotification> getNotifications() {
        return notificationHandler.getNotifications();
    }

    public void setNotificationListener(NotificationHandler.IUpdateListener listener) {
        notificationHandler.setListener(listener);
    }

    public void getApps(Activity activity, GenAppHandler.IResult result) {
        genAppHandler.getApps(activity, result);
    }

    public JRPC getEventJrpc() {
        return clientEvent.getJrpc();
    }
    public JRPC getIconJrpc() {
        return clientIcon.getJrpc();
    }
}

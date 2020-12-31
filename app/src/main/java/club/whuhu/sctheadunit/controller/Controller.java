package club.whuhu.sctheadunit.controller;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import club.whuhu.jrpc.Client;
import club.whuhu.jrpc.JRPC;
import club.whuhu.jrpc.Link;
import club.whuhu.sctheadunit.ConnectActivity;
import club.whuhu.sctheadunit.Dashboard;
import club.whuhu.sctheadunit.IconCache;
import club.whuhu.sctheadunit.Preferences;
import club.whuhu.sctheadunit.Storage;
import club.whuhu.sctheadunit.ui.UiList;

public class Controller  {

    public enum State {
        DISCONNECTED,
        CONNECTED
    }

    private static Controller instance;
    public static synchronized Controller getInstance() {
        if (instance == null) {
            instance = new Controller();
        }
        return  instance;
    }

    public interface IUpdateListener {
        void stateChanged(State state);
    }

    private ConnectActivity activity;

    private Client clientEvent;
    private Client clientIcon;

    private NotificationHandler notificationHandler;
    private GenAppHandler genAppHandler;

    private State state = State.DISCONNECTED;
    private IUpdateListener listener;

    public synchronized void init(ConnectActivity activity) {
        if (this.activity != null) {
            return;
        }

       this.activity = activity;

       Preferences.getInstance().init(activity);
       Storage.getInstance().init(activity);
       IconCache.getInstance().init(activity);

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
               onStateChanged(State.CONNECTED);
            }

            @Override
            public void disconnected() {
                onStateChanged(State.DISCONNECTED);
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

        // start client threads
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

    public State getState() {
        return  state;
    }

    private void onUpdateState(State state) {
        if (this.state == state) {
            return;
        }

        this.state = state;

        if (this.listener != null) {
            listener.stateChanged(state);
        }

        if (state == State.CONNECTED) {
            activity.startActivity(new Intent(activity, Dashboard.class));
        } else {
            notificationHandler.clear();
            activity.startActivity(new Intent(activity, ConnectActivity.class));
        }
    }

    private synchronized void onStateChanged(State state) {
        // check if both are connected, otherwise wait for the second
        if (state == State.CONNECTED && clientEvent.isConnected() && clientIcon.isConnected()) {
            onUpdateState(State.CONNECTED);
            return;
        }

        if (state == State.DISCONNECTED) {
            onUpdateState(State.DISCONNECTED);
        }
    }

    public void setListener(IUpdateListener listener) {
        this.listener = listener;
    }
}

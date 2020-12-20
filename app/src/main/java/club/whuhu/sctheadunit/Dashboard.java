package club.whuhu.sctheadunit;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import club.whuhu.jrpc.JRPC;
import club.whuhu.jrpc.Link;

public class Dashboard extends AppCompatActivity {

    private final Link.ILinkStateListener linkStateListener = new Link.ILinkStateListener() {
        @Override
        public void connecting() {
                System.out.println("Connecting...");
        }

        @Override
        public void connected() {
            System.out.println("Connected...");

        }

        @Override
        public void disconnected() {
            System.out.println("Disconnected...");

        }
    };

    private static Controller controller;

    private NotificationHandler notificationHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notificationHandler = new NotificationHandler(this);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        getWindow().setAttributes(attributes);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        setContentView(R.layout.activity_dashboard);

        Button dash = (Button) findViewById(R.id.button_dash);
        Button navigate = (Button) findViewById(R.id.button_navigate);

        navigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), Navigate.class));
            }
        });

        Button music = (Button) findViewById(R.id.button_music);
        Button apps = (Button) findViewById(R.id.button_applications);
        Button dial = (Button) findViewById(R.id.button_dial);
        Button cfg = (Button) findViewById(R.id.button_configure);

        cfg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), PhoneSelector.class));
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        controller = new Controller(this, linkStateListener);
        JRPC jrpc = getJrpc();
        jrpc.register("navigation_notification", new JRPC.Method() {
            @Override
            public void call(JRPC.Response r, Object params) throws JRPC.Error {

            }
        });
        jrpc.register("notification", new JRPC.Method() {
            @Override
            public void call(JRPC.Response r, Object params) throws JRPC.Error {
                System.out.println("XXXXXXXXXXXXXXXX EVENT added: ");
                if (notificationHandler != null) {
                    notificationHandler.update(params);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (controller != null) {
            controller.dispose();
        }
    }

    public synchronized static JRPC getJrpc() {
        if (controller == null) {
            return  null;
        }

        return  controller.getJrpc();
    }
}

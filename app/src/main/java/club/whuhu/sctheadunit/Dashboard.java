package club.whuhu.sctheadunit;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import club.whuhu.jrpc.JRPC;
import club.whuhu.jrpc.Link;
import club.whuhu.sctheadunit.controller.Controller;

public class Dashboard extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Preferences.getInstance().init(this);

        Controller.getInstance();

        IconCache.init(this);
        Storage.init(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        getWindow().setAttributes(attributes);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        setContentView(R.layout.activity_dashboard);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            startActivity(new Intent(this, NotificationListActivity.class));
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            startActivity(new Intent(this, ActionListActivity.class));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}

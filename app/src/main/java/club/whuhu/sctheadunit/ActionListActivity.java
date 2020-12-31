package club.whuhu.sctheadunit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;

import java.util.List;

import club.whuhu.sctheadunit.controller.Controller;
import club.whuhu.sctheadunit.controller.GenAppHandler;
import club.whuhu.sctheadunit.ui.UiList;

public class ActionListActivity extends AppCompatActivity {

    private ActionListActivity activity;
    private ListView listView;
    private UiList.Scope actionsScope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = this;

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        getWindow().setAttributes(attributes);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        setContentView(R.layout.activity_list);

        listView = (ListView) findViewById(R.id.notification_list);

        actionsScope = new UiList.Scope(listView, this, new GenAppHandler.GenScope());

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

package systems.movingdata.goremote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Notafacation extends Activity {

    private TextView mTextView;
    String Line1 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        if (intent.hasExtra("test") ){
            Line1 = intent.getStringExtra("test");
        }
        Log.d("Notafacation",intent.getExtras().keySet().toString());

         setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.TestMode);
                mTextView.setText(Line1);
                LinearLayout ButtonLayout = (LinearLayout) stub.findViewById(R.id.ButtonLayout);
                ButtonLayout.setVisibility(View.GONE);
            }
        });
    }
}

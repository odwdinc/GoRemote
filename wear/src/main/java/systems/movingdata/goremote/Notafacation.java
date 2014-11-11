package systems.movingdata.goremote;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.wearable.Wearable;

public class Notafacation extends MyActivity {

    private TextView mTextView;
    DataManager myDataManager;
    Bundle DataBul;
    String TAG = "Notafacation";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        DataBul = intent.getExtras();
        setContentView(R.layout.activity_main);
        myDataManager = new DataManager(this);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.TestMode);
                LinearLayout ButtonLayout = (LinearLayout) stub.findViewById(R.id.ButtonLayout);
                ButtonLayout.setVisibility(View.GONE);
                UpdateGui(DataBul);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        myDataManager.mGoogleApiClient.connect();
    }


    @Override
    protected void onStop() {
        Wearable.MessageApi.removeListener(myDataManager.mGoogleApiClient, myDataManager);
        Wearable.NodeApi.removeListener(myDataManager.mGoogleApiClient, myDataManager);
        myDataManager.mGoogleApiClient.disconnect();
        super.onStop();
    }


    @Override
    void UpdateGui(Bundle data){
        if (data.containsKey("TestMode")){
            mTextView.setText(data.getString("TestMode"));
        }
    }
}

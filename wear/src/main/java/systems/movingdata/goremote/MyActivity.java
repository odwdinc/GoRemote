package systems.movingdata.goremote;

import android.app.Activity;
import android.os.Bundle;

import com.google.android.gms.wearable.Wearable;

/**
 * Created by asprayx on 11/11/2014.
 */
public abstract class MyActivity extends Activity {
    String TAG = "MyActivity";
    DataManager myDataManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myDataManager = new DataManager(this,savedInstanceState);
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


}

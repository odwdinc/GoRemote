package systems.movingdata.goremote;

import android.os.Bundle;

public class MainActivity extends MyActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TAG="MainActivity";
        if (savedInstanceState == null){
            savedInstanceState = new Bundle();
        }
        savedInstanceState.putBoolean("Buttons",true);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        myDataManager.showNotification();
    }







}

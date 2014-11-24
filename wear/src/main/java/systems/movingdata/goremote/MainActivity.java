package systems.movingdata.goremote;

import android.os.Bundle;
import android.view.WindowManager;

public class MainActivity extends MyActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TAG="MainActivity";
        if (savedInstanceState == null){
            savedInstanceState = new Bundle();
        }
        savedInstanceState.putBoolean("Buttons",true);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStart() {
        super.onStart();
        myDataManager.showNotification();
    }







}

package systems.movingdata.goremote;

import android.os.Bundle;

public class Notafacation extends MyActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TAG="Notafacation";
        if (savedInstanceState == null){
            savedInstanceState = new Bundle();
        }
        savedInstanceState.putBoolean("Buttons",false);
        super.onCreate(savedInstanceState);
    }




}

package systems.movingdata.goremote;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by asprayx on 11/11/2014.
 */
public abstract class MyActivity extends Activity {
    String TAG = "MyActivity";
    abstract void UpdateGui(Bundle data);

}

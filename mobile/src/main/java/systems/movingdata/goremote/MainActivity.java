package systems.movingdata.goremote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {


    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;
    private static final String TAG = "CompannonActivity";
    Listeners myListeners;
    Spinner sSpinner;
    int ItemSelected;
    Button ConectSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myListeners = new Listeners();
        ConectSend = (Button)findViewById(R.id.button);
        sSpinner=(Spinner)findViewById(R.id.spinner);

        ConectSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick "+ItemSelected);
                new SendActivityPhoneMessage("onClick",""+ItemSelected).start();
            }

            });
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new ConnectionCallbacks())
                .addOnConnectionFailedListener(new ConnectionFailedListener())
                .build();

        getCurrentSsid();

    }


    @Override
    protected void onNewIntent(Intent intent) {
        Bundle extras = intent.getExtras();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        if (!mResolvingError) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, myListeners);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "Google API Client was connected");
            mResolvingError = false;
            Wearable.MessageApi.addListener(mGoogleApiClient, myListeners);
        }

        @Override
        public void onConnectionSuspended(int i) {
            // empty
        }
    }

    private class ConnectionFailedListener implements
            GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            // empty
        }
    }









    class SendActivityPhoneMessage extends Thread {
        String path;
        String message;

        // Constructor to send a message to the data layer
        SendActivityPhoneMessage(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetLocalNodeResult nodes = Wearable.NodeApi.getLocalNode(mGoogleApiClient).await();
            Node node = nodes.getNode();
            Log.v(TAG, "Activity Node is : "+node.getId()+ " - " + node.getDisplayName());


            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message.getBytes(Charset.forName("UTF-8"))).await();
            if (result.getStatus().isSuccess()) {
                Log.v(TAG, "Activity Message: {" + message + "} sent to: " + node.getDisplayName());
            }
            else {
                // Log an error
                Log.v(TAG, "ERROR: failed to send Activity Message");
            }

        }
    }


    private class Listeners implements MessageApi.MessageListener{
        //TODO
        @Override //MessageListener
        public void onMessageReceived(final MessageEvent messageEvent) {
            Log.d(TAG, "onMessageReceived() A message from watch was received:" + messageEvent
                .getRequestId() + " " + messageEvent.getPath());
            if(messageEvent.getPath().equals("ButtenText")) {
                String Sdata = new String(messageEvent.getData(), Charset.forName("UTF-8"));
                updateMessage(Sdata);
            }else if(messageEvent.getPath().equals("ButtenEnable")) {
                String Sdata = new String(messageEvent.getData(), Charset.forName("UTF-8"));
                Log.d(TAG, "ButtenEnable:" + Sdata);
                if(Sdata.contains("false")){
                    updateEnable(false);
                }else{
                    updateEnable(true);
                }
            }
        }

    }

    private void updateEnable(final boolean b) {
        runOnUiThread(new Runnable() {
            @Override
            public void run () {
                ConectSend.setEnabled(b);
            }
        });

    }

    void updateMessage(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run () {
                ConectSend.setText(message);
            }
        });
    }



    public void getCurrentSsid() {


        final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);


        // List stored networks
        List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
        ArrayList<String> Wifilist = new ArrayList<String>();
        for (WifiConfiguration config : configs) {

            Log.v(TAG, "networkId:" + config.networkId);
            Wifilist.add(config.SSID);
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_activated_1,Wifilist);
        dataAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);
        sSpinner.setAdapter(dataAdapter);
        sSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.v(TAG, "onItemSelected: \n" + i + ":" + l);
                ItemSelected = i;

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.v(TAG, "onNothingSelected: \n");
            }
        });




    }


}

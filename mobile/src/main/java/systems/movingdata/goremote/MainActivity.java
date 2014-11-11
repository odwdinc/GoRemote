package systems.movingdata.goremote;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageApi.SendMessageResult;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;


public class MainActivity extends Activity {


    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;
    private static final String TAG = "CompannonActivity";
    Listeners myListeners;
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String DATA_ACTIVITY_PATH = "/data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myListeners = new Listeners();
        Button send = (Button)findViewById(R.id.button);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new StartWearableActivityTask("TestMode","testing 123").execute();
            }

            });
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new ConnectionCallbacks())
                .addOnConnectionFailedListener(new ConnectionFailedListener())
                .build();
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
            onStartWearableActivity();
        }
    }

    @Override
    protected void onStop() {
        if (!mResolvingError) {
            Wearable.DataApi.removeListener(mGoogleApiClient, myListeners);
            Wearable.MessageApi.removeListener(mGoogleApiClient, myListeners);
            Wearable.NodeApi.removeListener(mGoogleApiClient, myListeners);
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
            Wearable.DataApi.addListener(mGoogleApiClient, myListeners);
            Wearable.MessageApi.addListener(mGoogleApiClient, myListeners);
            Wearable.NodeApi.addListener(mGoogleApiClient, myListeners);
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

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    private class StartWearableActivityTask extends AsyncTask<Void, Void, Void> {
        String ACTIVITY_PATH;
        byte[] ACTIVITY_DATA;
        private StartWearableActivityTask( String PATH, String Data) {
            this.ACTIVITY_PATH = PATH;
            if(Data.length() > 0) {
                this.ACTIVITY_DATA = Data.getBytes(Charset.forName("UTF-8"));
            }else{
                this.ACTIVITY_DATA = new byte[0];
            }
        }

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node, this.ACTIVITY_PATH, this.ACTIVITY_DATA );
            }
            return null;
        }
    }

    /** Sends an RPC to start a fullscreen Activity on the wearable. */
    public void onStartWearableActivity() {
        Log.d(TAG, "Generating RPC");
        connectGoPro();
        // Trigger an AsyncTask that will query for a list of connected nodes and send a
        // "start-activity" message to each connected node.
        new StartWearableActivityTask(START_ACTIVITY_PATH,"").execute();
    }


    public void connectGoPro(){
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", "00101010_11001");
        wifiConfig.preSharedKey = String.format("\"%s\"", "adored20");
        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
//remember id
        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
    }

    private void sendStartActivityMessage(String node, String ACTIVITY_PATH, byte[] by ) {
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, ACTIVITY_PATH, by).setResultCallback(
                new ResultCallback<SendMessageResult>() {
                    @Override
                    public void onResult(SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
        );
    }

    private class Listeners implements DataApi.DataListener,
            MessageApi.MessageListener, NodeApi.NodeListener{

        @Override //DataListener
        public void onDataChanged(DataEventBuffer dataEvents) {
            Log.d(TAG, "onDataChanged: " + dataEvents);
        }

        @Override //NodeListener
        public void onPeerConnected(final Node peer) {
            Log.d(TAG, "onPeerConnected: " + peer);


        }
        @Override
        public void onPeerDisconnected(Node peer) {
            Log.d(TAG, "onPeerDisconnected: " + peer);
        }




        @Override //MessageListener
        public void onMessageReceived(final MessageEvent messageEvent) {
            if (messageEvent.getPath().equals("/remote/1")) {
                Log.d(TAG,"PowerMode");
            }
            else if(messageEvent.getPath().equals("/remote/2")) {
                Log.d(TAG,"SlectMode");
            }
            else if(messageEvent.getPath().equals("/remote/3")) {
                Log.d(TAG,"Record");
            }
            else{
                Log.d(TAG, "onMessageReceived() A message from watch was received:" + messageEvent
                        .getRequestId() + " " + messageEvent.getPath());
            }
        }

    }
}

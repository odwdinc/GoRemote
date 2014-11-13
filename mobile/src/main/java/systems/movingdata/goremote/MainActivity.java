package systems.movingdata.goremote;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageApi.SendMessageResult;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


public class MainActivity extends Activity {


    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;
    private static final String TAG = "CompannonActivity";
    Listeners myListeners;
    Spinner sSpinner;
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String DATA_ACTIVITY_PATH = "/data";
    int netIdBackUp;
    int ItemSelected;
    Button ConectSend;
    JSONObject gpControl;
    JSONObject gpStatus;
    int RecordSend;

    int[] xmode = new int[] {0,10,11,1,12,13,2,3,14};
    int xmodepos = 0;

    Handler handler;
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
                connectWifi(ItemSelected);
                new CheckForConectionTask(ItemSelected).execute();
            }

            });
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new ConnectionCallbacks())
                .addOnConnectionFailedListener(new ConnectionFailedListener())
                .build();
        customHandler = new android.os.Handler();
        getCurrentSsid();

        handler = new Handler();

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
            Wearable.DataApi.removeListener(mGoogleApiClient, myListeners);
            Wearable.MessageApi.removeListener(mGoogleApiClient, myListeners);
            Wearable.NodeApi.removeListener(mGoogleApiClient, myListeners);
            mGoogleApiClient.disconnect();
        }
        connectWifi(netIdBackUp);
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

    private class StartMessageActivityTask extends AsyncTask<Void, Void, Void> {
        String ACTIVITY_PATH;
        byte[] ACTIVITY_DATA;
        private StartMessageActivityTask( String PATH, String Data) {
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

        // Trigger an AsyncTask that will query for a list of connected nodes and send a
        // "start-activity" message to each connected node.
        new StartMessageActivityTask(START_ACTIVITY_PATH,"").execute();
    }


    public void connectWifi(int netId){
        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        final WifiInfo connectionInfo = wifiManager.getConnectionInfo();

        if (connectionInfo != null && !(connectionInfo.getSSID().equals(""))) {
            //if (connectionInfo != null && !StringUtil.isBlank(connectionInfo.getSSID())) {
            if (connectionInfo.getNetworkId() ==  netId){

                return;
            }else{
                netIdBackUp = connectionInfo.getNetworkId();
            }
        }
        wifiManager.disconnect();
        Log.d(TAG, "disconnect");
        ConectSend.setText("Connecting");
        ConectSend.setEnabled(false);
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
        Log.d(TAG, "reconnect");
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
                new SendCommand("http://10.5.5.9/gp/gpControl/command/system/sleep").execute();

            }
            else if(messageEvent.getPath().equals("/remote/2")) {

                xmodepos ++;
                if (xmodepos >= xmode.length){
                    xmodepos = 0;
                }
                Log.d(TAG,"SlectMode:"+xmode[xmodepos]);
                new SendCommand("http://10.5.5.9/gp/gpControl/command/xmode?p="+xmode[xmodepos]).execute();

            }
            else if(messageEvent.getPath().equals("/remote/3")) {
                Log.d(TAG,"Record:"+RecordSend);
                new SendCommand("http://10.5.5.9/gp/gpControl/command/shutter?p="+RecordSend).execute();


            }
            else{
                Log.d(TAG, "onMessageReceived() A message from watch was received:" + messageEvent
                        .getRequestId() + " " + messageEvent.getPath());
            }
        }

    }
    private class CheckForConectionTask extends AsyncTask<Void, Void, Void> {
        int ExpectedID;
        private CheckForConectionTask(int ID) {
            ExpectedID = ID;
        }

        @Override
        protected Void doInBackground(Void... args) {
            checkForConection();
            return null;
        }

        void checkForConection(){

            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            Log.d(TAG, "Connecteding:");

            while (!networkInfo.isConnected()) {
                networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            }

            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Finished(ExpectedID == connectionInfo.getNetworkId());
                    }
                });
            Log.d(TAG, "isConnected:" + connectionInfo.getSSID());
        }
    }

    private class SGoPo extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... args) {
           if(checkForGoPro()) {
               //ToDo
               ProseesStatus();
               handler.postDelayed(SoGo, 1000);

           }
            return null;
        }
    }


    Runnable SoGo = new Runnable() {
        @Override
        public void run() {
            new SGoPo().execute();
        }
    };

    private class SendCommand extends AsyncTask<Void, Void, Void> {
        String url;
        private SendCommand(String url) {
            this.url = url;
        }

        @Override
        protected Void doInBackground(Void... args) {
            Log.d(TAG,"Retreaving:"+url);
            JSONObject jObject = getJson(url);
            if (jObject != null) {
                Log.d(TAG,"recived:"+jObject.toString());
            }else{
                Log.d(TAG,"Error:");
            }
            return null;
        }
    }

    void updateMessage(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run () {
                ConectSend.setText(message);
            }
        });
    }
    boolean checkForGoPro(){
        if (gpControl != null) {
            return  true;
        }
        updateMessage("Looking for Gopro...");

        gpControl = getJson("http://10.5.5.9/gp/gpControl");
        if (gpControl != null) {
                Log.v(TAG, "gpControl: \n" + gpControl.toString());
            new StartMessageActivityTask("/gpControl",gpControl.toString()).execute();
            updateMessage("Found Gopro...");

        }else{
            updateMessage("Not Found, Plese check Network...");
        }
        gpStatus = getJson("http://10.5.5.9/gp/gpControl");
        if (gpStatus != null) {
            Log.v(TAG, "Status: \n" + gpStatus.toString());
            new StartMessageActivityTask("/status",gpStatus.toString()).execute();
        }
        return  (gpControl != null);

    }
    android.os.Handler customHandler;

    void ProseesStatus(){
        updateMessage("Syncing Status...");
            try {
                gpStatus = getJson("http://10.5.5.9/gp/gpControl/status");
                if (gpStatus != null) {
                    Log.v(TAG, "Status: \n" + gpStatus.toString());
                    new StartMessageActivityTask("/status",gpStatus.toString()).execute();
                    JSONObject jsonChildNode = gpStatus.getJSONObject("status");
                    if (jsonChildNode.getInt("8") == 1){
                        RecordSend = 0;
                    }else{
                        RecordSend = 1;
                    }
                }
            }catch (Exception e) {
                e.printStackTrace();
            }

    }



    JSONObject getJson(String Url){
        JSONObject jObject = null;
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(Url);
            // Get the response
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String response_str = client.execute(request, responseHandler);

            jObject = new JSONObject(response_str);

        } catch (Exception e) {
            Log.v(TAG, "Oops: \n" );
        }

        return jObject;
    }

    void Finished(boolean CorectNetwork){
        ConectSend.setEnabled(true);
        if (CorectNetwork){
            ConectSend.setText("Connected");
            onStartWearableActivity();
            handler.postDelayed(SoGo, 1000);

        }else{
            ConectSend.setText("Error try another");
        }
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

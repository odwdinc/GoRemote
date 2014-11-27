package systems.movingdata.goremote;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;

/**
 * Created by ap on 11/23/14.
 */
public class DataListenerService extends WearableListenerService {
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String TAG = "DataListenerService";
    GoogleApiClient mGoogleApiClient;
    /////////////////////////////////////////////////
    // new SendActivityPhoneMessage("ack","").start();
    int[] xmode = new int[]{0, 10, 11, 1, 12, 13, 2, 3, 14};
    android.os.Handler customHandler;
    private int netIdBackUp;
    private boolean Connected =false;
    private boolean launchWhar = true;
    private JSONObject gpControl;
    private JSONObject gpStatus;
    private int RecordSend;
    Runnable SoGo = new Runnable() {
        @Override
        public void run() {
            if (Connected) {
                new GoGoPo().execute();
            }
        }

    };
    private int xmodepos;
    private SharedPreferences settings;
    private int GoProWifiID;

    public static void LOGD(final String tag, String message) {
            Log.d(tag, message);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        customHandler = new android.os.Handler();
        settings = getSharedPreferences("Test", Context.MODE_PRIVATE);
        GoProWifiID = settings.getInt("GoProWifiID", -1);
        Connected = settings.getBoolean("Connected", false);

    }

    @Override
    public void onPeerConnected(Node peer) {
        LOGD(TAG, "onPeerConnected: " + peer);

    }

    @Override
    public void onPeerDisconnected(Node peer) {
        LOGD(TAG, "onPeerDisconnected: " + peer);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        LOGD(TAG, "onMessageReceived: " + messageEvent);
        //TODO
        // Check to see if the message is to start an activity
        if (messageEvent.getPath().equals(START_ACTIVITY_PATH)) {
            launchWhar = false;
            if(!CheckSavedCamara()) {
                Intent startIntent = new Intent(this, MainActivity.class);
                launchWhar = false;
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startIntent);
            }else{
                Connected = true;
                launchWhar = true;
            }


        } else if (messageEvent.getPath().equals("/onClick")) {
            LOGD(TAG, "onClick: " +Connected);

            if (!Connected) {
                String Sdata = new String(messageEvent.getData(), Charset.forName("UTF-8"));
                //LOGD(TAG, "Sdata: " + Sdata);
                LOGD(TAG, "onClick startConection: ");
                startConection(Integer.parseInt(Sdata));
                Connected = true;

            } else {
                LOGD(TAG, "onClick EndConection: ");
                EndConection();
            }

        } else if (messageEvent.getPath().equals("/remote/1")) {
            Log.d(TAG, "PowerMode");
            new SendCommandToGoPro("http://10.5.5.9/gp/gpControl/command/system/sleep").execute();

        } else if (messageEvent.getPath().equals("/remote/2")) {

            xmodepos++;
            if (xmodepos >= xmode.length) {
                xmodepos = 0;
            }
            Log.d(TAG, "SlectMode:" + xmode[xmodepos]);
            new SendCommandToGoPro("http://10.5.5.9/gp/gpControl/command/xmode?p=" + xmode[xmodepos]).execute();

        } else if (messageEvent.getPath().equals("/remote/3")) {
            Log.d(TAG, "Record:" + RecordSend);
            new SendCommandToGoPro("http://10.5.5.9/gp/gpControl/command/shutter?p=" + RecordSend).execute();


        } else {
            Log.d(TAG, "onMessageReceived() A message from watch was received:" + messageEvent
                    .getRequestId() + " " + messageEvent.getPath());
        }
    }

    private boolean CheckSavedCamara() {
        if (GoProWifiID != -1){
           startConection(GoProWifiID);
           return true;
        }
        return false;
    }


    private void EndConection() {
        Connected = false;
        SharedPreferences.Editor edit = settings.edit();
        edit.putBoolean("Connected",Connected);
        edit.apply();

        gpControl = null;
        ButtenUpate("ReConect", true);
        launchWhar = true;
        connectWifi(netIdBackUp);
        showNotfaction(false);
        new SendMessageToWear("/disconnect", "").execute();

    }

    private void showNotfaction(boolean show) {
        int notificationId = 1;
        if (show) {
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Builder builder = new Notification.Builder(getApplicationContext());
            builder.setContentTitle("This is the title");
            builder.setContentText("This is the text");
            builder.setSubText("Some sub text");
            builder.setNumber(101);
            builder.setOngoing(true);
            builder.setContentIntent(pendingIntent);
            builder.setTicker("Fancy Notification");
            builder.setSmallIcon(R.drawable.ic_launcher);
            //builder.setAutoCancel(true);
            builder.setPriority(0);
            Notification notification = builder.build();
            NotificationManager notificationManger =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManger.notify(notificationId, notification);
        } else {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(notificationId);
        }
    }

    void startConection(int idata) {
        GoProWifiID = idata;
        ButtenUpate("Connecting", false);
        connectWifi(idata);
        Connected = true;
        new CheckForConectionTask(idata).execute();
    }

    public void connectWifi(int netId) {

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (netId != netIdBackUp) {
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();

            if (connectionInfo != null && !(connectionInfo.getSSID().equals(""))) {
                //if (connectionInfo != null && !StringUtil.isBlank(connectionInfo.getSSID())) {
                if (connectionInfo.getNetworkId() == netId) {
                    return;
                } else {
                    netIdBackUp = connectionInfo.getNetworkId();
                }
            }
        }
        Log.d(TAG, "disconnect form: " + netIdBackUp + " conectting to: " + netId);
        wifiManager.disconnect();
        Log.d(TAG, "disconnect");
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
        Log.d(TAG, "reconnect");
    }

    private void ButtenUpate(String ButtenText, boolean enable) {
        new SendActivityPhoneMessage("ButtenText", ButtenText).start();
        new SendActivityPhoneMessage("ButtenEnable", "" + enable).start();
    }

    // Sends an RPC to start a fullscreen Activity on the wearable.
    public void onStartWearableActivity() {
        Log.d(TAG, "Generating RPC");

        // Trigger an AsyncTask that will query for a list of connected nodes and send a
        // "start-activity" message to each connected node.
        new SendMessageToWear(START_ACTIVITY_PATH, "").execute();
    }

    JSONObject getJson(String Url) {
        JSONObject jObject = null;
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(Url);
            // Get the response
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String response_str = client.execute(request, responseHandler);

            jObject = new JSONObject(response_str);

        } catch (Exception e) {
            Log.v(TAG, "getJson Oops: +\n" + Url);
            EndConection();
        }

        return jObject;
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
            Log.v(TAG, "Activity Node is : " + node.getId() + " - " + node.getDisplayName());
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message.getBytes(Charset.forName("UTF-8"))).await();
            if (result.getStatus().isSuccess()) {
                Log.v(TAG, "Activity Message: {" + message + "} sent to: " + node.getDisplayName());
            } else {
                // Log an error
                Log.v(TAG, "ERROR: failed to send Activity Message");
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

        void checkForConection() {

            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            Log.d(TAG, "Connecteding:");

            while (!networkInfo.isConnected()) {
                networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            }

            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();

            Finished(ExpectedID == connectionInfo.getNetworkId());

            Log.d(TAG, "isConnected:" + connectionInfo.getSSID());
        }

        void Finished(Boolean CorectNetwork) {

            if (CorectNetwork) {
                ButtenUpate("Connected", false);
                SharedPreferences.Editor edit = settings.edit();
                edit.putBoolean("Connected",Connected);
                edit.apply();
                if (launchWhar) {
                    onStartWearableActivity();
                }

                new SGoPo().execute();

            } else {
                ButtenUpate("Error try another", true);
                EndConection();
            }
        }
    }

    private class SendMessageToWear extends AsyncTask<Void, Void, Void> {
        String ACTIVITY_PATH;
        byte[] ACTIVITY_DATA;

        private SendMessageToWear(String PATH, String Data) {
            this.ACTIVITY_PATH = PATH;
            if (Data.length() > 0) {
                this.ACTIVITY_DATA = Data.getBytes(Charset.forName("UTF-8"));
            } else {
                this.ACTIVITY_DATA = new byte[0];
            }
        }

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                SendMessage(node, this.ACTIVITY_PATH, this.ACTIVITY_DATA);
            }
            return null;
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

        private void SendMessage(String node, String ACTIVITY_PATH, byte[] by) {
            Log.i(TAG, "Sending to: " + ACTIVITY_PATH);

            Wearable.MessageApi.sendMessage(

                    mGoogleApiClient, node, ACTIVITY_PATH, by).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.e(TAG, "Failed to send message with status code: "
                                        + sendMessageResult.getStatus().getStatusCode());
                            }
                        }
                    }
            );
        }

    }

    private class GoGoPo extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... args) {
            if (Connected) {
                ProseesStatus();
                customHandler.postDelayed(SoGo, 1000);
            }
            return null;
        }

        void ProseesStatus() {

            try {
                gpStatus = getJson("http://10.5.5.9/gp/gpControl/status");
                if (gpStatus != null) {
                    Log.v(TAG, "Status: \n" + gpStatus.toString());
                    new SendMessageToWear("/status", gpStatus.toString()).execute();
                    JSONObject jsonChildNode = gpStatus.getJSONObject("status");
                    if (jsonChildNode.getInt("8") == 1) {
                        RecordSend = 0;
                    } else {
                        RecordSend = 1;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private class SGoPo extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... args) {
            if (checkForGoPro() && Connected) {
                showNotfaction(true);
                ButtenUpate("Syncing Status... Disconect", true);
                SharedPreferences.Editor edit = settings.edit();
                edit.putInt("GoProWifiID", GoProWifiID);
                edit.apply();
                customHandler.postDelayed(SoGo, 1000);
            }else {
                EndConection();
            }
            return null;
        }

        boolean checkForGoPro() {

            if (gpControl != null) {
                return true;
            }
            ButtenUpate("Looking for Gopro...", false);

            gpControl = getJson("http://10.5.5.9/gp/gpControl");
            if (gpControl != null) {
                Log.v(TAG, "gpControl: \n" + gpControl.toString());
                new SendMessageToWear("/gpControl", gpControl.toString()).execute();
                ButtenUpate("Found Gopro...", false);

            } else {
                ButtenUpate("Not Found, Plese check Network...", false);
            }
            gpStatus = getJson("http://10.5.5.9/gp/gpControl/status");
            if (gpStatus != null) {
                Log.v(TAG, "Status: \n" + gpStatus.toString());
                new SendMessageToWear("/status", gpStatus.toString()).execute();
            }
            return (gpControl != null);

        }
    }

    private class SendCommandToGoPro extends AsyncTask<Void, Void, Void> {
        String url;

        private SendCommandToGoPro(String url) {
            this.url = url;
        }

        @Override
        protected Void doInBackground(Void... args) {
            Log.d(TAG, "Retreaving:" + url);
            JSONObject jObject = getJson(url);
            if (jObject != null) {
                Log.d(TAG, "recived:" + jObject.toString());
            } else {
                Log.d(TAG, "Error:");
            }
            return null;
        }
    }


}
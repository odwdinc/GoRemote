package systems.movingdata.goremote;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

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
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.HashSet;
import java.util.List;

public class MainActivity extends Activity{


    private GoogleApiClient mGoogleApiClient;
    private static final long TIME_OUT_MS = 100;
    Listeners myListeners;
    private boolean mResolvingError = false;

    private static final String TAG = "UpdateButtonActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myListeners = new Listeners();
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                TextView mTextView = (TextView) stub.findViewById(R.id.TestMode);
                ImageButton powerMode = (ImageButton) stub.findViewById(R.id.PowerMode);

                powerMode.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        SendData randomWork = new SendData(1);
                        randomWork.start();

                    }
                });

                ImageButton Record = (ImageButton) stub.findViewById(R.id.Record);
                Record.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        SendData randomWork = new SendData(3);
                        randomWork.start();
                    }
                });

                ImageButton SlectMode = (ImageButton) stub.findViewById(R.id.SlectMode);
                SlectMode.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        SendData randomWork = new SendData(2);
                        randomWork.start();
                    }
                });
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new ConnectionCallbacks())
                .addOnConnectionFailedListener(new ConnectionFailedListener())
                .addApi(Wearable.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
            showNotification();
        }
    }

    private static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_REQUEST_CODE = 1;

    public void showNotification() {

        PendingIntent pi = PendingIntent.getActivity(this, NOTIFICATION_REQUEST_CODE,
                new Intent(this, Notafacation.class).putExtra("test","Test"),
                PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent min = PendingIntent.getActivity(this, NOTIFICATION_REQUEST_CODE,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        /*
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("notification_title")
                .setContentText("notification_title")
                .setSmallIcon(R.drawable.ic_launcher)
                .addAction(R.drawable.ic_launcher, "action_launch_activity",
                        pi)
                .build();
         Notification notification = new NotificationCompat.Builder(this)
                .setDeleteIntent(pi)
                .build();



                        Notification notification = new NotificationCompat.Builder(this)

                .setContentIntent(min)
                .extend(new NotificationCompat.WearableExtender()
                        .addPage(secondPageNotification))
                .build();

        */

        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(min)
                .extend(new Notification.WearableExtender()
                        .setDisplayIntent(pi)
                        .setCustomSizePreset(Notification.WearableExtender.SIZE_FULL_SCREEN))
                .build();

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification);

    }


    @Override
    protected void onStop() {
        if (!mResolvingError) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, myListeners);
            Wearable.NodeApi.removeListener(mGoogleApiClient, myListeners);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }


    private void sendMessage(String node,int button) {


        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, "/remote/"+button, new byte[0]).setResultCallback(
                new ResultCallback<SendMessageResult>() {
                    @Override
                    public void onResult(SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }else{
                            Log.i(TAG,"Sent Good");
                        }
                    }
                }
        );
        Log.i(TAG,"sent");

    }



    public class SendData extends Thread {
        int button;
        public SendData(int button) {
            this.button = button;
        }

        @Override
        public void run() {
            Log.v(TAG, "doing work in Thread");
            sendMessage(getRemoteNodeId(),this.button);
        }
    }

    private class ConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle bundle) {
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

    private Uri getUriForDataItem() {
        // If you've put data on the local node
        String nodeId = getLocalNodeId();
        // Or if you've put data on the remote node
        // String nodeId = getRemoteNodeId();
        // Or If you already know the node id
        // String nodeId = "some_node_id";
        return new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(nodeId).path("/goRemote").build();
    }

    private String getLocalNodeId() {
        NodeApi.GetLocalNodeResult nodeResult = Wearable.NodeApi.getLocalNode(mGoogleApiClient).await();
        return nodeResult.getNode().getId();
    }

    private String getRemoteNodeId() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodesResult =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        List<Node> nodes = nodesResult.getNodes();
        if (nodes.size() > 0) {
            return nodes.get(0).getId();
        }
        return null;
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

            Log.d(TAG, "onMessageReceived() A message from watch was received:" + messageEvent
                    .getRequestId() + " " + messageEvent.getPath());

        }

    }
}

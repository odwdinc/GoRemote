package systems.movingdata.goremote;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;

/**
 * Created by asprayx on 11/11/2014.
 */
public class DataManager implements DataApi.DataListener,
        MessageApi.MessageListener, NodeApi.NodeListener
        {
    public GoogleApiClient mGoogleApiClient;

    MyActivity ParentActivity;
    Bundle DataBul;

    public DataManager(MyActivity Parent) {
        ParentActivity = Parent;
        DataBul = new Bundle();

        mGoogleApiClient = new GoogleApiClient.Builder(ParentActivity)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new ConnectionCallbacks(this))
                .addOnConnectionFailedListener(new ConnectionFailedListener(this))
                .build();
    }



    private class ConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks {
        DataManager Mydata;
        private ConnectionCallbacks(DataManager data) {
            Mydata=data;
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(ParentActivity.TAG, "Google API Client was connected");
            Wearable.MessageApi.addListener(mGoogleApiClient, Mydata);
            Wearable.NodeApi.addListener(mGoogleApiClient, Mydata);
        }

        @Override
        public void onConnectionSuspended(int i) {
            // empty
        }
    }


    private class ConnectionFailedListener implements
            GoogleApiClient.OnConnectionFailedListener {
        DataManager Mydata;
        private ConnectionFailedListener(DataManager data) {
            Mydata=data;
        }
        @Override
        public void onConnectionFailed(ConnectionResult result) {
                        // empty
        }
    }


    @Override //DataListener
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(ParentActivity.TAG, "onDataChanged: " + dataEvents);
    }

    @Override //NodeListener
    public void onPeerConnected(final Node peer) {
        Log.d(ParentActivity.TAG, "onPeerConnected: " + peer);


    }
    @Override
    public void onPeerDisconnected(Node peer) {
        Log.d(ParentActivity.TAG, "onPeerDisconnected: " + peer);
    }

    public void updateScreen(final Bundle data){
        ParentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ParentActivity.UpdateGui(data);
            }
        });
    }

    public String getRemoteNodeId() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodesResult =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        List<Node> nodes = nodesResult.getNodes();
        if (nodes.size() > 0) {
            return nodes.get(0).getId();
        }
        return null;
    }


     private static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_REQUEST_CODE = 1;

    public void showNotification() {

        PendingIntent pi = PendingIntent.getActivity(ParentActivity, NOTIFICATION_REQUEST_CODE,
                new Intent(ParentActivity, Notafacation.class).putExtras(DataBul),
                PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent min = PendingIntent.getActivity(ParentActivity, NOTIFICATION_REQUEST_CODE,
                new Intent(ParentActivity, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT );

        Notification notification = new Notification.Builder(ParentActivity)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(min)
                .extend(new Notification.WearableExtender()
                        .setDisplayIntent(pi)
                        .setCustomSizePreset(Notification.WearableExtender.SIZE_FULL_SCREEN))
                .build();

        NotificationManagerCompat.from(ParentActivity).notify(NOTIFICATION_ID, notification);

    }


    @Override //MessageListener
    public void onMessageReceived(final MessageEvent messageEvent) {

        Log.d(ParentActivity.TAG, "onMessageReceived() A message from watch was received:" + messageEvent
                .getRequestId() + " " + messageEvent.getPath());
        DataBul.putString(messageEvent.getPath(), new String(messageEvent.getData(), Charset.forName("UTF-8")));
        updateScreen(DataBul);
    }


}
